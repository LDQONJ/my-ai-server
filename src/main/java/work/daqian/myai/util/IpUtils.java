package work.daqian.myai.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
public class IpUtils {

    private final static WebClient webClient = WebClient.builder()
            .baseUrl("http://ip-api.com")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR",
            "X-Real-IP"
    };

    /**
     * 从 HttpServletRequest 获取真实 IP
     */
    public static String getClientIp(HttpServletRequest request) {
        // 1. 先检查代理头
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For 可能包含多个 IP，取第一个
                int index = ip.indexOf(',');
                if (index != -1) {
                    return ip.substring(0, index).trim();
                }
                return ip.trim();
            }
        }
        // 2. 没有代理头，直接用 getRemoteAddr()
        return request.getRemoteAddr();
    }

    /**
     * 从 Spring 上下文获取
     */
    public static String getCurrentIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
            return getClientIp(request);
        }
        return "unknown";
    }

    /**
     * 根据 Ip 获取城市
     */
    public static String getCityFromIp(String ip) {
        Map response = null;
        int retry = 0;
        while (response == null && retry < 3) {
            try {
                response = doGetCity(ip);
            } catch (Exception e) {
                log.error("获取地址失败");
                retry++;
            }
        }
        if (response == null) return "北京";
        return response.get("city").toString();
    }

    @Nullable
    private static Map doGetCity(String ip) {
        Map response;
        response = webClient.get()
                .uri("/json/" + ip + "?lang=zh-CN")
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        return response;
    }
}
