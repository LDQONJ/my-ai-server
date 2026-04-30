package work.daqian.myai.service;

import work.daqian.myai.domain.dto.Message;
import work.daqian.myai.domain.vo.ChatMessageVO;

import java.util.List;

public interface ChatMessageService {
    List<ChatMessageVO> listAllMessage(String sessionId);
    void saveUserMessage(String sessionId, Long userId, String content, String audio);
    void saveAssistantMessage(String sessionId,
                              Long userId,
                              String modelName,
                              String content,
                              String thinking);

    List<ChatMessageVO> listMessagePage(String sessionId, Integer count);

    void saveAgentToolMessage(String sessionId, Long userId, String modelName, List<Message> agentToolTemp);
}
