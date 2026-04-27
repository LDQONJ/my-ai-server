package work.daqian.myai.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 记录用户对每个模型的单次调用使用量
 * </p>
 *
 * @author 李达千
 * @since 2026-04-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("model_usage_detail")
public class ModelUsageDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 Id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 模型名称
     */
    @TableField("model_name")
    private String modelName;

    /**
     * 用户 Id
     */
    @TableField("user_Id")
    private Long userId;

    /**
     * 会话 Id
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 输入的token数
     */
    @TableField("prompt_tokens")
    private Integer promptTokens;

    /**
     * 输出的 token 数
     */
    @TableField("completion_tokens")
    private Integer completionTokens;

    /**
     * 总的 token 使用量
     */
    @TableField("total_tokens")
    private Integer totalTokens;

    /**
     * 思考部分的 token 数
     */
    @TableField("reasoning_tokens")
    private Integer reasoningTokens;

    /**
     * 命中缓存的 token 数
     */
    @TableField("cached_tokens")
    private Integer cachedTokens;

    /**
     * 费用
     */
    @TableField("cost")
    private Integer cost;

    /**
     * 调用时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;


}
