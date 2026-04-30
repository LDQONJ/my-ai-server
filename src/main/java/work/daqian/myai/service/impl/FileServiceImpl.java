package work.daqian.myai.service.impl;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import work.daqian.myai.common.R;
import work.daqian.myai.exception.BadRequestException;
import work.daqian.myai.exception.BizIllegalException;
import work.daqian.myai.service.FileService;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
        String extension = replaced.substring(replaced.lastIndexOf(".")).toLowerCase();

        // 日期路径
        String datePath = LocalDate.now().toString().replaceAll("-", "/");

        // 原始文件路径（临时）
        String tempPath = BASE_DICTIONARY + datePath + "/" +
                originalName + "_" + UUID.randomUUID().toString().replaceAll("-", "") + extension;

        File tempFile = new File(tempPath);
        if (!tempFile.getParentFile().exists()) {
            tempFile.getParentFile().mkdirs();
        }

        try {
            // 先保存原始文件
            file.transferTo(tempFile);

            // 判断是否音频
            if (isAudioFile(extension)) {

                // 👉 转码后文件（统一用 wav）
                String targetRelativePath = datePath + "/" +
                        originalName + "_" + UUID.randomUUID().toString().replaceAll("-", "") + ".wav";

                String targetFullPath = BASE_DICTIONARY + targetRelativePath;

                File targetFile = new File(targetFullPath);

                // 调用 FFmpeg 转码
                convertTo16kPcm(tempFile, targetFile);

                // 删除原始文件（避免占空间）
                tempFile.delete();

                String path = "/files/" + targetRelativePath.replaceAll("/", "-");
                return R.ok(path);

            } else {
                // 非音频直接返回
                String path = "/files/" + (datePath + "/" +
                        tempFile.getName()).replaceAll("/", "-");
                return R.ok(path);
            }

        } catch (Exception e) {
            throw new BizIllegalException("上传失败: " + e.getMessage());
        }
    }
    /* public R<String> uploadFile(MultipartFile file) {
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
    } */

    @Override
    public ResponseEntity<Resource> download(String fileName) {
        File file = getFile(fileName);
        if (!file.exists()) throw new BizIllegalException("文件不存在");
        FileSystemResource resource = new FileSystemResource(file);
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        MediaType mediaType = getMediaType(extension);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    public @NonNull File getFile(String fileName) {
        String secondPath = fileName.replaceAll("-", "/");
        String name = BASE_DICTIONARY + secondPath;
        return new File(name);
    }

    /**
     * 根据文件扩展名获取对应的 MediaType
     */
    public MediaType getMediaType(String extension) {
        // 图片类型
        return switch (extension) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "bmp" -> MediaType.parseMediaType("image/bmp");
            case "webp" -> MediaType.parseMediaType("image/webp");

            // 文档类型
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "doc" -> MediaType.parseMediaType("application/msword");
            case "docx" ->
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "xls" -> MediaType.parseMediaType("application/vnd.ms-excel");
            case "xlsx" ->
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "ppt" -> MediaType.parseMediaType("application/vnd.ms-powerpoint");
            case "pptx" ->
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.presentationml.presentation");
            case "txt" -> MediaType.TEXT_PLAIN;

            // 压缩文件
            case "zip" -> MediaType.parseMediaType("application/zip");
            case "rar" -> MediaType.parseMediaType("application/x-rar-compressed");
            case "7z" -> MediaType.parseMediaType("application/x-7z-compressed");

            // 音视频
            case "mp3" -> MediaType.parseMediaType("audio/mpeg");
            case "mp4" -> MediaType.parseMediaType("video/mp4");
            case "avi" -> MediaType.parseMediaType("video/x-msvideo");

            // 默认二进制流
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    private boolean isAudioFile(String extension) {
        return extension.matches("\\.(mp3|wav|ogg|webm|aac|m4a|flac|amr)");
    }

    private void convertTo16kPcm(File input, File output) throws IOException, InterruptedException {

        ProcessBuilder builder = new ProcessBuilder(
                "ffmpeg",
                "-y", // 覆盖输出
                "-i", input.getAbsolutePath(),
                "-ac", "1",               // 单声道
                "-ar", "16000",           // 16kHz
                "-acodec", "pcm_s16le",   // PCM 16bit
                output.getAbsolutePath()
        );

        builder.redirectErrorStream(true);
        Process process = builder.start();

        // 必须读取输出流，否则可能阻塞
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // 可以打印日志
                // System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg 转码失败");
        }
    }
}
