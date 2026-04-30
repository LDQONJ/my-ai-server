package work.daqian.myai.service.impl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import work.daqian.myai.domain.dto.Message;
import work.daqian.myai.domain.po.ChatMessage;
import work.daqian.myai.domain.po.Model;
import work.daqian.myai.domain.vo.ChatMessageVO;
import work.daqian.myai.repository.ChatMessageRepository;
import work.daqian.myai.service.ChatMessageService;
import work.daqian.myai.service.IModelService;
import work.daqian.myai.util.BeanUtils;
import work.daqian.myai.util.SecurityUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository messageRepository;

    private final IModelService modelService;


    @Override
    public void saveUserMessage(String sessionId, Long userId, String content, String audio) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setUserId(userId);
        chatMessage.setSessionId(sessionId);
        chatMessage.setContent(content);
        chatMessage.setRole("user");
        chatMessage.setAudio(audio);
        messageRepository.save(chatMessage);
    }

    @Override
    public void saveAssistantMessage(String sessionId,
                                     Long userId,
                                     String modelName,
                                     String content,
                                     String thinking) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setUserId(userId);
        chatMessage.setSessionId(sessionId);
        chatMessage.setModelName(modelName);
        chatMessage.setContent(content);
        chatMessage.setThinking(thinking);
        chatMessage.setRole("assistant");
        messageRepository.save(chatMessage);
    }

    @Override
    @Transactional
    public void saveAgentToolMessage(String sessionId, Long userId, String modelName, List<Message> agentToolTemp) {
        for (Message message : agentToolTemp) {
            String role = message.getRole();
            if (role.equals("assistant")) {
                saveAssistantMessage(sessionId, userId, modelName, message.getContent(), null);
            } else {
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setUserId(userId);
                chatMessage.setSessionId(sessionId);
                chatMessage.setContent(message.getContent());
                chatMessage.setRole(role);
                messageRepository.save(chatMessage);
            }
        }
    }

    @Override
    public List<ChatMessageVO> listMessagePage(String sessionId, Integer count) {
        List<ChatMessage> messages = messageRepository.findBySessionIdOrderByCreateTimeDesc(sessionId, PageRequest.ofSize(count));
        Collections.reverse(messages);
        return convertToVOS(messages);
    }

    @Override
    public List<ChatMessageVO> listAllMessage(String sessionId) {
        List<ChatMessage> messages = messageRepository.findAllBySessionIdAndRoleNotContainingOrderByCreateTimeAsc(sessionId, "system");
        return convertToVOS(messages);
    }

    private @NonNull List<ChatMessageVO> convertToVOS(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) return List.of();
        Set<String> modelNames = messages.stream().map(ChatMessage::getModelName).collect(Collectors.toSet());
        List<Model> models = modelService.getByFullNames(modelNames);
        Map<String, String> modelNameMap = models.stream().collect(Collectors.toMap(Model::getFullName, Model::getName));
        List<ChatMessageVO> vos = new ArrayList<>(messages.size());
        String username = SecurityUtils.getCurrentUsername();
        for (ChatMessage message : messages) {
            String role = message.getRole();
            if ((role.equals("assistant") && message.getContent().startsWith("{"))
                    || role.equals("tool"))
                continue;
            ChatMessageVO vo = BeanUtils.copyBean(message, ChatMessageVO.class);
            String modelName = message.getModelName();
            if (role.equals("user")) {
                vo.setName(username != null ? username : "匿名用户");
            } else if (modelName == null || modelName.isEmpty()) {
                vo.setName("DeepSeek");
            } else {
                vo.setName(modelNameMap.get(modelName));
            }
            vos.add(vo);
        }
        return vos;
    }


}
