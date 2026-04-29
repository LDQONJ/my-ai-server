package work.daqian.myai.advice;


import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import work.daqian.myai.common.R;
import work.daqian.myai.constant.Constant;
import work.daqian.myai.exception.CommonException;
import work.daqian.myai.exception.DbException;
import work.daqian.myai.util.WebUtils;

@RestControllerAdvice
@Slf4j
public class CommonExceptionAdvice {

    /**
     * 处理 WebClient 网络层异常（连接失败、超时、DNS解析失败等）
     * 这是请求根本没到达对方服务，或者对方服务完全不可达的情况
     */
    @ExceptionHandler(WebClientRequestException.class)
    public Object handleWebClientRequestException(WebClientRequestException e) {
        String uri = e.getUri() != null ? e.getUri().toString() : "未知地址";
        Throwable rootCause = e.getRootCause();

        // 根据不同的根因返回更精确的提示
        if (rootCause instanceof java.net.ConnectException) {
            log.error("api 连接失败 -> 目标地址: {}, 异常: {}", uri, rootCause.getMessage());
            return processResponse(503, 503, "模型暂时不可用");
        }

        if (rootCause instanceof io.netty.channel.ConnectTimeoutException
                || rootCause instanceof java.net.ConnectException
                && rootCause.getMessage().contains("timeout")) {
            log.error("api 连接超时 -> 目标地址: {}", uri);
            return processResponse(504, 504, "模型暂时不可用");
        }

        if (rootCause instanceof java.net.UnknownHostException) {
            log.error("外部服务域名解析失败 -> 目标地址: {}", uri);
            return processResponse(503, 503, "api 接口地址解析失败");
        }

        if (rootCause instanceof javax.net.ssl.SSLException) {
            log.error("外部服务SSL握手失败 -> 目标地址: {}", uri, rootCause);
            return processResponse(502, 502, "api 接口安全连接失败");
        }

        // 其他网络层异常
        log.error("WebClient网络请求异常 -> 目标地址: {}, 请求方式: {}", uri, e.getMethod(), e);
        return processResponse(502, 502, "模型暂时不可用，请稍后重试");
    }

    /**
     * 处理 WebClient 响应异常（对方服务返回了4xx/5xx）
     * 按照具体的HTTP状态码细分处理
     */
    @ExceptionHandler(WebClientResponseException.class)
    public Object handleWebClientResponseException(WebClientResponseException e) {
        int statusCode = e.getRawStatusCode();
        String requestUrl = e.getRequest().getURI() != null ? e.getRequest().getURI().toString() : "未知地址";

        log.error("api 接口返回错误 -> 状态码: {}, 地址: {}, 响应: {}",
                statusCode, requestUrl, e.getResponseBodyAsString());

        // 根据具体的HTTP状态码返回不同的业务响应
        switch (statusCode) {
            case 400:
                return processResponse(400, 400, "模型参数格式错误");
            case 401:
                return processResponse(401, 401, "api 接口认证失败");
            case 403:
                return processResponse(403, 403, "api 接口拒绝访问");
            case 404:
                return processResponse(404, 404, "api 接口资源不存在");
            case 405:
                return processResponse(405, 405, "api 接口请求方法不允许");
            case 408:
                return processResponse(408, 408, "api 接口请求超时");
            case 429:
                return processResponse(429, 429, "api 接口请求过于频繁");
            case 500:
                return processResponse(502, 502, "api 接口内部错误");
            case 502:
                return processResponse(502, 502, "api 接口网关错误");
            case 503:
                return processResponse(503, 503, "api 接口暂时不可用");
            case 504:
                return processResponse(504, 504, "api 接口网关超时");
            default:
                if (statusCode >= 400 && statusCode < 500) {
                    return processResponse(400, statusCode, "api 接口客户端错误(" + statusCode + ")");
                } else if (statusCode >= 500) {
                    return processResponse(502, statusCode, "api 接口服务器错误(" + statusCode + ")");
                }
                return processResponse(502, statusCode, "api 接口异常(" + statusCode + ")");
        }
    }

    @ExceptionHandler(WebClientResponseException.ServiceUnavailable.class)
    public Object handleServiceUnavailableException(WebClientResponseException.ServiceUnavailable e) {
        String message = e.getMessage();
        log.error("api 调用失败 -> {}", message);
        if (message.startsWith("503 ")) {
            return processResponse(503, 503, "api 暂时不可用");
        }
        return processResponse(400, 400, "api 调用异常");
    }

    /**
     * 兜底处理所有 WebClientException（以防有新增的子类异常）
     */
    @ExceptionHandler(WebClientException.class)
    public Object handleWebClientException(WebClientException e) {
        log.error("WebClient通用异常 -> {}", e.getMessage(), e);
        return processResponse(502, 502, "api 接口调用异常");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDeniedException(AccessDeniedException e) {
        log.debug("访问被拒绝 -> {}", e.getMessage());
        return processResponse(403, 403, "没有访问权限");
    }

    @ExceptionHandler(AuthenticationException.class)
    public Object handleAuthenticationException(AuthenticationException e) {
        log.debug("认证失败 -> {}", e.getMessage());
        return processResponse(401, 401, "未登录或登录状态失效");
    }

    @ExceptionHandler(DbException.class)
    public Object handleDbException(DbException e) {
        log.error("mysql数据库操作异常 -> ", e);
        return processResponse(e.getStatus(), e.getCode(), e.getMessage());
    }

    @ExceptionHandler(CommonException.class)
    public Object handleBadRequestException(CommonException e) {
        log.error("自定义异常 -> {} , 状态码：{}, 异常原因：{}  ", e.getClass().getName(), e.getStatus(), e.getMessage());
        log.debug("", e);
        return processResponse(e.getStatus(), e.getCode(), e.getMessage());
    }


    @ExceptionHandler(Exception.class)
    public Object handleRuntimeException(Exception e) {
        log.error("其他异常 uri : {} -> ", WebUtils.getRequest().getRequestURI(), e);
        return processResponse(500, 500, "服务器内部异常");
    }

    private Object processResponse(int status, int code, String msg) {
        // 1.标记响应异常已处理（避免重复处理）
        WebUtils.setResponseHeader(Constant.BODY_PROCESSED_MARK_HEADER, "true");
        // 2.统一返回 R 对象封装，包含业务状态码、错误信息和请求 ID
        return R.error(code, msg).requestId(MDC.get(Constant.REQUEST_ID_HEADER));
    }
}
