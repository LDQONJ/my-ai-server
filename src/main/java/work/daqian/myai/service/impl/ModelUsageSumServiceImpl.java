package work.daqian.myai.service.impl;

import work.daqian.myai.domain.po.ModelUsageSum;
import work.daqian.myai.mapper.ModelUsageSumMapper;
import work.daqian.myai.service.IModelUsageSumService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * <p>
 * 模型使用量汇总 服务实现类
 * </p>
 *
 * @author 李达千
 * @since 2026-04-27
 */
@Service
public class ModelUsageSumServiceImpl extends ServiceImpl<ModelUsageSumMapper, ModelUsageSum> implements IModelUsageSumService {

    @Override
    public void upsertBatch(Collection<ModelUsageSum> usageSums) {
        baseMapper.upsertBatch(usageSums);
    }
}
