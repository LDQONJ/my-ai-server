package work.daqian.myai.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import work.daqian.myai.domain.dto.Message;
import work.daqian.myai.enums.Provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static work.daqian.myai.util.ChatUtil.saveUsageDetail;
import static work.daqian.myai.util.ChatUtil.toJson;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlibabaModelAdapter implements ModelAdapter {

    private final ObjectMapper mapper = new ObjectMapper();
    private final WebClient.Builder builder;

    @Value("${api.key.alibaba}")
    private String apiKey;

    @Override
    public Provider getProvider() {
        return Provider.ALIBABA;
    }

    @Override
    public WebClient buildWebClient() {
        HttpClient httpClient = HttpClient.newConnection();
        return builder.baseUrl("https://dashscope.aliyuncs.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public String getUri(String modelName) {
        return "/compatible-mode/v1/chat/completions";
    }

    @Override
    public Object buildRequest(String modelName, List<Message> prompt, boolean stream, boolean think) {
        List<Map<String, String>> messages = prompt.stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .toList();
        return Map.of(
                "model", modelName,
                "messages", messages,
                "reasoning_effort", "high",
                "stream", stream,
                "stream_options", Map.of("include_usage", true),
                "enable_thinking", think
        );
    }

    @Override
    public Flux<String> parseChunk(String chunk, StringBuilder contentBuilder, StringBuilder thinkingBuilder, Long userId, String sessionId) {
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

                    // 统计 Usage（choices 为空）
                    JsonNode choices = node.path("choices");
                    if (choices.isEmpty()) {
                        JsonNode usage = node.path("usage");
                        int promptTokens = usage.path("prompt_tokens").asInt();
                        int completionTokens = usage.path("completion_tokens").asInt();
                        int totalTokens = usage.path("total_tokens").asInt();
                        int reasoningTokens = 0;
                        JsonNode completionTokensDetails = usage.path("completion_tokens_details");
                        if (completionTokensDetails != null)
                            reasoningTokens = completionTokensDetails.path("reasoning_tokens").asInt();
                        int cachedTokens = 0;
                        JsonNode promptTokensDetails = usage.path("prompt_tokens_details");
                        if (promptTokensDetails != null)
                            cachedTokens = promptTokensDetails.path("cached_tokens").asInt();
                        String modelName = node.path("model").asText();
                        saveUsageDetail(userId, sessionId, modelName, promptTokens, completionTokens, totalTokens, reasoningTokens, cachedTokens);
                        continue;
                    }

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
            log.warn("解析阿里云 Api chunk 失败: {}", e.getMessage());
            return Flux.empty();
        }
    }
}
