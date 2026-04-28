package work.daqian.myai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ToolCallParser {

    private final ObjectMapper mapper;

    public ToolCall parse(String text) {
        try {
            return mapper.readValue(text, ToolCall.class);
        } catch (Exception e) {
            return null;
        }
    }
}
