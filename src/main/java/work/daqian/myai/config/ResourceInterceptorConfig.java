package work.daqian.myai.config;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import work.daqian.myai.config.properties.ResourceAuthProperties;
import work.daqian.myai.interceptor.LoginAuthInterceptor;
import work.daqian.myai.util.JwtUtil;

@Slf4j
@Configuration
@EnableConfigurationProperties(ResourceAuthProperties.class)
public class ResourceInterceptorConfig implements WebMvcConfigurer {

    private final ResourceAuthProperties authProperties;

    private final JwtUtil jwtUtil;

    @Autowired
    public ResourceInterceptorConfig(ResourceAuthProperties resourceAuthProperties, JwtUtil jwtUtil) {
        this.authProperties = resourceAuthProperties;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1.添加用户信息拦截器
        // registry.addInterceptor(new UserInfoInterceptor()).order(0);
        // 2.是否需要做登录拦截
        if(!authProperties.getEnable()){
            // 无需登录拦截
            log.info("无需拦截");
            return;
        }
        log.info("开始注册拦截器");
        // 2.添加登录拦截器
        InterceptorRegistration registration = registry.addInterceptor(new LoginAuthInterceptor(jwtUtil)).order(1);
        // 2.1.添加拦截器路径
        if(CollUtil.isNotEmpty(authProperties.getIncludeLoginPaths())){
            registration.addPathPatterns(authProperties.getIncludeLoginPaths());
        }
        // 2.2.添加排除路径
        if(CollUtil.isNotEmpty(authProperties.getExcludeLoginPaths())){
            registration.excludePathPatterns(authProperties.getExcludeLoginPaths());
        }
        // 2.3.排除swagger路径
        registration.excludePathPatterns(
                "/v2/**",
                "/v3/**",
                "/swagger-resources/**",
                "/webjars/**",
                "/doc.html"
        );
    }
}
