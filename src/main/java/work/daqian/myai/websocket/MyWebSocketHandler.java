package work.daqian.myai.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class MyWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketService webSocketService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 连接成功时，保存 wsId 和 WebSocket 会话
        webSocketService.addSession(session);
        System.out.println("新的ws连接已建立，wsId: " + session.getId());

        // 将 wsId 传递给客户端
        String wsId = session.getId();
        session.sendMessage(new TextMessage(wsId));
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws InterruptedException, IOException {
        // 处理来自客户端的消息
        log.info("收到客户端消息: {}", message.getPayload());

        // 响应给客户端
        String responseMessage = "已收到消息: " + message.getPayload();
        session.sendMessage(new TextMessage(responseMessage));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 客户端断开时移除会话
        webSocketService.removeSession(session);
        System.out.println("ws连接断开，wsId: " + session.getId());
    }
}
