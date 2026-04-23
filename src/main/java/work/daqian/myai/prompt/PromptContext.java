package work.daqian.myai.prompt;

import lombok.Builder;
import lombok.Data;
import work.daqian.myai.domain.dto.Message;

import java.util.List;

@Data
@Builder
public class PromptContext {

    // 最近消息
    private List<Message> history;

    // 历史摘要
    private String summary;

    // 人格设定
    private String persona;

    // 规则
    private String rules;

    // 用户记忆（暂不实现）
    // private String memory;

    // 用户当前输入
    private Message userInput;

}
