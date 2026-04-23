package work.daqian.myai.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import work.daqian.myai.domain.dto.LoginUserDTO;
import work.daqian.myai.util.JwtUtil;
import work.daqian.myai.util.UserContext;

@Slf4j
public class LoginAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public LoginAuthInterceptor(JwtUtil jwtUtil) {
        log.info("LoginAuthInterceptor 构造中，jwtUtil: " + jwtUtil);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 获取请求头中的 Token
        String token = request.getHeader("Authorization");

        // log.info("检测到已登录用户，token：{}", token);

        // 2. 校验 Token
        if (token != null && !token.isEmpty()) {
            LoginUserDTO userDTO = jwtUtil.parseToken(token);
            if (userDTO != null) {
                // 3. 保存用户信息到上下文
                UserContext.setUser(userDTO.getId());
                return true;
            }
        }

        // TODO 暂时不拦截
        return true;


        /* // 4. 校验失败，返回 401
        response.setStatus(401);
        return false; */
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 5. 请求结束后清理上下文，防止内存泄漏
        UserContext.removeUser();
    }
}
