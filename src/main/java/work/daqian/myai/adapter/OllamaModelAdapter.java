package work.daqian.myai.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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

import static work.daqian.myai.util.ChatUtil.toJson;

@Component
@RequiredArgsConstructor
public class OllamaModelAdapter implements ModelAdapter {

    private static final String BASE_URL = "http://127.0.0.1:11434";
    private final ObjectMapper mapper = new ObjectMapper();
    private final WebClient.Builder builder;

    @Override
    public Provider getProvider() {
        return Provider.OLLAMA;
    }

    @Override
    public WebClient buildWebClient() {
        HttpClient httpClient = HttpClient.newConnection();
        return builder.baseUrl(BASE_URL)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public String getUri(String modelName) {
        return "/api/chat";
    }

    @Override
    public Object buildRequest(String modelName, List<Message> prompt, boolean think) {
        return Map.of(
                "model", modelName,
                "messages", prompt,
                "stream", true,
                "think", think
        );
    }

    @Override
    public Flux<String> parseChunk(String chunk, StringBuilder contentBuilder, StringBuilder thinkingBuilder, Long userId, String sessionId) {
        try {
            String[] lines = chunk.split("\n");
            List<String> result = new ArrayList<>();
            synchronized (contentBuilder) {
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    JsonNode node = mapper.readTree(line);
                    JsonNode message = node.path("message");

                    String thinking = message.path("thinking").asText(null);
                    if (thinking != null && !thinking.isEmpty()) {
                        result.add(toJson("thinking", thinking));
                        thinkingBuilder.append(thinking);
                    }
                    String content = message.path("content").asText(null);
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
}
