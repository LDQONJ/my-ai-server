package work.daqian.myai.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import work.daqian.myai.common.R;

public interface FileService {
    R<String> uploadFile(MultipartFile file);

    ResponseEntity<Resource> download(String fileName);
}
