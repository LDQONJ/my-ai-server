package work.daqian.myai.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import work.daqian.myai.domain.po.ModelUsageDetail;
import work.daqian.myai.service.IModelUsageDetailService;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatUtil implements ApplicationContextAware {

    private static ApplicationContext context;
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String toJson(String type, String text) {
        try {
            return mapper.writeValueAsString(
                    Map.of(
                            "type", type,
                            "content", text
                    )
            );
        } catch (Exception e) {
            return "";
        }
    }

    public static String toSSEDone() {
        return "{\"done\":true}";
    }

    public static void saveUsageDetail(Long userId, String sessionId, String modelName, Integer promptTokens, Integer completionTokens,
                                       Integer totalTokens, Integer reasoningTokens, Integer cachedTokens) {
        ModelUsageDetail usageDetail = new ModelUsageDetail();
        usageDetail.setUserId(userId);
        usageDetail.setModelName(modelName);
        usageDetail.setSessionId(sessionId);
        usageDetail.setPromptTokens(promptTokens);
        usageDetail.setCompletionTokens(completionTokens);
        usageDetail.setTotalTokens(totalTokens);
        usageDetail.setReasoningTokens(reasoningTokens);
        usageDetail.setCachedTokens(cachedTokens);
        IModelUsageDetailService usageDetailService = context.getBean(IModelUsageDetailService.class);
        usageDetailService.saveAndAdd(usageDetail);
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static void debugRequestBody(Object requestMap) {
        try {
            String jsonBody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestMap);
            log.error("===== 请求体 开始 =====");
            log.error(jsonBody);
            log.error("===== 请求体 结束 =====");
        } catch (Exception e) {
            log.error("序列化请求体失败: {}", e.getMessage());
        }
    }
}
