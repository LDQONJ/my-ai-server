package work.daqian.myai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import work.daqian.myai.common.PageDTO;
import work.daqian.myai.common.R;
import work.daqian.myai.domain.dto.UsageDetailPageQuery;
import work.daqian.myai.domain.po.ModelUsageDetail;
import work.daqian.myai.domain.vo.UsageDetailVO;

/**
 * <p>
 * 记录用户对每个模型的单次调用使用量 服务类
 * </p>
 *
 * @author 李达千
 * @since 2026-04-26
 */
public interface IModelUsageDetailService extends IService<ModelUsageDetail> {

    R<PageDTO<UsageDetailVO>> queryMyUsageDetailPage(UsageDetailPageQuery pageQuery);

    void saveAndAdd(ModelUsageDetail usageDetail);
}
