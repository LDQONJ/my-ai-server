package work.daqian.myai.config;

import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.lang.Nullable;
import work.daqian.myai.enums.PromptType;

import java.util.Arrays;

@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(Arrays.asList(
                new PromptTypeReadConverter(),
                new PromptTypeWriteConverter()
        ));
    }

    @ReadingConverter
    static class PromptTypeReadConverter implements Converter<String, PromptType> {
        @Nullable
        @Override
        public PromptType convert(@NonNull String name) {
            return PromptType.fromName(name);
        }
    }

    @WritingConverter
    static class PromptTypeWriteConverter implements Converter<PromptType, String> {
        @Nullable
        @Override
        public String convert(@NonNull PromptType promptType) {
            return promptType.getName();
        }
    }
}
