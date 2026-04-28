package work.daqian.myai.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import work.daqian.myai.common.PageDTO;
import work.daqian.myai.common.R;
import work.daqian.myai.domain.dto.UsageDetailPageQuery;
import work.daqian.myai.domain.vo.UsageDetailVO;
import work.daqian.myai.service.IModelUsageDetailService;

/**
 * <p>
 * 记录用户对每个模型的单次调用使用量 前端控制器
 * </p>
 *
 * @author 李达千
 * @since 2026-04-26
 */
@Slf4j
@RestController
@RequestMapping("/usage-detail")
@RequiredArgsConstructor
@Tag(name = "模型使用量相关接口")
public class ModelUsageDetailController {

    private final IModelUsageDetailService usageDetailService;

    @GetMapping("/page")
    @Operation(summary = "查询使用详情", description = "查询用户所有的模型调用记录")
    public R<PageDTO<UsageDetailVO>> queryMyUsageDetailPage(UsageDetailPageQuery pageQuery) {
        return usageDetailService.queryMyUsageDetailPage(pageQuery);
    }
}
