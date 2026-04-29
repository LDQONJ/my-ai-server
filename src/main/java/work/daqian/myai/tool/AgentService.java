package work.daqian.myai.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import work.daqian.myai.adapter.ModelAdapter;
import work.daqian.myai.adapter.NonStreamResponse;
import work.daqian.myai.domain.dto.Message;
import work.daqian.myai.enums.Provider;
import work.daqian.myai.prompt.PromptBuilder;
import work.daqian.myai.websocket.WebSocketService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService implements InitializingBean {

    private final ToolExecutor toolExecutor;

    private final ToolCallParser toolCallParser;

    private final List<ModelAdapter> adapters;

    private final List<Tool> tools;
    private final WebSocketService webSocketService;

    private Map<Provider, ModelAdapter> adapterMap;

    private static List<ToolDefinition> TDS;

    private final PromptBuilder promptBuilder;

    @Override
    public void afterPropertiesSet() {
        adapterMap = adapters.stream()
                .collect(Collectors.toMap(ModelAdapter::getProvider, Function.identity()));
        TDS = tools.stream().map(Tool::getToolDefinition).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static final int MAX_STEPS = 5;

    public List<Message> runAgent(String wsId, String userInput, String ip) {

        List<Message> messages = new ArrayList<>();

        List<Message> result = new ArrayList<>();

        messages.add(new Message("system", """
                你是一个工具调用者，根据用户的输入判断是否需要调用工具。调用规则:
                
                1. 只在你认为必须调用工具时才调用，不需要调用工具时不要回复用户，直接输出{}即可。
                2. 如果当前问题已经可以回答，请不要再调用任何工具
                3. 不要为了“补充信息”而额外调用工具
                4. 工具调用应最小化（能少就少）
                """));
        messages.add(new Message("system", promptBuilder.buildToolPrompt(TDS, ip)));
        messages.add(new Message("user", userInput));

        // Agent Loop
        for (int i = 0; i < MAX_STEPS; i++) {

            // 模型决策（非流式）
            String decision = chatOnce("qwen3.6-flash", Provider.ALIBABA, messages);

            if (decision == null || decision.equals("{}")) break;

            ToolCall call = toolCallParser.parse(decision);

            // 执行工具
            String toolResult = toolExecutor.execute(call, wsId);
            if (toolResult == null) break;

            // 写入上下文
            messages.add(new Message("assistant", decision));
            messages.add(new Message("tool", toolResult));

            result.add(new Message("assistant", decision));
            result.add(new Message("tool", toolResult));

            // log.info("本次 Agent 循环结果: {}", result);
        }
        if (result.isEmpty()) {
            webSocketService.sendMessageToClient(wsId, "未调用工具");
        } else {
            webSocketService.sendMessageToClient(wsId, "已调用 " + (result.size() >> 1) + " 个工具");
        }
        return result;
    }

    public String chatOnce(String modelName, Provider provider, List<Message> messages) {
        ModelAdapter modelAdapter = adapterMap.get(provider);
        Object request = modelAdapter.buildRequest(modelName, messages, false, false);
        Class<? extends NonStreamResponse> clazz = modelAdapter.getNonStreamResponseClass();
        NonStreamResponse response = modelAdapter
                .buildWebClient().post()
                .uri(modelAdapter.getUri(modelName))
                .bodyValue(request)
                .retrieve()
                .bodyToMono(clazz)
                .block();
        return response.getContent();
    }
}
