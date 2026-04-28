package work.daqian.myai.mapper;

import org.apache.ibatis.annotations.Param;
import work.daqian.myai.domain.po.ModelUsageSum;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.Collection;

/**
 * <p>
 * 模型使用量汇总 Mapper 接口
 * </p>
 *
 * @author 李达千
 * @since 2026-04-27
 */
public interface ModelUsageSumMapper extends BaseMapper<ModelUsageSum> {

    void upsertBatch(@Param("usageSums") Collection<ModelUsageSum> usageSums);
}
