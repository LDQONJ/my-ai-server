package work.daqian.myai.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * <p>
 * 模型使用量汇总
 * </p>
 *
 * @author 李达千
 * @since 2026-04-27
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("model_usage_sum")
public class ModelUsageSum implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 Id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户 Id
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 模型名称
     */
    @TableField("model_name")
    private String modelName;

    /**
     * 使用日期
     */
    @TableField("period_date")
    private LocalDate periodDate;

    /**
     * 输入 Token 总量
     */
    @TableField("prompt_tokens")
    private Long promptTokens = 0L;

    /**
     * 输出 Token 总量
     */
    @TableField("completion_tokens")
    private Long completionTokens = 0L;

    /**
     * 消耗 Token 总量
     */
    @TableField("total_tokens")
    private Long totalTokens = 0L;

    /**
     * 思考部分 Token 总量
     */
    @TableField("reasoning_tokens")
    private Long reasoningTokens = 0L;

    /**
     * 命中缓存的 Token 总量
     */
    @TableField("cached_tokens")
    private Long cachedTokens = 0L;

    /**
     * 费用总量
     */
    @TableField("cost")
    private Integer cost = 0;

    public static void increment(ModelUsageSum old, ModelUsageDetail usageDetail) {
        old.setPromptTokens(old.getPromptTokens() + usageDetail.getPromptTokens());
        old.setCompletionTokens(old.getCompletionTokens() + usageDetail.getCompletionTokens());
        old.setTotalTokens(old.getTotalTokens() + usageDetail.getTotalTokens());
        old.setReasoningTokens(old.getReasoningTokens() + usageDetail.getReasoningTokens());
        old.setCachedTokens(old.getCachedTokens() + usageDetail.getCachedTokens());
        old.setCost(old.getCost() + usageDetail.getCost());
    }
}
