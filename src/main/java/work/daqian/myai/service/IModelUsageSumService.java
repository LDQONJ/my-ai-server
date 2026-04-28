package work.daqian.myai.service;

import work.daqian.myai.domain.po.ModelUsageSum;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Collection;

/**
 * <p>
 * 模型使用量汇总 服务类
 * </p>
 *
 * @author 李达千
 * @since 2026-04-27
 */
public interface IModelUsageSumService extends IService<ModelUsageSum> {

    void upsertBatch(Collection<ModelUsageSum> usageSums);
}
