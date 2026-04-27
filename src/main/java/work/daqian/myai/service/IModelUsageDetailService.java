package work.daqian.myai.service;

import work.daqian.myai.common.R;
import work.daqian.myai.domain.po.ModelUsageDetail;
import com.baomidou.mybatisplus.extension.service.IService;
import work.daqian.myai.domain.vo.UsageDetailVO;

import java.util.List;

/**
 * <p>
 * 记录用户对每个模型的单次调用使用量 服务类
 * </p>
 *
 * @author 李达千
 * @since 2026-04-26
 */
public interface IModelUsageDetailService extends IService<ModelUsageDetail> {

    R<List<UsageDetailVO>> queryUsageDetail();
}
