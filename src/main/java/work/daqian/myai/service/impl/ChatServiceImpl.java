package work.daqian.myai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import work.daqian.myai.domain.dto.ChatFormDTO;
import work.daqian.myai.domain.dto.ChatRequest;
import work.daqian.myai.domain.dto.Message;
import work.daqian.myai.domain.po.ChatSession;
import work.daqian.myai.domain.po.Model;
import work.daqian.myai.enums.Provider;
import work.daqian.myai.mapper.ChatSessionMapper;
import work.daqian.myai.prompt.PromptBuilder;
import work.daqian.myai.prompt.PromptContext;
import work.daqian.myai.service.ChatMessageService;
import work.daqian.myai.service.ChatService;
import work.daqian.myai.service.ContextService;
import work.daqian.myai.service.IModelService;
import work.daqian.myai.util.RedisUtil;
import work.daqian.myai.util.SecurityAssert;
import work.daqian.myai.util.SecurityUtils;

import java.time.Duration;
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
    private final ObjectMapper mapper = new ObjectMapper();
    private final IModelService modelService;
    private final RedisUtil redisUtil;
    @Autowired
    @Qualifier("taskExecutor")
    private Executor taskExecutor;

    // 持有多个 WebClient
    public static WebClient ollamaClient;
    private WebClient bailianClient;

    @Value("${aliyun.api.key}")
    private String aliApiKey;

    @Override
    public void afterPropertiesSet() {
        // Ollama 客户端（你原来的逻辑）
        reactor.netty.http.client.HttpClient ollamaHttp = reactor.netty.http.client.HttpClient.newConnection();
        ollamaClient = webClientBuilder
                .baseUrl("http://127.0.0.1:11434")
                .clientConnector(new ReactorClientHttpConnector(ollamaHttp))
                .build();

        // 百炼客户端
        reactor.netty.http.client.HttpClient bailianHttp = reactor.netty.http.client.HttpClient.newConnection();
        this.bailianClient = webClientBuilder
                .baseUrl("https://dashscope.aliyuncs.com")
                .defaultHeader("Authorization", "Bearer " + aliApiKey)
                .clientConnector(new ReactorClientHttpConnector(bailianHttp))
                .build();
    }

    @Override
    public Flux<String> streamChat(ChatFormDTO chatForm) {
        Long userId = SecurityUtils.getCurrentUserId();
        String sessionId = chatForm.getSessionId();
        Boolean think = chatForm.getThink();
        Boolean enablePrompt = chatForm.getPrompt();
        String text = chatForm.getText();
        Long modelId = chatForm.getModelId();
        String providerAndName = redisUtil.cacheEmptyIfNE(
                "model:id:",
                modelId,
                Duration.ofHours(1),
                (mid) -> {
                    Model model = modelService.getById(mid);
                    return model.getProvider().getValue() + "," + model.getFullName();
                }
        );
        String[] split = providerAndName.split(",");
        Provider provider;
        String modelName = "qwen3.5:9b";
        if (split.length == 2) {
            Provider p = Provider.fromValue(Integer.parseInt(split[0]));
            SecurityAssert.canAccessModel(p);
            provider = p;
            modelName = split[1];
        } else {
            provider = Provider.OLLAMA;
        }

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
        AtomicBoolean isSaved = new AtomicBoolean(false);

        // 根据 Provider 选择 WebClient、URI、请求体
        WebClient client;
        String uri;
        Object request;

        if (provider == Provider.BAILIAN) {
            client = bailianClient;
            uri = "/compatible-mode/v1/chat/completions";
            // 构建百炼请求体
            List<Map<String, String>> messages = prompt.stream()
                    .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                    .toList();
            request = Map.of(
                    "model", modelName,
                    "messages", messages,
                    "reasoning_effort", "high",
                    "stream", true,
                    "stream_options", Map.of("include_usage", false),
                    "extra_body", Map.of("enable_thinking", think)
            );
        } else {
            client = ollamaClient;
            uri = "/api/chat";
            request = new ChatRequest(
                    modelName,
                    prompt,
                    true,
                    think
            );
        }

        return client.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(chunk -> {
                    if (provider == Provider.BAILIAN) {
                        return parseBailianChunk(chunk, contentBuilder, thinkingBuilder);
                    }
                    return parseOllamaChunk(chunk, contentBuilder, thinkingBuilder);
                })
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
                });
    }

    // Ollama 解析
    private Flux<String> parseOllamaChunk(String chunk, StringBuilder contentBuilder, StringBuilder thinkingBuilder) {
        try {
            String[] lines = chunk.split("\n");
            List<String> result = new ArrayList<>();
            synchronized (contentBuilder) {
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    JsonNode node = mapper.readTree(line);
                    JsonNode message = node.path("message");
                    String thinking = message.path("thinking").asText();
                    if (thinking != null && !thinking.isEmpty()) {
                        result.add(toJson("thinking", thinking));
                        if (thinkingBuilder != null) thinkingBuilder.append(thinking);
                    }
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

    // 百炼 SSE 解析
    private Flux<String> parseBailianChunk(String chunk, StringBuilder contentBuilder, StringBuilder thinkingBuilder) {
        try {
            List<String> result = new ArrayList<>();
            String[] lines = chunk.split("\n");

            synchronized (contentBuilder) {
                for (String line : lines) {
                    line = line.trim();

                    // 跳过空行
                    if (line.isEmpty()) continue;

                    // 跳过结束标记
                    if ("[DONE]".equals(line)) continue;

                    // 提取 JSON
                    JsonNode node = mapper.readTree(line);

                    // 跳过 usage 统计帧（choices 为空）
                    JsonNode choices = node.path("choices");
                    if (choices.isEmpty()) continue;

                    JsonNode delta = choices.get(0).path("delta");

                    String reasoning = delta.path("reasoning_content").asText(null);
                    if (reasoning != null && !reasoning.isEmpty()) {
                        result.add(toJson("thinking", reasoning));
                        thinkingBuilder.append(reasoning);
                    }

                    String content = delta.path("content").asText(null);
                    if (content != null && !content.isEmpty()) {
                        result.add(toJson("content", content));
                        contentBuilder.append(content);
                    }
                }
            }
            return Flux.fromIterable(result);
        } catch (Exception e) {
            log.warn("解析百炼chunk失败: {}", e.getMessage());
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
