package work.daqian.myai.mapper;

import org.apache.ibatis.annotations.Param;
import work.daqian.myai.domain.po.Model;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * LLM 模型 Mapper 接口
 * </p>
 *
 * @author 李达千
 * @since 2026-04-22
 */
public interface ModelMapper extends BaseMapper<Model> {

    List<Model> selectBatchFullNames(@Param("modelNames") Set<String> modelNames);
}
