package work.daqian.myai;

import jakarta.annotation.PostConstruct;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication(exclude = RedisRepositoriesAutoConfiguration.class)
@MapperScan("work.daqian.myai.mapper")
@EnableMongoAuditing
@EnableScheduling
public class MyaiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyaiApplication.class, args);
    }

    @PostConstruct
    void setDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Singapore"));
    }
}
