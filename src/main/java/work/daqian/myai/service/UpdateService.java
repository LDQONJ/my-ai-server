package work.daqian.myai.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import work.daqian.myai.common.R;

import java.util.Map;

public interface UpdateService {
    R<String> checkUpdate(Map<String, String> versionMap);

    ResponseEntity<Resource> download();
}
