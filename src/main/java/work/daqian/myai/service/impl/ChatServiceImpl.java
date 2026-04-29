package work.daqian.myai.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import work.daqian.myai.adapter.ModelAdapter;
import work.daqian.myai.domain.dto.ChatFormDTO;
import work.daqian.myai.domain.dto.Message;
import work.daqian.myai.domain.po.ChatSession;
import work.daqian.myai.domain.po.Model;
import work.daqian.myai.enums.Provider;
import work.daqian.myai.exception.BadRequestException;
import work.daqian.myai.mapper.ChatSessionMapper;
import work.daqian.myai.prompt.PromptBuilder;
import work.daqian.myai.prompt.PromptContext;
import work.daqian.myai.service.ChatMessageService;
import work.daqian.myai.service.ChatService;
import work.daqian.myai.service.ContextService;
import work.daqian.myai.service.IModelService;
import work.daqian.myai.tool.AgentService;
import work.daqian.myai.tool.ToolCall;
import work.daqian.myai.tool.ToolCallParser;
import work.daqian.myai.tool.ToolExecutor;
import work.daqian.myai.util.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService, InitializingBean {

    private final ChatMessageService messageService;
    private final ChatSessionMapper sessionMapper;
    private final ContextService contextService;
    private final PromptBuilder promptBuilder;
    private final IModelService modelService;
    private final RedisUtil redisUtil;
    private final ToolCallParser toolCallParser;
    private final ToolExecutor toolExecutor;
    @Lazy
    private final AgentService agentService;
    @Autowired
    @Qualifier("taskExecutor")
    private Executor taskExecutor;

    private final List<ModelAdapter> adapters;
    private Map<Provider, ModelAdapter> adapterMap;

    @Override
    public void afterPropertiesSet() {
        adapterMap = adapters.stream()
                .collect(Collectors.toMap(ModelAdapter::getProvider, Function.identity()));
    }

    @Override
    public Flux<String> streamChat(ChatFormDTO chatForm) {
        String text = chatForm.getText();
        // 提前异步调用工具链，减少阻塞时间
        String currentIp = IpUtils.getCurrentIp();
        CompletableFuture<List<Message>> agentFuture = CompletableFuture.supplyAsync(() -> agentService.runAgent(text, currentIp), taskExecutor);
        Long userId = SecurityUtils.getCurrentUserId();
        String sessionId = chatForm.getSessionId();
        Boolean think = chatForm.getThink();
        Boolean enablePrompt = chatForm.getPrompt();
        Long modelId = chatForm.getModelId();
        Boolean search = chatForm.getSearch();
        String providerAndName = redisUtil.cacheEmptyIfNE(
                "model:id:",
                modelId,
                Duration.ofHours(1),
                (mid) -> {
                    Model model = modelService.getById(mid);
                    if (model == null) throw new BadRequestException("模型不存在");
                    return model.getProvider().getValue() + "," + model.getFullName();
                }
        );
        String[] split = providerAndName.split(",");
        Provider provider;
        String modelName;
        if (split.length == 2) {
            int value = Integer.parseInt(split[0]);
            Provider p = Provider.fromValue(value == 2 ? 1 : value);
            SecurityAssert.canAccessModel(p);
            provider = p;
            modelName = split[1];
        } else {
            provider = Provider.OLLAMA;
            modelName = "qwen3.5:9b";
        }
        Message userMessage = new Message("user", text);

        List<Message> history = contextService.getHistory(sessionId, false);
        PromptContext promptContext;
        if (enablePrompt) {
            promptContext = PromptContext.builder()
                    .persona(contextService.getPersona(sessionId))
                    .rules(contextService.getRules(sessionId))
                    .example(contextService.getExample(sessionId))
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
        try {
            prompt.addAll(agentFuture.get());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        if (search) {
            // 联网搜索
            String searchDecision = agentService.chatOnce(modelName, provider, List.of(
                    new Message("system", promptBuilder.buildSearchPrompt()),
                    userMessage
            ));
            String searchResult = getToolResult(searchDecision);
            if (!StringUtils.isEmpty(searchResult)) {
                prompt.add(new Message("assistant", searchDecision));
                prompt.add(new Message("tool", searchResult));
            }
        }

        StringBuilder contentBuilder = new StringBuilder();
        StringBuilder thinkingBuilder = new StringBuilder();
        AtomicBoolean isSaved = new AtomicBoolean(false);

        // 根据 Provider 选择 WebClient、URI、请求体
        ModelAdapter modelAdapter = adapterMap.get(provider);
        WebClient client = modelAdapter.buildWebClient();
        String uri = modelAdapter.getUri(modelName);
        Object request = modelAdapter.buildRequest(modelName, prompt, true, think);

        return client.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(chunk -> modelAdapter.parseChunk(chunk, contentBuilder, thinkingBuilder, userId, sessionId))
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
                                    if (provider.equals(Provider.OLLAMA)) {
                                        int promptTokens = prompt.stream()
                                                .mapToInt(message -> message.getContent().length())
                                                .sum();
                                        int reasoningTokens = thin.length();
                                        int contentTokens = cont.length();
                                        if (isEnglish(text)) {
                                            promptTokens >>= 2;
                                            reasoningTokens >>= 2;
                                            contentTokens >>= 2;
                                        }
                                        int completionTokens = reasoningTokens + contentTokens;
                                        int totalTokens = completionTokens + promptTokens;
                                        ChatUtil.saveUsageDetail(userId, sessionId, modelName,
                                                promptTokens, completionTokens, totalTokens, reasoningTokens, 0);
                                    }
                                    messageService.saveUserMessage(sessionId, userId, text);
                                    messageService.saveAssistantMessage(sessionId, userId, modelName, cont, thin);
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

    @Nullable
    private String getToolResult(String decision) {
        ToolCall toolCall = toolCallParser.parse(decision);
        String toolResult = null;
        if (toolCall != null) {
            toolResult = toolExecutor.execute(toolCall);
        }
        return toolResult;
    }

    private boolean isEnglish(String s) {
        char c = s.charAt(0);
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }


}
