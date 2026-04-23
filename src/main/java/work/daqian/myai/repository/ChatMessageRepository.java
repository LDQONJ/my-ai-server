package work.daqian.myai.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import work.daqian.myai.domain.po.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;


public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    List<ChatMessage> findAllBySessionIdAndRoleNotContainingOrderByCreateTimeAsc(String sessionId, String excludedRole);

    List<ChatMessage> findBySessionIdOrderByCreateTimeDesc(String sessionId, PageRequest pageRequest);

    void deleteChatMessagesByUserIdNullAndCreateTimeBefore(LocalDateTime time);

    void deleteChatMessagesBySessionId(String sessionId);
}
