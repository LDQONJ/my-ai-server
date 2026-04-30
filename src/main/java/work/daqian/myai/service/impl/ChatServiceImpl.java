package work.daqian.myai.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
import work.daqian.myai.tool.impl.WebSearchTool;
import work.daqian.myai.util.*;
import work.daqian.myai.websocket.WebSocketService;

import java.time.Duration;
import java.util.ArrayList;
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
    private final WebSocketService webSocketService;
    private final WebSearchTool webSearchTool;
    @Autowired
    @Qualifier("taskExecutor")
    private Executor taskExecutor;
    @Value("${AGENT_MODEL}")
    private String agentModel;

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
        String wsId = chatForm.getWsId();
        String sessionId = chatForm.getSessionId();
        List<Message> history = contextService.getHistory(sessionId, false);
        CompletableFuture<List<Message>> agentFuture = CompletableFuture.supplyAsync(() -> agentService.runAgent(wsId, history, text, currentIp), taskExecutor);

        Long userId = SecurityUtils.getCurrentUserId();
        Boolean think = chatForm.getThink();
        Boolean enablePrompt = chatForm.getPrompt();
        Long modelId = chatForm.getModelId();
        Boolean search = chatForm.getSearch();
        String audio = chatForm.getAudio();

        // 根据模型 id 查询模型名称和模型供应商，优先查找缓存
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

        // 构建 prompt
        Message userMessage = new Message("user", text);
        // log.info("当前会话 {} 历史上下文：{}", sessionId, history);
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
        List<Message> agentToolTemp = new ArrayList<>(10);
        List<Message> agentResult;
        // 拼接工具调用结果
        try {
            agentResult = agentFuture.get();
            prompt.addAll(agentResult);
            agentToolTemp.addAll(agentResult);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        List<Message> searchContext = new ArrayList<>(32);
        searchContext.add(new Message("system", promptBuilder.buildSearchPrompt(webSearchTool.getToolDefinition())));
        searchContext.addAll(history);
        searchContext.add(userMessage);
        searchContext.addAll(agentResult);
        if (search) {
            // 联网搜索
            String searchDecision = agentService.getDecision(agentModel, Provider.ALIBABA, searchContext);
            String searchResult = getToolResult(searchDecision, wsId);
            if (!StringUtils.isEmpty(searchResult)) {
                prompt.add(new Message("system", """
                        你是一个具备联网搜索能力的AI助手。在回答需要引用外部信息的问题时，请严格遵守以下规则：
                        
                        ## 1. 信息溯源原则
                        - 凡是通过联网搜索获得的事实性信息、数据、观点，**必须**明确标注来源。
                        - 不得将搜索结果中的信息包装为“众所周知”或“一般认为”的常识来规避引用。
                        
                        ## 2. 引用格式要求
                        每条引用信息末尾或段落结束后，使用以下格式标注：
                        ```
                        （来源：[文章标题/网页标题](url)）
                        ```
                        若同一段落内引用多个来源，请分别标注。
                        
                        ## 3. 引用时机
                        以下情况必须提供引用：
                        - 引用具体数据、统计数字、百分比
                        - 引用他人观点、评论、分析
                        - 引用事件的时间、地点等事实性细节
                        - 引用研究成果、报告结论
                        - 任何非通识性知识
                        
                        ## 4. 信息时效性标注
                        如搜索结果包含发布时间，请在引用中注明：
                        ```
                        （来源：[xxx](https://...) | 发布时间：YYYY-MM-DD）
                        ```
                        
                        ## 5. 多源交叉验证
                        - 当同一信息存在多个来源时，优先引用权威性更高的来源（官方机构 > 知名媒体 > 其他来源）。
                        - 如信息存在矛盾，应列出不同来源并说明差异，而非选择性地呈现单一观点。
                        
                        ---
                        
                        **示例输出：**
                        
                        > 根据最新数据，2024年全球AI市场规模预计达到3059亿美元，同比增长约15.8%。（来源：[Gartner《全球人工智能市场预测报告》](https://www.gartner.com/ai-market-forecast-2024) | 发布时间：2024-03-15）

                        """));
                prompt.add(new Message("assistant", searchDecision));
                prompt.add(new Message("tool", searchResult));
                agentToolTemp.add(new Message("assistant", searchDecision));
                agentToolTemp.add(new Message("tool", searchResult));
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
        webSocketService.sendMessageToClient(wsId, "模型加载中...");
        return client.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(chunk -> modelAdapter.parseChunk(chunk, contentBuilder, thinkingBuilder, userId, sessionId))
                .filter(content -> content != null && !content.isEmpty())
                .concatWith(Flux.just(ChatUtil.toSSEDone()))
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
                            history.addAll(agentToolTemp);
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
                                    messageService.saveUserMessage(sessionId, userId, text, audio);
                                    messageService.saveAgentToolMessage(sessionId, userId, "qwen3.6-flash", agentToolTemp);
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
    private String getToolResult(String decision, String wsId) {
        ToolCall toolCall = toolCallParser.parse(decision);
        String toolResult = null;
        if (toolCall != null) {
            toolResult = toolExecutor.execute(toolCall, wsId);
        }
        return toolResult;
    }

    private boolean isEnglish(String s) {
        char c = s.charAt(0);
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }
}
