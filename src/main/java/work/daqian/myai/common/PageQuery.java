package work.daqian.myai.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PageQuery {
    @Schema(description = "页码")
    private int pageNo = 1;
    @Schema(description = "每页大小")
    private int pageSize = 10;
}
