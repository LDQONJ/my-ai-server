package work.daqian.myai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import work.daqian.myai.service.AudioService;

@Slf4j
@RestController
@RequestMapping("/audio")
@RequiredArgsConstructor
@Tag(name = "语音相关接口")
public class AudioController {

    private final AudioService audioService;

    @PostMapping(value = "/streamASR", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式语音转文字")
    public Flux<String> voiceToText(@RequestBody String fileName) {
        return audioService.voiceToText(fileName);
    }

    @PostMapping(value = "/streamTTS", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式文字转语音")
    public Flux<String> textToVoice(@RequestBody String id) {
        return audioService.textToVoice(id);
    }
}
