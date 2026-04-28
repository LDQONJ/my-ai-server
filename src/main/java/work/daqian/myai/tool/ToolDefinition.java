package work.daqian.myai.tool;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ToolDefinition {

    private String name;

    private String description;

    private String parameters;
}
