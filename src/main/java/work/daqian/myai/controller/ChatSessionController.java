package work.daqian.myai.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import work.daqian.myai.common.R;
import work.daqian.myai.domain.vo.ChatSessionVO;
import work.daqian.myai.service.IChatSessionService;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 李达千
 * @since 2026-04-18
 */
@RestController
@RequestMapping("/session")
@RequiredArgsConstructor
@Tag(name = "聊天会话管理接口")
public class ChatSessionController {

    private final IChatSessionService sessionService;

    @GetMapping("/create")
    @Operation(summary = "创建新会话")
    public R<String> create() {
        return sessionService.create();
    }

    @GetMapping("/list")
    @Operation(summary = "获取当前用户的会话列表")
    public R<List<ChatSessionVO>> list() {
        return sessionService.listMySessions();
    }

    @PutMapping
    @Operation(summary = "重命名会话")
    public R<Void> rename(
            @Parameter(description = "会话ID", required = true) @RequestParam("id") String id,
            @Parameter(description = "新标题", required = true) @RequestParam("title") String title) {
        return sessionService.rename(id, title);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除会话")
    public R<Void> delete(@Parameter(description = "会话ID", required = true) @PathVariable("id") String id) {
        return sessionService.delete(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询会话详情")
    public R<ChatSessionVO> queryById(@Parameter(description = "会话ID", required = true) @PathVariable("id") String id) {
        return sessionService.queryById(id);
    }

    @GetMapping(value = "/title/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "根据Id流式更新标题")
    public Flux<String> generateTitle(@PathVariable("id") String id) {
        return sessionService.generateTitle(id);
    }

}
