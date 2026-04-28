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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static work.daqian.myai.util.ChatUtil.saveUsageDetail;
import static work.daqian.myai.util.ChatUtil.toJson;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleModelAdapter implements ModelAdapter {

    private final WebClient.Builder builder;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${api.key.google}")
    private String apiKey;

    @Override
    public Provider getProvider() {
        return Provider.GOOGLE;
    }

    @Override
    public WebClient buildWebClient() {
        HttpClient httpClient = HttpClient.newConnection();
        return builder.baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("x-goog-api-key", apiKey)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public String getUri(String modelName) {
        return "/v1beta/models/" + modelName + ":streamGenerateContent?alt=sse";
    }

    @Override
    public Object buildRequest(String modelName, List<Message> prompt, boolean stream, boolean think) {
        boolean lite = modelName.contains("lite");
        if (lite) think = false;
        List<Map<String, String>> systemPrompts = new ArrayList<>(5);
        Map<String, Object> systemInstruction = Map.of(
                "role", "system",
                "parts", systemPrompts
        );
        List<Map<String, Object>> contents = new ArrayList<>(prompt.size());
        for (Message message : prompt) {
            if (message.getRole().equals("system")) {
                systemPrompts.add(Map.of(
                        "text", message.getContent()
                ));
            } else {
                contents.add(Map.of(
                        "role", message.getRole().equals("assistant") ? "model" : message.getRole(),
                        "parts", List.of(Map.of("text", message.getContent()))
                ));
            }
        }
        Map<String, Object> thinkingConfig = new HashMap<>(5);
        thinkingConfig.put("includeThoughts", think);
        if (!modelName.contains("2.5")) thinkingConfig.put("thinkingLevel", "high");
        Map<String, Map<String, Object>> generationConfig = Map.of(
                "thinkingConfig", thinkingConfig
        );
        Map<String, Object> requestMap = new HashMap<>(5);
        requestMap.put("contents", contents);

        if (!lite)
            requestMap.put("generationConfig", generationConfig);
        if (!systemPrompts.isEmpty())
            requestMap.put("systemInstruction", systemInstruction);

        // ChatUtil.debugRequestBody(requestMap);
        return requestMap;
    }

    @Override
    public Flux<String> parseChunk(String chunk,
                                   StringBuilder contentBuilder,
                                   StringBuilder thinkingBuilder,
                                   Long userId, String sessionId) {
        try {
            List<String> result = new ArrayList<>();
            String line = chunk.trim();

            //System.out.println(line + "#######");

            // 跳过空行和结束标记
            if (line.isEmpty()) return Flux.empty();
            if ("[DONE]".equals(line)) return Flux.empty();

            JsonNode node = mapper.readTree(line);

            // 获取 candidates
            JsonNode candidates = node.path("candidates");
            if (candidates.isEmpty()) return Flux.empty();

            JsonNode firstCandidate = candidates.get(0);
            if (firstCandidate == null) return Flux.empty();

            // 获取 parts
            JsonNode parts = firstCandidate.path("content").path("parts");
            if (parts.isEmpty()) return Flux.empty();

            // 获取 finishReason
            String finishReason = firstCandidate.path("finishReason").asText(null);

            synchronized (contentBuilder) {
                for (JsonNode part : parts) {
                    String text = part.path("text").asText(null);
                    boolean isThought = part.path("thought").asBoolean(false);

                    if (text != null && !text.isEmpty()) {
                        if (isThought) {
                            // ★ 思考内容 → 前端 type: "thinking"
                            result.add(toJson("thinking", text));
                            thinkingBuilder.append(text);
                        } else {
                            // ★ 正式回答 → 前端 type: "content"
                            result.add(toJson("content", text));
                            contentBuilder.append(text);
                        }
                    }
                }

                // 收集 token 用量（最后一帧或 STOP 帧）
                if ("STOP".equals(finishReason)) {
                    JsonNode usageMeta = node.path("usageMetadata");
                    if (!usageMeta.isMissingNode() && !usageMeta.isNull()) {
                        String modelName = node.path("modelVersion").asText();
                        int promptTokens = usageMeta.path("promptTokenCount").asInt();
                        int totalTokenCount = usageMeta.path("totalTokenCount").asInt();
                        int contentTokens = usageMeta.path("candidatesTokenCount").asInt();
                        int reasoningTokens = usageMeta.path("thoughtsTokenCount").asInt();
                        saveUsageDetail(userId, sessionId, modelName, promptTokens, contentTokens + reasoningTokens, totalTokenCount, reasoningTokens, 0);
                    }
                }
            }

            return Flux.fromIterable(result);

        } catch (Exception e) {
            log.warn("解析 Google chunk 失败: {}", e.getMessage());
            return Flux.empty();
        }
    }

    @Override
    public Class<? extends NonStreamResponse> getNonStreamResponseClass() {
        return null;
    }
}
