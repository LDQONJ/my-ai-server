package work.daqian.myai.service;

import reactor.core.publisher.Flux;

public interface AudioService {
    Flux<String> voiceToText(String fileName);
}
