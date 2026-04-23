package work.daqian.myai.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "聊天请求表单")
public class ChatFormDTO {
    @Schema(description = "会话ID", example = "1")
    private String sessionId;
    @Schema(description = "聊天内容", example = "你好")
    private String text;
    @Schema(description = "是否开启深度思考", example = "false")
    private Boolean think;
    @Schema(description = "是否开启提示词功能", example = "true")
    private Boolean prompt;
}
