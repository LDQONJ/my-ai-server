package work.daqian.myai.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import work.daqian.myai.common.R;
import work.daqian.myai.domain.dto.UserDTO;
import work.daqian.myai.domain.vo.UserVO;
import work.daqian.myai.service.IUserService;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 李达千
 * @since 2026-04-17
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "用户管理接口")
public class UserController {

    private final IUserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public R<String> register(@RequestBody UserDTO registerForm) {
        return userService.register(registerForm);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public R<String> login(@RequestBody UserDTO loginForm) {
        return userService.login(loginForm);
    }

    @GetMapping("/me")
    @Operation(summary = "查询用户信息")
    public R<UserVO> me() {
        return userService.me();
    }

    @PutMapping
    @Operation(summary = "更新用户信息")
    public R<Void> update(@RequestBody UserDTO updateForm) {
        return userService.updateUserInfo(updateForm);
    }
}
