package work.daqian.myai.websocket;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "WebSocket相关接口")
public class WebSocketController {

    @Autowired
    private WebSocketService webSocketService;

    @PostMapping("/send-message")
    public String sendMessage(@RequestParam String sessionId, @RequestBody String message) {
        webSocketService.sendMessageToClient(sessionId, message);
        return "消息已发送";
    }

    @GetMapping("/broadcast-message")
    public String broadcastMessage(@RequestParam String message) {
        webSocketService.sendMessageToAll(message);
        return "消息已广播";
    }
}
