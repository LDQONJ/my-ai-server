package work.daqian.myai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("work.daqian.myai.mapper")
@EnableMongoAuditing
@EnableScheduling
public class MyaiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyaiApplication.class, args);
    }

}
