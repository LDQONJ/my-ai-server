package work.daqian.myai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import work.daqian.myai.domain.dto.ChatFormDTO;
import work.daqian.myai.domain.dto.ChatRequest;
import work.daqian.myai.domain.dto.Message;
import work.daqian.myai.domain.po.ChatSession;
import work.daqian.myai.mapper.ChatSessionMapper;
import work.daqian.myai.prompt.PromptBuilder;
import work.daqian.myai.prompt.PromptContext;
import work.daqian.myai.service.ChatMessageService;
import work.daqian.myai.service.ChatService;
import work.daqian.myai.service.ContextService;
import work.daqian.myai.service.IModelService;
import work.daqian.myai.util.SecurityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService, InitializingBean {


    private final WebClient.Builder webClientBuilder;

    private final ChatMessageService messageService;

    private final ChatSessionMapper sessionMapper;

    private final ContextService contextService;

    private final PromptBuilder promptBuilder;

    public static WebClient webClient;

    private final ObjectMapper mapper = new ObjectMapper();

    private final IModelService modelService;

    @Autowired
    @Qualifier("taskExecutor")
    private Executor taskExecutor;

    @Override
    public void afterPropertiesSet() {
        // HttpClient.newConnection() 确保不使用连接池
        // 这样在 WebClient 链路 Cancel 时，TCP 连接会立即被物理 Close
        reactor.netty.http.client.HttpClient httpClient = reactor.netty.http.client.HttpClient.newConnection();
        webClient = webClientBuilder
                .baseUrl("http://127.0.0.1:11434")
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public Flux<String> streamChat(ChatFormDTO chatForm) {
        Long userId = SecurityUtils.getCurrentUserId();
        String sessionId = chatForm.getSessionId();
        Boolean think = chatForm.getThink();
        Boolean enablePrompt = chatForm.getPrompt();
        String text = chatForm.getText();

        List<Message> history = contextService.getHistory(sessionId, false);
        Message userMessage = new Message("user", text);

        PromptContext promptContext;
        if (enablePrompt) {
            promptContext = PromptContext.builder()
                    .persona(contextService.getPersona(sessionId))
                    .rules(contextService.getRules(sessionId))
                    .summary(contextService.getSummary(sessionId))
                    .history(history)
                    .userInput(userMessage)
                    .build();
        } else {
            promptContext = PromptContext.builder()
                    .summary(contextService.getSummary(sessionId))
                    .history(history)
                    .userInput(userMessage)
                    .build();
        }

        List<Message> prompt = promptBuilder.build(promptContext);

        StringBuilder contentBuilder = new StringBuilder();
        StringBuilder thinkingBuilder = new StringBuilder();

        // 确保保存逻辑只执行一次
        AtomicBoolean isSaved = new AtomicBoolean(false);

        ChatRequest request = new ChatRequest(
                modelService.getCurrentModel().get(),
                prompt,
                true,
                think
        );
        return webClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(chunk -> parseChunk(chunk, contentBuilder, thinkingBuilder))
                .filter(content -> content != null && !content.isEmpty())
                .doFinally(signalType -> {
                            if (isSaved.compareAndSet(false, true)) {
                                String cont;
                                String thin;
                                synchronized (contentBuilder) {
                                    cont = contentBuilder.toString();
                                    thin = thinkingBuilder.toString();
                                }
                                if (!cont.isEmpty() || !thin.isEmpty()) {
                                    history.add(userMessage);
                                    history.add(new Message("assistant", cont));
                                    CompletableFuture.runAsync(() -> {
                                        try {
                                            messageService.saveUserMessage(sessionId, userId, text);
                                            messageService.saveAssistantMessage(sessionId, userId, cont, thin);
                                            contextService.saveHistory(sessionId, history);
                                            ChatSession session = sessionMapper.selectById(sessionId);
                                            if (session != null) {
                                                session.setMessageCount(session.getMessageCount() + 2);
                                                if (!cont.isEmpty()) {
                                                    session.setLastMessage(cont.substring(0, Math.min(cont.length(), 30)));
                                                } else {
                                                    session.setLastMessage(thin.substring(0, Math.min(thin.length(), 30)));
                                                }
                                                sessionMapper.updateById(session);
                                            }
                                        } catch (Exception e) {
                                            log.error("保存聊天记录失败", e);
                                        }
                                    }, taskExecutor);
                                } else {
                                    log.info("由于 AI 回复失败，未保存任何记录");
                                }
                            }
                        }
                ).concatWith(Flux.just(toSSEDone()));
    }

    // 解析 Ollama chunk
    private Flux<String> parseChunk(String chunk, StringBuilder contentBuilder, StringBuilder thinkingBuilder) {
        try {
            String[] lines = chunk.split("\n");
            List<String> result = new ArrayList<>();

            // 锁定 contentBuilder 确保 append 过程不被 doFinally 的 toString 打断
            synchronized (contentBuilder) {
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    JsonNode node = mapper.readTree(line);
                    JsonNode message = node.path("message");

                    // 1. 解析思考内容
                    String thinking = message.path("thinking").asText();
                    if (thinking != null && !thinking.isEmpty()) {
                        result.add(toJson("thinking", thinking));
                        if (thinkingBuilder != null)
                            thinkingBuilder.append(thinking);
                    }

                    // 2. 解析正常内容
                    String content = message.path("content").asText();
                    if (content != null && !content.isEmpty()) {
                        result.add(toJson("content", content));
                        contentBuilder.append(content);
                    }
                }
            }
            return Flux.fromIterable(result);
        } catch (Exception e) {
            return Flux.empty();
        }
    }

    private String toJson(String type, String text) {
        try {
            return mapper.writeValueAsString(
                    Map.of(
                            "type", type,
                            "content", text
                    )
            );
        } catch (Exception e) {
            return "";
        }
    }

    public static String toSSEDone() {
        return "{\"done\":true}";
    }
}
