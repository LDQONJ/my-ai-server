package work.daqian.myai.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "聊天消息视图对象")
public class ChatMessageVO {
    @Schema(description = "消息的 Id", example = "duh9yda8dha2")
    private String id;
    @Schema(description = "角色 (user/assistant)", example = "user")
    private String role;
    @Schema(description = "思考内容", example = "思考内容")
    private String thinking;
    @Schema(description = "消息内容", example = "你好")
    private String content;
    @Schema(description = "语音文件", example = "/files/audio.mp3")
    private String audio;
    @Schema(description = "用户名或模型名称", example = "ldq or DeepSeek")
    private String name;
}
