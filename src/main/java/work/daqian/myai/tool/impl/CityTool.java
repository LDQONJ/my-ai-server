package work.daqian.myai.tool.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import work.daqian.myai.tool.Tool;
import work.daqian.myai.tool.ToolDefinition;
import work.daqian.myai.util.IpUtils;
import work.daqian.myai.websocket.WebSocketService;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CityTool implements Tool {

    private final WebSocketService webSocketService;

    public String getCity(String ip) {
        return IpUtils.getCityFromIp(ip);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return new ToolDefinition(
                "getCity",
                "根据 IP 获取用户所在地",
                """
                        {
                            "ip": "ip地址" /* ipv4或者ipv6地址值，无需特殊处理 */
                        }
                        """
        );
    }

    @Override
    public String doTool(String wsId, Map<String, Object> arguments) {
        webSocketService.sendMessageToClient(wsId, "正在获取所在城市...");
        return getCity((String) arguments.get("ip"));
    }
}
