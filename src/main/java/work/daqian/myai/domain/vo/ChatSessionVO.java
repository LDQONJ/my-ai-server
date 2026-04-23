package work.daqian.myai.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "聊天会话视图对象")
public class ChatSessionVO {
    @Schema(description = "对话 id")
    private String id;
    @Schema(description = "会话名称", example = "新会话")
    private String title;
    @Schema(description = "最近一次 AI 回复的消息")
    private String lastMessage;
    @Schema(description = "消息列表")
    private List<ChatMessageVO> messages;
}
