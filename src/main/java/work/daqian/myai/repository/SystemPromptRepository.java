package work.daqian.myai.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import work.daqian.myai.domain.po.SystemPrompt;
import work.daqian.myai.prompt.PromptType;

import java.util.Collection;

public interface SystemPromptRepository extends MongoRepository<SystemPrompt, String> {

    SystemPrompt findSystemPromptBySessionIdAndType(String sessionId, PromptType type);

    SystemPrompt findSystemPromptByUserIdAndType(Long userId, PromptType type);

    void deleteAllBySessionId(String sessionId);

    void deleteAllBySessionIdIn(Collection<String> sessionIds);
}
