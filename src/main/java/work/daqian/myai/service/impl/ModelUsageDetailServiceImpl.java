package work.daqian.myai.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import work.daqian.myai.common.PageDTO;
import work.daqian.myai.common.R;
import work.daqian.myai.common.SecurityUserDetails;
import work.daqian.myai.domain.dto.UsageDetailPageQuery;
import work.daqian.myai.domain.po.ChatSession;
import work.daqian.myai.domain.po.Model;
import work.daqian.myai.domain.po.ModelUsageDetail;
import work.daqian.myai.domain.vo.UsageDetailVO;
import work.daqian.myai.exception.BadRequestException;
import work.daqian.myai.mapper.ModelUsageDetailMapper;
import work.daqian.myai.service.IChatSessionService;
import work.daqian.myai.service.IModelService;
import work.daqian.myai.service.IModelUsageDetailService;
import work.daqian.myai.util.BeanUtils;
import work.daqian.myai.util.SecurityUtils;
import work.daqian.myai.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static work.daqian.myai.constant.RedisConstants.USAGE_DETAIL_PREFIX;
import static work.daqian.myai.constant.RedisConstants.USAGE_SUM_PREFIX;

/**
 * <p>
 * 记录用户对每个模型的单次调用使用量 服务实现类
 * </p>
 *
 * @author 李达千
 * @since 2026-04-26
 */
@Service
@RequiredArgsConstructor
public class ModelUsageDetailServiceImpl extends ServiceImpl<ModelUsageDetailMapper, ModelUsageDetail> implements IModelUsageDetailService {

    private final IModelService modelService;

    private final IChatSessionService sessionService;

    private final StringRedisTemplate redisTemplate;

    @Override
    public R<PageDTO<UsageDetailVO>> queryMyUsageDetailPage(UsageDetailPageQuery pageQuery) {
        SecurityUserDetails userDetails = SecurityUtils.getCurrentUser();
        if (userDetails == null) throw new BadRequestException("请先登录账号");
        Page<ModelUsageDetail> usageDetailPage = lambdaQuery()
                .eq(ModelUsageDetail::getUserId, userDetails.getId())
                .like(!StringUtils.isEmpty(pageQuery.getModelName()), ModelUsageDetail::getModelName, pageQuery.getModelName())
                .orderByDesc(ModelUsageDetail::getCreateTime)
                .page(com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO.of(pageQuery.getPageNo(), pageQuery.getPageSize()));
        List<ModelUsageDetail> usageDetails = usageDetailPage.getRecords();
        if (usageDetails == null || usageDetails.isEmpty()) return R.ok(PageDTO.empty());
        Set<String> modelNames = new HashSet<>(usageDetails.size());
        Set<String> sessionIds = new HashSet<>(usageDetails.size());
        for (ModelUsageDetail usageDetail : usageDetails) {
            modelNames.add(usageDetail.getModelName());
            sessionIds.add(usageDetail.getSessionId());
        }

        List<Model> models = modelService.getByFullNames(modelNames);
        List<ChatSession> sessions = sessionService.getByIds(sessionIds);

        Map<String, String> modelNameMap = models.stream().collect(Collectors.toMap(Model::getFullName, Model::getName));
        Map<String, String> sessionTitleMap = sessions.stream().collect(Collectors.toMap(ChatSession::getId, ChatSession::getTitle));

        List<UsageDetailVO> vos = new ArrayList<>(usageDetails.size());
        for (ModelUsageDetail usageDetail : usageDetails) {
            UsageDetailVO vo = BeanUtils.copyBean(usageDetail, UsageDetailVO.class);
            vo.setModelName(modelNameMap.get(usageDetail.getModelName()));
            vo.setSessionTitle(sessionTitleMap.get(usageDetail.getSessionId()));
            vo.setContentTokens(vo.getCompletionTokens() - vo.getReasoningTokens());
            vos.add(vo);
        }
        return R.ok(PageDTO.of(usageDetailPage.getTotal(), vos));
    }

    @Override
    public void saveAndAdd(ModelUsageDetail usageDetail) {
        LocalDate today = LocalDate.now();
        String detailKey = USAGE_DETAIL_PREFIX + today;
        Long userId = usageDetail.getUserId();
        String modelName = usageDetail.getModelName();
        String sessionId = usageDetail.getSessionId();
        Integer promptTokens = usageDetail.getPromptTokens();
        Integer completionTokens = usageDetail.getCompletionTokens();
        Integer totalTokens = usageDetail.getTotalTokens();
        Integer reasoningTokens = usageDetail.getReasoningTokens();
        Integer cachedTokens = usageDetail.getCachedTokens();
        String detail = userId + ","
                + modelName + ","
                + sessionId + ","
                + promptTokens + ","
                + completionTokens + ","
                + totalTokens + ","
                + reasoningTokens + ","
                + cachedTokens + ","
                + Instant.now().getEpochSecond();
        // usage:detail:2026-04-27 "10086,deepseek-v4-pro,dha87dg8gdau34dhiasuddas,80,120,200,80,0,1777171526"
        redisTemplate.opsForList().rightPush(detailKey, detail);
        String hashFieldPrefix = userId + ":" + modelName;
        // usage:sum:2026-04-27 10086:deepseek-v4-pro:p ↑80
        String sumKey = USAGE_SUM_PREFIX + today;
        redisTemplate.opsForHash().increment(sumKey, hashFieldPrefix + ":p", promptTokens);
        redisTemplate.opsForHash().increment(sumKey, hashFieldPrefix + ":co", completionTokens);
        redisTemplate.opsForHash().increment(sumKey, hashFieldPrefix + ":t", totalTokens);
        redisTemplate.opsForHash().increment(sumKey, hashFieldPrefix + ":r", reasoningTokens);
        redisTemplate.opsForHash().increment(sumKey, hashFieldPrefix + ":ca", cachedTokens);
    }
}
