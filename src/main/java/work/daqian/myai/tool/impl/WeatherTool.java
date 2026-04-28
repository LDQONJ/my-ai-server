package work.daqian.myai.tool.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class WeatherTool implements InitializingBean {

    private WebClient webClient;

    @Value("${api.key.weather.id}")
    private String appId;

    @Value("${api.key.weather.secret}")
    private String appSecret;


    public String getWeather(String city) {
        return webClient.get()
                .uri("/api?unescape=1&version=v63&appid=" + appId + "&appsecret=" + appSecret + "&city=" + city)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.webClient = WebClient.builder()
                .baseUrl("http://pddfps.tianqiapi.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
