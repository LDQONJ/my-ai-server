package work.daqian.myai.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "提示词")
public class PromptDTO {
    @Schema(description = "AI 角色设定")
    private String persona;
    @Schema(description = "AI 回复规则")
    private String rules;
    @Schema(description = "对话示例",
            example = """
                    [
                        {
                            "userMsg": "xxx",
                            "aiMsg": "xxx"
                        },
                    ]
                    """)
    private List<Turn> example;

    @JsonIgnore
    public String getExampleStr() {
        String json = null;
        try {
            json = mapper.writeValueAsString(example);
        } catch (JsonProcessingException e) {
            return "";
        }
        return json;
    }

    public void setExampleFromStr(String json) {
        Turn[] turns = null;
        try {
            turns = mapper.readValue(json, Turn[].class);
        } catch (JsonProcessingException e) {
        }
        if (turns != null)
            example = List.of(turns);
    }

    @JsonIgnore
    private transient ObjectMapper mapper = new ObjectMapper();

    @Data
    static class Turn {
        private String userMsg;
        private String aiMsg;
    }
}
