package work.daqian.myai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import work.daqian.myai.common.R;
import work.daqian.myai.common.SecurityUserDetails;
import work.daqian.myai.domain.dto.LoginUserDTO;
import work.daqian.myai.domain.dto.UserDTO;
import work.daqian.myai.util.JwtUtil;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "用户认证接口")
public class AuthController {

    private final AuthenticationManager authenticationManager;

    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    @Operation(summary = "登录", description = "使用用户名密码进行登录")
    public R<String> login(@RequestBody UserDTO userDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userDTO.getUsername(),
                        userDTO.getPassword()
                )
        );
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        Object principal = authentication.getPrincipal();
        Long userId = null;
        if (principal instanceof SecurityUserDetails) {
            userId = ((SecurityUserDetails) principal).getId();
        }
        LoginUserDTO loginUserDTO = new LoginUserDTO();
        loginUserDTO.setId(userId);
        loginUserDTO.setUsername(userDTO.getUsername());
        loginUserDTO.setRoles(roles);
        String token = jwtUtil.createToken(loginUserDTO);
        return R.ok(token);
    }
}
