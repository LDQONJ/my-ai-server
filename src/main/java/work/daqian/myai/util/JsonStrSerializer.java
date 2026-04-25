package work.daqian.myai.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class JsonStrSerializer extends JsonSerializer<Object> {

    @Override
    public void serialize(Object obj, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (obj instanceof String) {
            String trimmed = ((String) obj).trim();
            if ((trimmed.startsWith("{") && trimmed.endsWith("}"))
            || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
                jsonGenerator.writeRawValue(trimmed);
                return;
            }
        }
        jsonGenerator.writeObject(obj);
    }
}
