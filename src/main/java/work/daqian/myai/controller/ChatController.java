package work.daqian.myai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import work.daqian.myai.domain.dto.ChatFormDTO;
import work.daqian.myai.service.ChatService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
@Tag(name = "聊天接口")
public class ChatController {

    private final ChatService chatService;

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式聊天", description = "通过 Server-Sent Events (SSE) 发送聊天内容")
    public Flux<String> streamChat(@RequestBody ChatFormDTO chatForm) {
        // log.info("收到聊天消息：{}", chatForm);
        return chatService.streamChat(chatForm);
    }
}
