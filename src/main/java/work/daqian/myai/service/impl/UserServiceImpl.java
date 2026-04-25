package work.daqian.myai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import work.daqian.myai.common.R;
import work.daqian.myai.domain.dto.LoginUserDTO;
import work.daqian.myai.domain.dto.UserDTO;
import work.daqian.myai.domain.po.User;
import work.daqian.myai.domain.vo.UserVO;
import work.daqian.myai.exception.BadRequestException;
import work.daqian.myai.mapper.UserMapper;
import work.daqian.myai.service.IUserService;
import work.daqian.myai.util.*;

import java.time.Duration;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 李达千
 * @since 2026-04-17
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final StringRedisTemplate redisTemplate;

    private final JwtUtil jwtUtil;

    private final PasswordEncoder passwordEncoder;

    private final RedisUtil redisUtil;

    private final ObjectMapper mapper;

    private static final String USER_KEY_PREFIX = "user:uid:";

    @Override
    @Transactional
    public R<String> register(UserDTO registerForm) {
        String email = registerForm.getEmail();
        String code = registerForm.getCode();
        String key = "code:target:" + email;
        String cacheCode = redisTemplate.opsForValue().get(key);
        if (cacheCode == null || !cacheCode.equals(code))
            throw new BadRequestException("验证码错误");
        String username = registerForm.getUsername();
        Long count = lambdaQuery().eq(User::getUsername, username).count();
        if (count > 0)
            throw new BadRequestException("用户名已存在");
        User user = BeanUtils.copyBean(registerForm, User.class);
        String password = passwordEncoder.encode(registerForm.getPassword());
        user.setPassword(password);
        save(user);
        LoginUserDTO loginUserDTO = new LoginUserDTO();
        loginUserDTO.setId(user.getId());

        String token = generateToken(loginUserDTO);

        return R.ok(token);
    }

    @Override
    public R<String> login(UserDTO loginForm) {
        String username = loginForm.getUsername();
        String password = loginForm.getPassword();
        // 先根据用户名查询用户
        User user = lambdaQuery().eq(User::getUsername, username).one();
        if (user == null)
            throw new BadRequestException("用户名或密码错误");

        // 使用 matches 方法验证密码
        if (!passwordEncoder.matches(password, user.getPassword()))
            throw new BadRequestException("用户名或密码错误");
        LoginUserDTO loginUserDTO = new LoginUserDTO();
        loginUserDTO.setId(user.getId());
        String token = generateToken(loginUserDTO);

        return R.ok(token);
    }

    @Override
    public R<String> me() {
        Long userId = UserContext.getUser();
        String voJson = redisUtil.cacheEmptyIfNE(USER_KEY_PREFIX, userId, Duration.ofHours(1), (uid) -> {
            User me = getById(userId);
            UserVO userVO = BeanUtils.copyBean(me, UserVO.class);
            String json;
            try {
                json = mapper.writeValueAsString(userVO);
            } catch (JsonProcessingException e) {
                return "";
            }
            return json;
        });

        return R.ok(voJson);
    }

    @Override
    public R<Void> updateUserInfo(UserDTO updateForm) {
        Long userId = UserContext.getUser();
        if (userId == null) throw new BadRequestException("用户未登录");
        User user = BeanUtils.copyBean(updateForm, User.class);
        user.setId(userId);
        updateById(user);
        return R.ok();
    }

    public String generateToken(LoginUserDTO detail) {
        // 2.2.生成access-token
        String token = jwtUtil.createToken(detail);
        // 2.3.生成refresh-token，将refresh-token的JTI 保存到Redis
        String refreshToken = jwtUtil.createRefreshToken(detail);
        // 2.4.将refresh-token写入用户cookie，并设置HttpOnly为true
        int maxAge = (int) Duration.ofDays(7).toMinutes();
        WebUtils.cookieBuilder()
                .name("refresh")
                .value(refreshToken)
                .maxAge(maxAge)
                .httpOnly(true)
                .build();
        return token;
    }
}
