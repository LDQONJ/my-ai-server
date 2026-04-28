package work.daqian.myai.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import work.daqian.myai.common.PageDTO;
import work.daqian.myai.common.R;
import work.daqian.myai.domain.dto.LoginUserDTO;
import work.daqian.myai.domain.dto.UserDTO;
import work.daqian.myai.domain.dto.UserPageQuery;
import work.daqian.myai.domain.po.User;
import work.daqian.myai.domain.vo.UserVO;
import work.daqian.myai.exception.BadRequestException;
import work.daqian.myai.mapper.UserMapper;
import work.daqian.myai.service.IUserService;
import work.daqian.myai.util.*;

import java.time.Duration;
import java.util.List;

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
        loginUserDTO.setRoles(List.of("ROLE_USER"));
        loginUserDTO.setUsername(username);
        String token = generateToken(loginUserDTO);
        return R.ok(token);
    }

    @Override
    public R<String> me() {
        Long userId = SecurityUtils.getCurrentUserId();
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
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new BadRequestException("用户未登录");
        User user = BeanUtils.copyBean(updateForm, User.class);
        user.setId(userId);
        updateById(user);
        redisTemplate.delete(USER_KEY_PREFIX + userId);
        return R.ok();
    }

    @Override
    public R<PageDTO<UserVO>> queryUserPage(UserPageQuery pageQuery) {
        Page<User> userPage = lambdaQuery()
                .page(com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO.of(pageQuery.getPageNo(), pageQuery.getPageSize()));
        List<User> users = userPage.getRecords();
        if (CollUtils.isEmpty(users)) return R.ok(PageDTO.empty());
        List<UserVO> vos = BeanUtils.copyList(users, UserVO.class);
        return R.ok(PageDTO.of(userPage.getTotal(), vos));
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
