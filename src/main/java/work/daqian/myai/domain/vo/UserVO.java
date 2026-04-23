package work.daqian.myai.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户信息")
public class UserVO {

    @Schema(description = "用户名", example = "jasper")
    private String username;

    @Schema(description = "用户昵称", example = "张三")
    private String nickName;

    @Schema(description = "用户邮箱", example = "123@qq.com")
    private String email;

    @Schema(description = "用户头像", example = "/files/xxxx.jpg")
    private String avatar;

    @Schema(description = "用户手机号", example = "13888488848")
    private String phone;
}
