package work.daqian.myai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import work.daqian.myai.common.R;
import work.daqian.myai.domain.po.Model;
import work.daqian.myai.domain.vo.ModelVO;
import work.daqian.myai.exception.BadRequestException;
import work.daqian.myai.mapper.ModelMapper;
import work.daqian.myai.service.IModelService;
import work.daqian.myai.util.BeanUtils;
import work.daqian.myai.util.RedisUtil;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 * LLM 模型 服务实现类
 * </p>
 *
 * @author 李达千
 * @since 2026-04-22
 */
@Getter
@Service
@RequiredArgsConstructor
public class ModelServiceImpl extends ServiceImpl<ModelMapper, Model> implements IModelService, InitializingBean {

    @Value("${ldq.ai.default-model}")
    private String defaultModel;

    private AtomicReference<String> currentModel;

    private final RedisUtil redisUtil;

    private final ObjectMapper mapper;

    @Override
    public R<String> listAllModel() {
        String modelList = redisUtil.cacheEmptyIfNE("model:list", "", Duration.ofHours(1), (id) -> {
            List<Model> list = lambdaQuery().eq(Model::getEnable, true).list();
            if (list == null || list.isEmpty()) return "";
            List<ModelVO> vos = BeanUtils.copyList(list, ModelVO.class);
            String vosJson;
            try {
                vosJson = mapper.writeValueAsString(vos);
            } catch (JsonProcessingException e) {
                return "";
            }
            return vosJson;
        });
        return R.ok(modelList);
    }

    @Override
    public R<Void> changeModel(Long id) {
        String current = currentModel.get();
        Model model = getById(id);
        if (model == null) throw new BadRequestException("模型不存在");
        boolean success = currentModel.compareAndSet(current, model.getFullName());
        if (!success) throw new BadRequestException("修改失败");
        return R.ok();
    }

    @Override
    public R<ModelVO> currentModel() {
        String current = currentModel.get();
        Model model = lambdaQuery().eq(Model::getFullName, current).one();
        ModelVO modelVO = BeanUtils.copyBean(model, ModelVO.class);
        return R.ok(modelVO);
    }

    @Override
    public void afterPropertiesSet() {
        this.currentModel = new AtomicReference<>(defaultModel);
    }
}
