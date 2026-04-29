package work.daqian.myai.tool.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import work.daqian.myai.tool.Tool;
import work.daqian.myai.tool.ToolDefinition;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSearchTool implements Tool,InitializingBean {

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
        String zippedResult = null;
        try {
            JsonNode node = mapper.readTree(json);
            JsonNode results = node.path("results");
            SearchResult[] searchResults = mapper.convertValue(results, SearchResult[].class);
            for (SearchResult searchResult : searchResults) {
                // 5 条结果每条结果内容最多留 200 字，否则可能一次消耗数万 token
                String content = searchResult.getContent();
                searchResult.setContent(content.substring(0, Math.min(200, content.length())));
            }
            zippedResult = mapper.writeValueAsString(searchResults);
        } catch (Exception e) {
        }
        return zippedResult;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.tavily.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bear " + apiKey)
                .build();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return null;
    }

    @Data
    static class SearchResult {
        private String title;
        private String content;
        private Double score;
    }
}
