package work.daqian.myai.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import work.daqian.myai.domain.po.SystemPrompt;

public interface SystemPromptRepository extends MongoRepository<SystemPrompt, String> {

    SystemPrompt findSystemPromptBySessionIdAndType(String sessionId, String type);

    SystemPrompt findSystemPromptByUserIdAndType(Long userId, String type);

    void deleteAllBySessionId(String sessionId);
}
