package work.daqian.myai.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    String key() default "";  // 限流的key，支持SpEL

    int limit() default 10;  // 令牌容量

    int period() default 60;  // 时间窗口（秒）

    String message() default "操作过于频繁，请稍后再试";

    String roles() default "ROLE_USER";

    // 基于什么限流
    LimitType limitType() default LimitType.IP;

    enum LimitType {
        IP,     // 基于IP
        USER,   // 基于用户
        METHOD  // 基于方法（全局）
    }
}
