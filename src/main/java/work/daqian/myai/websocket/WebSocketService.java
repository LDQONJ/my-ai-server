package work.daqian.myai.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    // 存储每个 WebSocket 会话
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 添加 WebSocket 会话
    public void addSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    // 移除 WebSocket 会话
    public void removeSession(WebSocketSession session) {
        sessions.remove(session.getId());
    }

    // 向所有连接的客户端发送消息
    public void sendMessageToAll(String message) {
        for (WebSocketSession session : sessions.values()) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (Exception e) {
                log.error("广播消息失败");
            }
        }
    }

    // 向特定客户端发送消息
    public void sendMessageToClient(String sessionId, String message) {
        if (sessionId == null) return;
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (Exception e) {
                log.error("发送消息失败，wsId: {}, 消息内容: {}", sessionId, message);
            }
        }
    }
}
