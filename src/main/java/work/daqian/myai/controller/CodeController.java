package work.daqian.myai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import work.daqian.myai.common.R;
import work.daqian.myai.service.CodeService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/verifyCode")
@Tag(name = "验证码接口")
public class CodeController {

    private final CodeService codeService;

    @GetMapping
    @Operation(summary = "发送验证码", description = "向指定的目标（邮箱或手机）发送验证码")
    public R<Void> sendCode(@Parameter(description = "目标地址 (邮箱/手机号)", required = true) @RequestParam("target") String target) {
        return codeService.sendCode(target);
    }

}
