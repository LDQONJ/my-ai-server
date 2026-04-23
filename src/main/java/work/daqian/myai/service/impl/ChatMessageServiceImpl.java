package work.daqian.myai.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import work.daqian.myai.domain.po.ChatMessage;
import work.daqian.myai.domain.vo.ChatMessageVO;
import work.daqian.myai.repository.ChatMessageRepository;
import work.daqian.myai.service.ChatMessageService;
import work.daqian.myai.util.BeanUtils;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository messageRepository;


    @Override
    public void saveUserMessage(String sessionId, Long userId, String content) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setUserId(userId);
        chatMessage.setSessionId(sessionId);
        chatMessage.setContent(content);
        chatMessage.setRole("user");
        messageRepository.save(chatMessage);
    }

    @Override
    public void saveAssistantMessage(String sessionId,
                                        Long userId,
                                        String content,
                                        String thinking) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setUserId(userId);
        chatMessage.setSessionId(sessionId);
        chatMessage.setContent(content);
        chatMessage.setThinking(thinking);
        chatMessage.setRole("assistant");
        messageRepository.save(chatMessage);
    }

    @Override
    public List<ChatMessageVO> listMessagePage(String sessionId, Integer count) {
        List<ChatMessage> messages = messageRepository.findBySessionIdOrderByCreateTimeDesc(sessionId, PageRequest.ofSize(count));
        Collections.reverse(messages);
        return BeanUtils.copyList(messages, ChatMessageVO.class);
    }

    @Override
    public List<ChatMessageVO> listAllMessage(String sessionId) {
        List<ChatMessage> messages = messageRepository.findAllBySessionIdAndRoleNotContainingOrderByCreateTimeAsc(sessionId, "system");
        return BeanUtils.copyList(messages, ChatMessageVO.class);
    }


}
