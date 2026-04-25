package work.daqian.myai.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "登录用户信息")
public class LoginUserDTO {
    @Schema(description = "用户ID", example = "1")
    private Long id;
    @Schema(description = "用户名", example = "Jack")
    private String username;

    private List<String> roles;
}
