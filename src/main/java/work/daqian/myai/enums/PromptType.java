package work.daqian.myai.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import work.daqian.myai.exception.DbException;

@Getter
@AllArgsConstructor
public enum PromptType {
    GLOBAL_PERSONA("global_persona", "context:persona:uid:"),
    GLOBAL_RULES("global_rules", "context:rules:uid"),
    PERSONA("persona", "context:persona:sid:"),
    RULES("rules", "context:rules:sid:"),
    SUMMARY("summary", "context:summary:sid:"),
    HISTORY("history", "context:history:sid:"),
    GLOBAL_EXAMPLE("global_example", "context:example:uid:"),
    EXAMPLE("example", "context:example:sid:"),
    ;
    private final String name;
    private final String keyPrefix;

    public static PromptType fromName(String name) {
        for (PromptType promptType : values()) {
            if (promptType.getName().equals(name)) return promptType;
        }
        throw new DbException("不存在该类型的提示词");
    }
}
