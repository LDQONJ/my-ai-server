package work.daqian.myai.service;

import reactor.core.publisher.Flux;
import work.daqian.myai.domain.dto.ChatFormDTO;

public interface ChatService {

    Flux<String> streamChat(ChatFormDTO chatForm);

}
