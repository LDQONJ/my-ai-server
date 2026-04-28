package work.daqian.myai.adapter;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import work.daqian.myai.domain.dto.Message;
import work.daqian.myai.enums.Provider;

import java.util.List;

/**
 * 模型适配器：屏蔽不同 AI 平台的请求/响应差异
 */
public interface ModelAdapter {

    /**
     * 适配器支持的平台
     */
    Provider getProvider();

    /**
     * 构建 WebClient
     */
    WebClient buildWebClient();

    /**
     * 获取请求 URI，可能包含动态参数（模型名、API Key 等）
     */
    String getUri(String modelName);

    /**
     * 构建请求体（JSON 对象，会直接传给 WebClient.bodyValue）
     */
    Object buildRequest(String modelName, List<Message> prompt, boolean stream, boolean think);

    /**
     * 解析流式 chunk，返回给前端的事件流（同步操作，内部通过回调收集 token）
     */
    Flux<String> parseChunk(String chunk,
                            StringBuilder contentBuilder,
                            StringBuilder thinkingBuilder,
                            Long userId, String sessionId);

}
