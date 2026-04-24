package work.daqian.myai.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final StringRedisTemplate redisTemplate;

    /**
     * 先查 Redis 再查数据库，数据库没有数据则在 Redis 中缓存空字符串，防止缓存穿透
     * @param keyPrefix Redis Key 前缀
     * @param id 业务 Id
     * @param expire Redis 过期时间
     * @param getById 数据库查询逻辑（钩子方法），必须返回 String 类型（在 Redis 中存储的值）
     * @return 返回查询到的值，如果没有查询到则返回空字符串
     * @param <T> 业务 Id 的类型
     * @author LDQONJ
     */
    public <T> String cacheEmptyIfNE(
            String keyPrefix,
            T id,
            Duration expire,
            Function<T, String> getById) {
        String key = keyPrefix + id;
        String cache = redisTemplate.opsForValue().get(key);
        if (cache != null) return cache; // empty or data
        String data = getById.apply(id);
        redisTemplate.opsForValue().set(key, Objects.requireNonNullElse(data, ""), expire);
        return data; // data or empty
    }
}
