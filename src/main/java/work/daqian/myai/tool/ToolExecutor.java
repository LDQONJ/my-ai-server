package work.daqian.myai.tool;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import work.daqian.myai.tool.impl.CityTool;
import work.daqian.myai.tool.impl.WeatherTool;
import work.daqian.myai.tool.impl.WebSearchTool;
import work.daqian.myai.websocket.WebSocketService;

@Service
@RequiredArgsConstructor
public class ToolExecutor {

    private final WeatherTool weatherTool;

    private final WebSearchTool webSearchTool;

    private final CityTool cityTool;

    private final WebSocketService webSocketService;

    public String execute(ToolCall call, String wsId) {
        if (call == null || call.getTool() == null) return null;

        return switch (call.getTool()) {
            case "getCity" -> {
                webSocketService.sendMessageToClient(wsId, "正在获取所在城市...");
                String ip = (String) call.getArguments().get("ip");
                yield cityTool.getCity(ip);
            }
            case "getWeather" -> {
                webSocketService.sendMessageToClient(wsId, "正在获取天气信息...");
                String city = (String) call.getArguments().get("city");
                String version = (String) call.getArguments().get("version");
                yield weatherTool.getWeather(city, version);
            }
            case "webSearch" -> {
                String query = (String) call.getArguments().get("query");
                webSocketService.sendMessageToClient(wsId, "正在联网搜索: “" + query + "”...");
                yield webSearchTool.webSearch(wsId, query);
            }
            default -> "未知工具";
        };

    }
}
