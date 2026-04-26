package work.daqian.myai.advice;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import work.daqian.myai.annotation.RateLimit;

import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class RateLimitAspect {
    private final ConcurrentHashMap<String, SimpleRateLimiter> limiters = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        SecurityContextHolder.getContext().getAuthentication();
        String key = generateKey(point, rateLimit);

        SimpleRateLimiter limiter = limiters.computeIfAbsent(key, k ->
                new SimpleRateLimiter(rateLimit.limit(), rateLimit.period()));

        if (!limiter.tryAcquire()) {
            throw new AccessDeniedException(rateLimit.message());
        }

        return point.proceed();
    }

    private RateLimit findMatchedAnno(RateLimit[] rateLimits, String roles) {
        for (RateLimit rateLimit : rateLimits) {
            if (rateLimit.roles().equals(roles))
                return rateLimit;
        }
        throw new AccessDeniedException("角色不存在");
    }

    private String generateKey(ProceedingJoinPoint point, RateLimit rateLimit) {
        switch (rateLimit.limitType()) {
            case USER:
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String username = auth != null ? auth.getName() : "anonymous";
                return "user:" + username + ":" + point.getSignature().getName();

            case IP:
                HttpServletRequest request = ((ServletRequestAttributes)
                        RequestContextHolder.currentRequestAttributes()).getRequest();
                return "ip:" + getClientIP(request) + ":" + point.getSignature().getName();

            case METHOD:
            default:
                return "method:" + point.getSignature().toLongString();
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    // 简单的令牌桶实现
    private static class SimpleRateLimiter {
        private final double rate;      // 每秒产生的令牌数
        private final int capacity;     // 桶容量
        private double tokens;          // 当前令牌数
        private long lastRefillTime;    // 上次补充时间

        public SimpleRateLimiter(int limit, int period) {
            this.capacity = limit;
            this.rate = (double) limit / period;
            this.tokens = limit;
            this.lastRefillTime = System.currentTimeMillis();
        }

        public synchronized boolean tryAcquire() {
            refill();
            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            double elapsed = (now - lastRefillTime) / 1000.0;
            tokens = Math.min(capacity, tokens + elapsed * rate);
            lastRefillTime = now;
        }
    }
}
