package work.daqian.myai.tool.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import work.daqian.myai.tool.Tool;
import work.daqian.myai.tool.ToolDefinition;
import work.daqian.myai.websocket.WebSocketService;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TimeTool implements Tool {

    private final WebSocketService webSocketService;

    public String getTime() {
        return LocalDateTime.now().toString();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return new ToolDefinition(
                "getTime",
                "获取当前时刻的时间值", "none"
        );
    }

    @Override
    public String doTool(String wsId, Map<String, Object> arguments) {
        webSocketService.sendMessageToClient(wsId, "正在获取当前时间...");
        return getTime();
    }
}
