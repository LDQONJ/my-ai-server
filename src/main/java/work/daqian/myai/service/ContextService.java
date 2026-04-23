package work.daqian.myai.service;

import work.daqian.myai.common.R;
import work.daqian.myai.domain.dto.Message;
import work.daqian.myai.domain.dto.PromptDTO;

import java.util.List;

public interface ContextService {

    List<Message> getHistory(String sessionId, boolean onlyCache);

    String getPersona(String sessionId);

    String getRules(String sessionId);

    String getSummary(String sessionId);

    void saveHistory(String sessionId, List<Message> messages);

    void clear(String sessionId);

    void zipContext(String sessionId);

    R<PromptDTO> queryGlobalSystemPrompt();

    R<Void> updateGlobalSystemPrompt(PromptDTO prompt);

    R<PromptDTO> querySessionSystemPrompt(String id);

    R<Void> updateSessionSystemPrompt(String id, PromptDTO prompt);
}
