package work.daqian.myai.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录用户信息")
public class LoginUserDTO {
    @Schema(description = "用户ID", example = "1")
    private Long id;
}
