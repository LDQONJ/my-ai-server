package work.daqian.myai.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import work.daqian.myai.domain.po.ModelUsageDetail;
import work.daqian.myai.domain.po.ModelUsageSum;
import work.daqian.myai.mapper.ModelUsageDetailMapper;
import work.daqian.myai.mapper.ModelUsageSumMapper;
import work.daqian.myai.util.CollUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static work.daqian.myai.constant.RedisConstants.USAGE_DETAIL_PREFIX;
import static work.daqian.myai.constant.RedisConstants.USAGE_SUM_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsageDetailTask {

    private final StringRedisTemplate redisTemplate;

    private final ModelUsageDetailMapper usageDetailMapper;

    private final ModelUsageSumMapper usageSumMapper;

    @Scheduled(cron = "0 */5 * * * *")
    //@Transactional
    public void saveUsageDetailToDB() {
        LocalDate today = LocalDate.now();
        String detailKey = USAGE_DETAIL_PREFIX + today;
        String sumKey = USAGE_SUM_PREFIX + today;
        List<Object> result = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            connection.listCommands().lRange(detailKey.getBytes(), 0, -1);
            connection.hashCommands().hGetAll(sumKey.getBytes());
            connection.keyCommands().del(detailKey.getBytes());
            connection.keyCommands().del(sumKey.getBytes());
            return null;
        });
        List<String> uds = (List<String>) result.get(0);
        Map<Object, Object> entries = (Map<Object, Object>) result.get(1);
        if (CollUtils.isEmpty(uds)) return;
        List<ModelUsageDetail> usageDetails = uds.stream().map(ud -> {
            String[] split = ud.split(",");
            ModelUsageDetail usageDetail = null;
            if (split.length >= 9) {
                usageDetail = new ModelUsageDetail();
                usageDetail.setUserId(Long.parseLong(split[0]));
                usageDetail.setModelName(split[1]);
                usageDetail.setSessionId(split[2]);
                usageDetail.setPromptTokens(Integer.parseInt(split[3]));
                usageDetail.setCompletionTokens(Integer.parseInt(split[4]));
                usageDetail.setTotalTokens(Integer.parseInt(split[5]));
                usageDetail.setReasoningTokens(Integer.parseInt(split[6]));
                usageDetail.setCachedTokens(Integer.parseInt(split[7]));
                usageDetail.setCreateTime(LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(split[8])), ZoneId.systemDefault()));
            }
            return usageDetail;
        }).filter(Objects::nonNull).toList();
        usageDetailMapper.insertBatch(usageDetails);


        Map<String, ModelUsageSum> usageSumMap = new HashMap<>(Math.max(entries.size() >> 2, 8));
        entries.forEach((k, v) -> {
            // 2045384076902391809:qwen3.5:9b:r
            String[] split = ((String) k).split(":");
            long userId = Long.parseLong(split[0]);
            String modelName;
            String tokenName;
            if (split.length >= 4) {
                // ollama 模型名字中间带了冒号
                modelName = split[1] + ":" + split[2];
                tokenName = split[3];
            } else {
                modelName = split[1];
                tokenName = split[2];
            }
            String key = userId + modelName;
            ModelUsageSum usageSum = usageSumMap.get(key);
            if (usageSum == null)
                usageSum = new ModelUsageSum();
            Long tokenValue = Long.parseLong((String) v);
            switch (tokenName) {
                case "p":
                    usageSum.setPromptTokens(usageSum.getPromptTokens() + tokenValue);
                    break;
                case "co":
                    usageSum.setCompletionTokens(usageSum.getCompletionTokens() + tokenValue);
                    break;
                case "t":
                    usageSum.setTotalTokens(usageSum.getTotalTokens() + tokenValue);
                    break;
                case "r":
                    usageSum.setReasoningTokens(usageSum.getReasoningTokens() + tokenValue);
                    break;
                case "ca":
                    usageSum.setCachedTokens(usageSum.getCachedTokens() + tokenValue);
                    break;
            }
            usageSum.setUserId(userId);
            usageSum.setModelName(modelName);
            usageSum.setPeriodDate(today);
            usageSumMap.put(key, usageSum);
        });
        Collection<ModelUsageSum> usageSums = usageSumMap.values();
        usageSumMapper.upsertBatch(usageSums);
    }
}
