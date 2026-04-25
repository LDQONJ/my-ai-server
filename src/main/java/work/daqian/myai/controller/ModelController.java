package work.daqian.myai.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import work.daqian.myai.common.R;
import work.daqian.myai.domain.vo.ModelVO;
import work.daqian.myai.service.IModelService;

/**
 * <p>
 * LLM 模型 前端控制器
 * </p>
 *
 * @author 李达千
 * @since 2026-04-22
 */
@RestController
@RequestMapping("/model")
@RequiredArgsConstructor
@Tag(name = "LLM 模型相关接口")
public class ModelController {

    private final IModelService modelService;

    @Operation(summary = "可用模型列表", description = "获取所有可用模型")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                    {
                        "code": 200,
                        "msg": "OK",
                        "data": [
                            {
                                "id": 1,
                                "name": "DeepSeek",
                                "description": "xxx"
                            }
                        ]
                    }
                    """)))
    @GetMapping("/list")
    public R<String> listAllModel() {
        return modelService.listAllModel();
    }

    @Operation(summary = "更换模型", description = "切换成指定模型")
    @GetMapping("/change")
    public R<Void> changeModel(@RequestParam("id") Long id) {
        return modelService.changeModel(id);
    }

    @Operation(summary = "当前模型", description = "获取当前正在使用的模型")
    @GetMapping
    public R<ModelVO> currentModel() {
        return modelService.currentModel();
    }

}
