package work.daqian.myai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import work.daqian.myai.common.R;
import work.daqian.myai.service.FileService;

@RestController
@RequestMapping("/files")
@Tag(name = "文件管理相关接口")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "上传文件", description = "上传文件接口，响应 data 中为文件路径")
    @PostMapping
    public R<String> uploadFile(@Parameter(description = "文件数据", required = true) @RequestParam("file") MultipartFile file){
        return fileService.uploadFile(file);
    }

    @Operation(summary = "下载文件", description = "浏览器会自动下载头像")
    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable("fileName") String fileName) {
        return fileService.download(fileName);
    }
}

