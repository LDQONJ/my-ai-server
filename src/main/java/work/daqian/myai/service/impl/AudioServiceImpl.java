package work.daqian.myai.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import work.daqian.myai.adapter.AlibabaModelAdapter;
import work.daqian.myai.exception.BizIllegalException;
import work.daqian.myai.service.AudioService;
import work.daqian.myai.service.FileService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static work.daqian.myai.util.ChatUtil.toSSEDone;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioServiceImpl implements AudioService {

    private final AlibabaModelAdapter alibabaModelAdapter;

    private final FileService fileService;

    @Override
    public Flux<String> voiceToText(String fileName) {
        if (fileName.startsWith("/files/"))
            fileName = fileName.substring(6);
        File file = fileService.getFile(fileName);
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        MediaType mediaType = fileService.getMediaType(extension);
        Mono<String> base64DataUri = audioFileToBase64DataUri(file, mediaType.toString());
        Map<String, Object> request = Map.of(
                "model", "qwen3-asr-flash",
                "messages", List.of(Map.of(
                                "role", "user",
                                "content", List.of(Map.of(
                                                "type", "input_audio",
                                                "input_audio", Map.of("data", Objects.requireNonNull(base64DataUri.block()))
                                        )))),
                "stream", true
        );

        Flux<String> textStream = null;
        try {
            textStream = alibabaModelAdapter.buildWebClient().post()
                    .uri(alibabaModelAdapter.getUri(null))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .flatMap(chunk -> alibabaModelAdapter.parseChunk(chunk, null, null, null, null))
                    .filter(content -> content != null && !content.isEmpty())
                    .concatWith(Flux.just(toSSEDone()));
        } catch (Exception e) {
            throw new BizIllegalException("语音转文本失败");
        }
        return textStream;
    }


    /**
     * 将音频文件转换为 Base64 编码的 Data URI
     */
    public Mono<String> audioFileToBase64DataUri(File file, String mimeType) {
        return Mono.fromCallable(() -> {
            if (!file.exists()) {
                throw new IOException("音频文件不存在: " + file.getName());
            }

            // 读取文件并转为 Base64
            FileInputStream fis = new FileInputStream(file);
            byte[] fileBytes = fis.readAllBytes();
            fis.close();

            // Base64 编码
            String base64Str = Base64.getEncoder().encodeToString(fileBytes);

            // 构建 Data URI
            return String.format("data:%s;base64,%s", mimeType, base64Str);
        });
    }

    /**
     * 将字节数组转换为 Base64 Data URI
     */
    public Mono<String> bytesToBase64DataUri(byte[] audioBytes, String mimeType) {
        return Mono.fromCallable(() -> {
            String base64Str = Base64.getEncoder().encodeToString(audioBytes);
            return String.format("data:%s;base64,%s", mimeType, base64Str);
        });
    }
}
