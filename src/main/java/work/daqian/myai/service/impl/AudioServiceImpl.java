package work.daqian.myai.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import work.daqian.myai.adapter.AlibabaModelAdapter;
import work.daqian.myai.domain.po.ChatMessage;
import work.daqian.myai.exception.BadRequestException;
import work.daqian.myai.exception.BizIllegalException;
import work.daqian.myai.repository.ChatMessageRepository;
import work.daqian.myai.service.AudioService;
import work.daqian.myai.service.FileService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static work.daqian.myai.util.ChatUtil.toSSEDone;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioServiceImpl implements AudioService {

    private final AlibabaModelAdapter alibabaModelAdapter;

    private final ChatMessageRepository messageRepository;

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

    @Override
    public Flux<String> textToVoice(String id) {
        Optional<ChatMessage> messageOptional = messageRepository.findById(id);
        if (messageOptional.isEmpty()) throw new BadRequestException("消息不存在");
        ChatMessage message = messageOptional.get();
        return textToVoiceWithText(message.getContent());
    }

    public Flux<String> textToVoiceWithText(String text) {
        /**
         * {
         *   "model": "qwen3-tts-flash",
         *   "input": {
         *     "text": "哈喽，我是李达千的AI助手，有什么事情需要我帮助吗？",
         *     "voice": "Cherry",
         *     "language_type": "Chinese"
         *   }
         * }
         */
        Map<String, Object> requestBody = Map.of(
                "model", "qwen3-tts-flash",
                "input", Map.of(
                        "text", text,
                        "voice", "Serena",
                        "language_type", "Chinese"
                )
        );
        return alibabaModelAdapter.buildWebClient().post()
                .uri(alibabaModelAdapter.getMultiUri())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(chunk -> alibabaModelAdapter.parseAudioChunk(chunk, null, null))
                .filter(content -> content != null && !content.isEmpty())
                .concatWith(Flux.just(toSSEDone()));
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
