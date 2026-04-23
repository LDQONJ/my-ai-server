package work.daqian.myai.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import work.daqian.myai.common.R;
import work.daqian.myai.exception.BadRequestException;
import work.daqian.myai.exception.BizIllegalException;
import work.daqian.myai.service.FileService;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    private final static String BASE_DICTIONARY = "D:/my-ai-img/";

    @Override
    public R<String> uploadFile(MultipartFile file) {
        String originName = file.getOriginalFilename();
        if (originName == null) throw new BadRequestException("文件名不能为空");
        String replaced = originName.replaceAll("-", "_");
        String originalName = replaced.substring(0, replaced.lastIndexOf("."));
        String extension = replaced.substring(replaced.lastIndexOf("."));
        String secondPath = LocalDate.now().toString().replaceAll("-", "/") + "/" + // result: 2025/07/29/
                originalName +
                UUID.randomUUID().toString().replaceAll("-", "") +
                extension;
        String fileName = BASE_DICTIONARY + secondPath;
        File localFile = new File(fileName);
        if (!localFile.getParentFile().exists()) {
            localFile.getParentFile().mkdirs();
        }
        try {
            file.transferTo(localFile);
            String path = "/files/" + secondPath.replaceAll("/", "-");
            return R.ok(path);
        } catch (IOException e) {
            throw new BizIllegalException("上传失败");
        }
    }

    @Override
    public ResponseEntity<Resource> download(String fileName) {
        String secondPath = fileName.replaceAll("-", "/");
        String name = BASE_DICTIONARY + secondPath;
        java.io.File file = new java.io.File(name);
        if (!file.exists()) throw new BizIllegalException("文件不存在");
        FileSystemResource resource = new FileSystemResource(file);
        String extension = fileName.substring(fileName.lastIndexOf('.'));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/" + extension.toLowerCase()))
                .body(resource);
    }
}
