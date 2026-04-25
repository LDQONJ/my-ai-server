package work.daqian.myai.prompt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import work.daqian.myai.exception.DbException;

@Getter
@AllArgsConstructor
public enum PromptType {
    GLOBAL_PERSONA(1, "context:persona:uid:"),
    GLOBAL_RULES(2, "context:rules:uid"),
    PERSONA(3, "context:persona:sid:"),
    RULES(4, "context:rules:sid:"),
    SUMMARY(5, "context:summary:sid:"),
    HISTORY(6, "context:history:sid:"),
    ;
    private final int value;
    private final String keyPrefix;

    public static PromptType fromValue(int value) {
        for (PromptType promptType : values()) {
            if (promptType.getValue() == value) return promptType;
        }
        throw new DbException("不存在该类型的提示词");
    }
}
