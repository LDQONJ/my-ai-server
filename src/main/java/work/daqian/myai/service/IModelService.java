package work.daqian.myai.service;

import work.daqian.myai.common.R;
import work.daqian.myai.domain.po.Model;
import com.baomidou.mybatisplus.extension.service.IService;
import work.daqian.myai.domain.vo.ModelVO;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 * LLM 模型 服务类
 * </p>
 *
 * @author 李达千
 * @since 2026-04-22
 */
public interface IModelService extends IService<Model> {

    R<List<ModelVO>> listAllModel();

    R<Void> changeModel(Long id);

    AtomicReference<String> getCurrentModel();

    R<ModelVO> currentModel();
}
