package work.daqian.myai.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户登录表单")
public class UserDTO {

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "用户昵称", example = "张三")
    private String nickName;

    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "邮箱", example = "test@test.com")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "验证码", example = "1234")
    private String code;

    @Schema(description = "头像", example = "/files/xxx.jpg")
    private String avatar;

}
