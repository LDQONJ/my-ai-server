package work.daqian.myai.util;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import work.daqian.myai.constant.JwtConstants;
import work.daqian.myai.domain.dto.LoginUserDTO;
import work.daqian.myai.exception.BadRequestException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import static work.daqian.myai.constant.JwtConstants.PAYLOAD_USER_KEY;

@Slf4j
@Component
public class JwtUtil {

    private final StringRedisTemplate stringRedisTemplate;
    private final byte[] keyBytes;

    public JwtUtil(StringRedisTemplate stringRedisTemplate, @Value("${ldq.jwt.secret}") String secret) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    }

    private JWTSigner getSigner() {
        return JWTSignerUtil.hs256(keyBytes);
    }

    /**
     * 创建 access-token
     *
     * @param userDTO 用户信息
     * @return access-token
     */
    public String createToken(LoginUserDTO userDTO) {
        // 1.生成jws
        return JWT.create()
                .setPayload(PAYLOAD_USER_KEY, userDTO)
                .setExpiresAt(new Date(System.currentTimeMillis() + Duration.ofDays(7).toMillis()))
                .setSigner(getSigner())
                .sign();
    }

    /**
     * 创建刷新token，并将token的JTI记录到Redis中
     *
     * @param userDetail 用户信息
     * @return 刷新token
     */
    public String createRefreshToken(LoginUserDTO userDetail) {
        // 1.生成 JTI
        String jti = UUID.randomUUID().toString(true);
        // 2.生成jwt
        // 2.1.如果是记住我，则有效期7天，否则30分钟
        Duration ttl = Duration.ofDays(7);
        // 2.2.生成token
        String token = JWT.create()
                .setJWTId(jti)
                .setPayload(PAYLOAD_USER_KEY, userDetail)
                .setExpiresAt(new Date(System.currentTimeMillis() + ttl.toMillis()))
                .setSigner(getSigner())
                .sign();
        // 3.缓存jti，有效期与token一致，过期或删除JTI后，对应的refresh-token失效
        stringRedisTemplate.opsForValue()
                .set(JwtConstants.JWT_REDIS_KEY_PREFIX + userDetail.getId(), jti, ttl);
        return token;
    }

    /**
     * 解析刷新token
     *
     * @param refreshToken 刷新token
     * @return 解析刷新token得到的用户信息
     */
    public LoginUserDTO parseRefreshToken(String refreshToken) {
        // 1.校验token是否为空
        if (refreshToken == null || refreshToken.isEmpty()) return null;
        JWT jwt;
        try {
            jwt = JWT.of(refreshToken).setSigner(getSigner());
        } catch (Exception e) {
            throw new BadRequestException(400, "token 错误", e);
        }
        // 2.校验jwt是否有效
        if (!jwt.verify()) {
            // 验证失败
            throw new BadRequestException(400, "token 错误");
        }
        // 3.校验是否过期
        try {
            JWTValidator.of(jwt).validateDate();
        } catch (ValidateException e) {
            throw new BadRequestException(400, "token 过期");
        }
        // 4.数据格式校验
        Object userPayload = jwt.getPayload(PAYLOAD_USER_KEY);
        Object jtiPayload = jwt.getPayload(JwtConstants.PAYLOAD_JTI_KEY);
        if (jtiPayload == null || userPayload == null) {
            // 数据为空
            throw new BadRequestException(400, "token 错误");
        }

        // 5.数据解析
        LoginUserDTO userDTO;
        try {
            userDTO = ((JSONObject) userPayload).toBean(LoginUserDTO.class);
        } catch (RuntimeException e) {
            // 数据格式有误
            throw new BadRequestException(400, "token 错误");
        }

        // 6.JTI校验
        String jti = stringRedisTemplate.opsForValue().get(JwtConstants.JWT_REDIS_KEY_PREFIX + userDTO.getId());
        if (!StringUtils.equals(jti, jtiPayload.toString())) {
            // jti不一致
            throw new BadRequestException(400, "token 错误");
        }
        return userDTO;
    }

    /**
     * 解析access-token
     *
     * @param token access-token
     * @return 解析access-token得到的用户信息
     */
    public LoginUserDTO parseToken(String token) {
        // 1.校验token是否为空
        if (token == null || token.isEmpty()) return null;

        // 2.去除Bearer前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 3.校验并解析jwt
        JWT jwt;
        try {
            jwt = JWT.of(token).setSigner(getSigner());
        } catch (Exception e) {
            log.info("创建 jwt 对象失败，token：{}", token);
            throw new BadRequestException(400, "token 错误");
        }
        // 4.校验jwt是否有效
        try {
            if (!jwt.verify()) {
                // 验证失败
                log.info("jwt 解析失败，token：{}", token);
                throw new BadRequestException(400, "token 错误");
            }
        } catch (Exception e) {
            log.info("jwt 验证失败，token：{}", token, e);
            throw new BadRequestException(400, "token 错误");
        }
        // 5.校验是否过期
        try {
            JWTValidator.of(jwt).validateDate();
        } catch (ValidateException e) {
            throw new BadRequestException(400, "token 错误");
        }
        // 6.数据格式校验
        Object userPayload = jwt.getPayload(JwtConstants.PAYLOAD_USER_KEY);
        if (userPayload == null) {
            // 数据为空
            throw new BadRequestException(400, "token 错误");
        }
        // 7.数据解析
        LoginUserDTO userDTO;
        try {
            userDTO = ((JSONObject) userPayload).toBean(LoginUserDTO.class);
        } catch (RuntimeException e) {
            // 数据格式有误
            throw new BadRequestException(400, "token 错误");
        }

        return userDTO;
    }



    /**
     * 清理刷新refresh-token的jti，本质是refresh-token作废
     */
    public void cleanJtiCache() {
        stringRedisTemplate.delete(JwtConstants.JWT_REDIS_KEY_PREFIX + UserContext.getUser());
    }
}