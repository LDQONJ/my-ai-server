package work.daqian.myai.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    @Qualifier("taskExecutor")
    private Executor taskExecutor;

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 关键点：将你自定义的线程池设置为 MVC 异步处理的默认执行器
        if (taskExecutor instanceof ThreadPoolTaskExecutor) {
            configurer.setTaskExecutor((ThreadPoolTaskExecutor) taskExecutor);
        }
        // 设置异步请求的超时时间（可选，默认通常是 10-30s，AI 对话建议设长一点）
        configurer.setDefaultTimeout(60000);
    }
}
