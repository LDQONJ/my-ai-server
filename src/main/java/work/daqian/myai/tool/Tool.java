package work.daqian.myai.tool;

import java.util.Map;

public interface Tool {

    ToolDefinition getToolDefinition();

    String doTool(String wsId, Map<String, Object> arguments);
}
