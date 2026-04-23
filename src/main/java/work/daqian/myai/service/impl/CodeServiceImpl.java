package work.daqian.myai.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import work.daqian.myai.common.R;
import work.daqian.myai.service.CodeService;
import work.daqian.myai.util.MailUtil;

import java.time.Duration;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeServiceImpl implements CodeService {

    private final StringRedisTemplate redisTemplate;

    private final MailUtil mailUtil;

    @Override
    public R<Void> sendCode(String target) {
        String key = "code:target:" + target;
        String cacheCode = redisTemplate.opsForValue().get(key);
        if (cacheCode != null) return R.ok();
        String code = generateCode();
        redisTemplate.opsForValue().set(key, code);
        redisTemplate.expire(key, Duration.ofMinutes(5));
        if (target.indexOf('@') != -1)
            mailUtil.sendSimpleMail(target, "验证码", "您的验证码为：" + code);
        log.debug("{} 的验证码为：{}", target, code);
        return R.ok();
    }



    private String generateCode() {
        Random random = new Random();
        int i = random.nextInt(899999) + 100000;
        return i + "";
    }
}
