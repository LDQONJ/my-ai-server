package work.daqian.myai.mapper;

import org.apache.ibatis.annotations.Param;
import work.daqian.myai.domain.po.ModelUsageDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 * 记录用户对每个模型的单次调用使用量 Mapper 接口
 * </p>
 *
 * @author 李达千
 * @since 2026-04-26
 */
public interface ModelUsageDetailMapper extends BaseMapper<ModelUsageDetail> {

    void insertBatch(@Param("usageDetails") List<ModelUsageDetail> usageDetails);
}
