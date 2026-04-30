package work.daqian.myai.tool;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ToolExecutor implements InitializingBean {

    private final List<Tool> tools;

    private Map<String, Tool> toolMap;

    public String execute(ToolCall call, String wsId) {
        if (call == null) return null;
        String toolName = call.getTool();
        return toolMap.get(toolName).doTool(wsId, call.getArguments());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        toolMap = tools.stream()
                .collect(Collectors.toMap(tool -> tool.getToolDefinition().getName(), tool -> tool));
    }
}
