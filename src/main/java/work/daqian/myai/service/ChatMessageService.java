package work.daqian.myai.service;

import work.daqian.myai.domain.vo.ChatMessageVO;

import java.util.List;

public interface ChatMessageService {
    List<ChatMessageVO> listAllMessage(String sessionId);
    void saveUserMessage(String sessionId, Long userId, String content);
    void saveAssistantMessage(String sessionId,
                              Long userId,
                              String content,
                              String thinking);

    List<ChatMessageVO> listMessagePage(String sessionId, Integer count);
}
