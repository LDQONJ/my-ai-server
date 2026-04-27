package work.daqian.myai.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "使用详情分页查询参数")
public class UsageDetailPageQuery {
    @Schema(description = "页码")
    private int pageNo = 1;
    @Schema(description = "每页大小")
    private int pageSize = 10;
    @Schema(description = "模型名称")
    private String modelName;
}
