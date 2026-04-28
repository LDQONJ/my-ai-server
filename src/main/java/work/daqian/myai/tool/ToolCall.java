package work.daqian.myai.tool;

import lombok.Data;

import java.util.Map;

@Data
public class ToolCall {

    private String tool;

    private Map<String, Object> arguments;
}
