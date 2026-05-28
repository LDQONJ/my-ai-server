package work.daqian.myai.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import work.daqian.myai.security.IpRateLimiterService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class IpRoleRateLimitFilter extends OncePerRequestFilter {

    private final IpRateLimiterService rateLimiterService;
    private final Map<String, Integer> roleCapacityMap;  // 角色 -> 每小时上限
    private final int defaultCapacity;                  // 未匹配角色时的默认上限
    private final List<AntPathRequestMatcher> pathMatchers;

    public IpRoleRateLimitFilter(IpRateLimiterService rateLimiterService,
                                 Map<String, Integer> roleCapacityMap,
                                 int defaultCapacity,
                                 List<String> patterns) {
        this.rateLimiterService = rateLimiterService;
        this.roleCapacityMap = roleCapacityMap;
        this.defaultCapacity = defaultCapacity;
        this.pathMatchers = patterns.stream()
                .map(AntPathRequestMatcher::new)
                .toList();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 是否匹配限流路径
        boolean shouldLimit = pathMatchers.stream()
                .anyMatch(matcher -> matcher.matches(request));

        if (!shouldLimit) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. 获取客户端 IP
        String clientIp = getClientIP(request);

        // 2. 获取当前角色
        String role = extractRole();

        // 3. 确定该角色对应的容量
        int capacity = roleCapacityMap.getOrDefault(role, defaultCapacity);

        // 4. 构建限流键（IP + 角色）
        String rateLimitKey = clientIp + ":" + role;

        // 5. 尝试消费令牌
        if (!rateLimiterService.tryConsume(rateLimitKey, capacity)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests - rate limit exceeded for role: " + role);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从 SecurityContext 中提取角色。
     * 约定：取第一个以 "ROLE_" 开头的权限，未认证则返回 "ANONYMOUS"。
     */
    private String extractRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "ANONYMOUS";
        }
        Optional<String> role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst();
        return role.orElse("ANONYMOUS");
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}