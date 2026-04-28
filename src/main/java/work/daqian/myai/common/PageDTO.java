package work.daqian.myai.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageDTO<T> {
    private Long total;
    private List<T> data;
    public static <T> PageDTO<T> of(Long total, List<T> data) {
        return new PageDTO<>(total, data);
    }

    public static <T> PageDTO<T> empty() {
        return new PageDTO<>();
    }
}
