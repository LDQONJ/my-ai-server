package work.daqian.myai.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IpRateLimiterService {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    /**
     * 为指定的 key 获取或创建桶，容量由 capacity 决定
     */
    public Bucket resolveBucket(String key, int capacity) {
        // key 中包含 capacity 信息，避免不同容量共用同一 key 的桶
        String compositeKey = key + ":" + capacity;
        return cache.computeIfAbsent(compositeKey, k -> createNewBucket(capacity));
    }

    private Bucket createNewBucket(int capacity) {
        Bandwidth limit = Bandwidth.classic(capacity,
                Refill.intervally(capacity, Duration.ofHours(1)));
        return Bucket4j.builder().addLimit(limit).build();
    }

    /**
     * 尝试消费一个令牌
     */
    public boolean tryConsume(String key, int capacity) {
        Bucket bucket = resolveBucket(key, capacity);
        return bucket.tryConsume(1);
    }
}