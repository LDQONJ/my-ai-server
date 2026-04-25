package work.daqian.myai.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import work.daqian.myai.common.R;
import work.daqian.myai.domain.dto.UserDTO;
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
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                                "code": 200,
                                "msg": "OK",
                                "data": {
                                    "username": "test",
                                    "nickName": null,
                                    "email": "test@daqian.work",
                                    "avatar": "/files/2026-04-19-_7f5a5bbde3b7284c3cd115a0234744ea87e752a0aa7e544a.jpg",
                                    "phone": "18888488848"
                                },
                                "requestId": null
                            }
                            """)
            ))
    public R<String> me() {
        return userService.me();
    }

    @PutMapping
    @Operation(summary = "更新用户信息")
    public R<Void> update(@RequestBody UserDTO updateForm) {
        return userService.updateUserInfo(updateForm);
    }
}
