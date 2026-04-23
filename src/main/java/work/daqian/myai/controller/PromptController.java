package work.daqian.myai.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import work.daqian.myai.common.R;
import work.daqian.myai.domain.dto.PromptDTO;
import work.daqian.myai.service.ContextService;

@Slf4j
@RestController
@RequestMapping("/prompt")
@RequiredArgsConstructor
@Tag(name = "提示词相关接口")
public class PromptController {

    private final ContextService contextService;

    @Operation(summary = "查询全局提示词")
    @GetMapping
    public R<PromptDTO> queryGlobalSystemPrompt() {
        return contextService.queryGlobalSystemPrompt();
    }

    @Operation(summary = "设置全局提示词")
    @PostMapping
    public R<Void> updateGlobalSystemPrompt(@RequestBody PromptDTO prompt) {
        return contextService.updateGlobalSystemPrompt(prompt);
    }

    @Operation(summary = "查询对话提示词")
    @GetMapping("/{id}")
    public R<PromptDTO> querySessionSystemPrompt(@PathVariable("id") String id) {
        return contextService.querySessionSystemPrompt(id);
    }

    @Operation(summary = "设置对话提示词")
    @PostMapping("/{id}")
    public R<Void> updateSessionSystemPrompt(@PathVariable("id") String id, @RequestBody PromptDTO prompt) {
        return contextService.updateSessionSystemPrompt(id, prompt);
    }
}
