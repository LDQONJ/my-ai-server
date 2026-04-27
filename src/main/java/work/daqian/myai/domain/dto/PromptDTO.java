package work.daqian.myai.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "提示词")
public class PromptDTO {
    @Schema(description = "AI 角色设定")
    private String persona;
    @Schema(description = "AI 回复规则")
    private String rules;
}
