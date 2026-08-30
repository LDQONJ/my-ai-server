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
        // if (cacheCode != null) return R.ok();
        String code = generateCode();
        redisTemplate.opsForValue().set(key, code);
        redisTemplate.expire(key, Duration.ofMinutes(5));
        if (target.indexOf('@') != -1) {
            String htmlContent = """
                    <!DOCTYPE html>
                    <html lang="zh-CN">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>LDQ's AI 验证码</title>
                    </head>
                    <body style="margin: 0; padding: 0; font-family: 'Helvetica Neue', Helvetica, Arial, 'Microsoft Yahei', sans-serif; background-color: transparent;" width="100%">
                        <table align="center" border="0" cellpadding="0" cellspacing="0" width="92%" style="background-color: transparent; padding: 40px 0;">
                            <tr>
                                <td align="center">
                                    <table border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 600px; background-color: #ffffff; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); overflow: hidden;">
                                        <tr>
                                            <td align="center" style="background-color: #0052cc; padding: 25px 0;">
                                                <h1 style="color: #ffffff; margin: 0; font-size: 24px; font-weight: 500; letter-spacing: 1px;">注册验证</h1>
                                            </td>
                                        </tr>
                    
                                        <tr>
                                            <td style="padding: 30px 20px 20px 20px;">
                                                <p style="color: #333333; font-size: 16px; margin-bottom: 20px;">尊敬的用户，您好：</p>
                                                <p style="color: #555555; font-size: 15px; line-height: 1.6; margin-bottom: 30px;">
                                                    您正在进行 <a href="https://ai.lidaqian.com" style="text-decoration: none;">LDQ's AI</a> 账号注册。您的邮箱注册验证码为：
                                                </p>
                    
                                                <div style="text-align: center; margin: 30px 0;">
                                                    <span style="display: inline-block; background-color: #f0f5ff; border: 1px solid #cce0ff; color: #0052cc; font-size: 32px; font-weight: bold; letter-spacing: 8px; padding: 15px 40px; border-radius: 6px;">
                                                        {{VERIFY_CODE}}
                                                    </span>
                                                </div>
                    
                                                <p style="color: #777777; font-size: 14px; line-height: 1.6;">
                                                    该验证码将在 <strong>5 分钟</strong> 后失效。<span style="color: #d93025;">请勿将此验证码泄露给任何人</span>。
                                                </p>
                                                <p style="color: #777777; font-size: 14px; line-height: 1.6; margin-top: 10px;">
                                                    如果您并未进行此项操作，请忽略此邮件。
                                                </p>
                                            </td>
                                        </tr>
                    
                                        <!-- 底部版权说明 -->
                                        <tr>
                                            <td style="background-color: #fafafa; padding: 20px 40px; border-top: 1px solid #eeeeee; text-align: center;">
                                                <p style="color: #aaaaaa; font-size: 12px; line-height: 1.5; margin: 0;">
                                                    此邮件由系统自动发送，请勿直接回复。<br>
                                                    &copy; 2026 Lidaqian.com All Rights Reserved.
                                                </p>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                        </table>
                    </body>
                    </html>
                    """.replace("{{VERIFY_CODE}}", code);
            String textContent = """
                    LDQ's AI 注册验证
                    
                    尊敬的用户，您好：
                    
                    您正在进行 LDQ's AI 账号注册。您的邮箱注册验证码为：705856
                    
                    该验证码将在 5 分钟 后失效。请勿将此验证码泄露给任何人。
                    
                    如果您并未进行此项操作，请忽略此邮件。
                    
                    此邮件由系统自动发送，请勿直接回复。
                    
                    © 2026 Lidaqian.com All Rights Reserved.
                    """;
            mailUtil.sendSimpleMail(target, "LDQ's AI 验证码", htmlContent, textContent);
        }
        log.debug("{} 的验证码为：{}", target, code);
        return R.ok();
    }


    private String generateCode() {
        Random random = new Random();
        int i = random.nextInt(899999) + 100000;
        return i + "";
    }
}
