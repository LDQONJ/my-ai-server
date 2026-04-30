package work.daqian.myai.domain.po;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document("chat_message")
public class ChatMessage {

    @Id
    private String id;

    private String sessionId;

    private Long userId;

    private String role;

    private String content;

    private String audio;

    private String thinking;

    private String modelName;

    @CreatedDate
    private LocalDateTime createTime;
}
