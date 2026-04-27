package work.daqian.myai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import work.daqian.myai.common.R;
import work.daqian.myai.common.SecurityUserDetails;
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

import java.util.*;
import java.util.stream.Collectors;

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

    @Override
    public R<List<UsageDetailVO>> queryUsageDetail() {
        SecurityUserDetails userDetails = SecurityUtils.getCurrentUser();
        if (userDetails == null) throw new BadRequestException("请先登录账号");
        List<ModelUsageDetail> usageDetails = lambdaQuery()
                .eq(ModelUsageDetail::getUserId, userDetails.getId())
                .orderByDesc(ModelUsageDetail::getCreateTime)
                .list();
        if (usageDetails.isEmpty()) return R.ok(List.of());
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
        return R.ok(vos);
    }
}
