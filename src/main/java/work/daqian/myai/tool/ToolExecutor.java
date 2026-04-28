package work.daqian.myai.tool;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import work.daqian.myai.tool.impl.WeatherTool;
import work.daqian.myai.tool.impl.WebSearchTool;

@Service
@RequiredArgsConstructor
public class ToolExecutor {

    private final WeatherTool weatherTool;

    private final WebSearchTool webSearchTool;

    public String execute(ToolCall call) {
        if (call == null || call.getTool() == null) return null;

        return switch (call.getTool()) {
            case "getWeather" -> {
                String city = (String) call.getArguments().get("city");
                yield weatherTool.getWeather(city);
            }
            case "webSearch" -> {
                String query = (String) call.getArguments().get("query");
                yield webSearchTool.webSearch(query);
            }
            default -> "未知工具";
        };

    }
}
