package work.daqian.myai.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "模型单次调用的 Token 消耗量详情")
public class UsageDetailVO {

    @Schema(description = "模型名称", example = "DeepSeek V4 Pro")
    private String modelName;

    @Schema(description = "对话 Id", example = "sudg8agdg123di1hsag")
    private String sessionId;

    @Schema(description = "对话标题", example = "日常问候")
    private String sessionTitle;

    @Schema(description = "输入消耗的 Token", example = "20")
    private Integer promptTokens;

    @Schema(description = "输出消耗的 Token", example = "40")
    private Integer completionTokens;

    @Schema(description = "思考消耗的 Token", example = "20")
    private Integer reasoningTokens;

    @Schema(description = "回复消耗的 Token", example = "20")
    private Integer contentTokens;

    @Schema(description = "总共消耗的 Token", example = "60")
    private Integer totalTokens;

    @Schema(description = "输入命中的缓存的 Token", example = "0")
    private Integer cachedTokens;

    @Schema(description = "消耗的金额", example = "0")
    private Integer cost;

    @Schema(description = "调用时间", example = "2026-04-26 20:21:43")
    private LocalDateTime createTime;
}
