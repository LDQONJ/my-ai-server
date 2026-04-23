package work.daqian.myai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import work.daqian.myai.config.properties.SwaggerProperties;

@Configuration
@ConditionalOnProperty(prefix = "ldq.swagger", name = "enable", havingValue = "true")
@EnableConfigurationProperties(SwaggerProperties.class)
public class Knife4jConfig {

    @Resource
    private SwaggerProperties swaggerProperties;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(this.swaggerProperties.getTitle())
                        .description(this.swaggerProperties.getDescription())
                        .version(this.swaggerProperties.getVersion())
                        .contact(new Contact()
                                .name(this.swaggerProperties.getContactName())
                                .url(this.swaggerProperties.getContactUrl())
                                .email(this.swaggerProperties.getContactEmail())));
    }
}
