package work.daqian.myai.domain.po;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import work.daqian.myai.prompt.PromptType;

@Data
@Document("system_prompt")
public class SystemPrompt {
    @Id
    private String id;

    private String sessionId;

    private Long userId;

    private PromptType type;

    private String content;
}
