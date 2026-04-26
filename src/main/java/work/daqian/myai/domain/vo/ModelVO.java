package work.daqian.myai.domain.vo;

import lombok.Data;
import work.daqian.myai.enums.Provider;

@Data
public class ModelVO {
    private Long id;

    private String name;

    private String description;

    private Provider provider;
}
