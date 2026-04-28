package work.daqian.myai.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import work.daqian.myai.common.PageQuery;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "使用详情分页查询参数")
public class UsageDetailPageQuery extends PageQuery {
    @Schema(description = "模型名称")
    private String modelName;
}
