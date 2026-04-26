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
import work.daqian.myai.enums.Provider;
import work.daqian.myai.mapper.ModelMapper;
import work.daqian.myai.service.IModelService;
import work.daqian.myai.util.BeanUtils;
import work.daqian.myai.util.RedisUtil;
import work.daqian.myai.util.SecurityAssert;

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
        String providerAndName = redisUtil.cacheEmptyIfNE(
                "model:id:",
                id,
                Duration.ofHours(1),
                (mid) -> {
                    Model model = getById(mid);
                    return model.getProvider().getValue() + "," + model.getFullName();
                }
        );
        String[] split = providerAndName.split(",");
        if (split.length == 2)
            if (split[0].equals("1")) {
                // 只缓存Ollama本地模型，防止生成标题以及提取摘要时模型频繁更换
                currentModel.set(split[1]);
            } else {
                // 校验是否有对该模型的使用权限
                SecurityAssert.canAccessModel(Provider.fromValue(Integer.parseInt(split[0])));
            }
        return R.ok();
    }

    @Override
    public void afterPropertiesSet() {
        this.currentModel = new AtomicReference<>(defaultModel);
    }
}
