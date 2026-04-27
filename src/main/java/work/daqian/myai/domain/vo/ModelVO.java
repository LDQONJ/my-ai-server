package work.daqian.myai.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import work.daqian.myai.enums.Provider;

@Data
@Schema(description = "模型信息")
public class ModelVO {

    @Schema(description = "模型 Id")
    private Long id;

    @Schema(description = "模型名称")
    private String name;

    @Schema(description = "模型介绍")
    private String description;

    @Schema(description = "模型提供者")
    private Provider provider;
}
