package work.daqian.myai.tool.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSearchTool implements InitializingBean {

    private WebClient webClient;

    private final ObjectMapper mapper;

    @Value("${api.key.search}")
    private String apiKey;

    public String webSearch(String query) {
        Map<String, String> request = Map.of(
                "query", query
        );
        String json = webClient.post()
                .uri("/search")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        String results = null;
        try {
            JsonNode node = mapper.readTree(json);
            results = node.path("results").toString();
        } catch (Exception e) {
        }
        return results;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.tavily.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bear " + apiKey)
                .build();
    }
}
