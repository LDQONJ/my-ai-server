package work.daqian.myai.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageDTO<T> {
    private Integer total;
    private List<T> data;
    public static <T> PageDTO<T> of(Integer total, List<T> data) {
        return new PageDTO<>(total, data);
    }

    public static <T> PageDTO<T> empty() {
        return new PageDTO<>();
    }
}
