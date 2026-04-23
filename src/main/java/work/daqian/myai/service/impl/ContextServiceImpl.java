package work.daqian.myai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import work.daqian.myai.common.R;
import work.daqian.myai.domain.dto.ChatRequest;
import work.daqian.myai.domain.dto.ChatResponse;
import work.daqian.myai.domain.dto.Message;
import work.daqian.myai.domain.dto.PromptDTO;
import work.daqian.myai.domain.po.SystemPrompt;
import work.daqian.myai.domain.vo.ChatMessageVO;
import work.daqian.myai.exception.BadRequestException;
import work.daqian.myai.exception.BizIllegalException;
import work.daqian.myai.repository.SystemPromptRepository;
import work.daqian.myai.service.ChatMessageService;
import work.daqian.myai.service.ContextService;
import work.daqian.myai.service.IModelService;
import work.daqian.myai.service.SystemPromptService;
import work.daqian.myai.util.BeanUtils;
import work.daqian.myai.util.UserContext;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static work.daqian.myai.service.impl.ChatServiceImpl.webClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContextServiceImpl implements ContextService {

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper mapper;

    private static final String HISTORY_PREFIX = "context:history:sid:";

    private static final String SUMMARY_PREFIX = "context:summary:sid:";

    private static final String PERSONA_PREFIX = "context:parsona:sid:";

    private static final String RULES_PREFIX = "context:rules:sid:";

    private static final int MAX_CONTEXT = 20;

    private final ChatMessageService messageService;

    private final SystemPromptService promptService;

    private final SystemPromptRepository promptRepository;

    private final IModelService modelService;

    @Override
    public List<Message> getHistory(String sessionId, boolean onlyCache) {
        String key = HISTORY_PREFIX + sessionId;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            if (onlyCache) return new ArrayList<>();
            List<ChatMessageVO> vos = messageService.listMessagePage(sessionId, MAX_CONTEXT); //
            List<Message> messages = BeanUtils.copyList(vos, Message.class);
            if (!messages.isEmpty() && messages.get(messages.size() - 1).getRole().equals("user"))
                messages = messages.subList(0, messages.size() - 1);
            try {
                json = mapper.writeValueAsString(messages);
            } catch (JsonProcessingException e) {
                return new ArrayList<>();
            }
            redisTemplate.opsForValue().set(key, json);
            return new ArrayList<>(messages);
        }
        try {
            Message[] messageArray = mapper.readValue(json, Message[].class);
            List<Message> messages = Arrays.asList(messageArray);
            if (!messages.isEmpty() && messages.get(messages.size() - 1).getRole().equals("user"))
                messages = messages.subList(0, messages.size() - 1);
            return new ArrayList<>(messages);
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void saveHistory(String sessionId, List<Message> messages) {
        int size = messages.size();
        if (size > MAX_CONTEXT) {
            zipContext(sessionId);
            return;
        }
        String key = HISTORY_PREFIX + sessionId;
        try {
            redisTemplate.opsForValue().set(key, mapper.writeValueAsString(messages));
            redisTemplate.expire(key, Duration.ofHours(1));
        } catch (JsonProcessingException e) {
            log.error("保存消息到redis失败");
        }
    }

    @Override
    public void clear(String sessionId) {
        redisTemplate.delete(PERSONA_PREFIX + sessionId);
        redisTemplate.delete(RULES_PREFIX + sessionId);
        redisTemplate.delete(SUMMARY_PREFIX + sessionId);
        redisTemplate.delete(HISTORY_PREFIX + sessionId);
        promptRepository.deleteAllBySessionId(sessionId);
    }

    /**
     * 根据redis的历史记录生成摘要并压缩redis缓存
     * @param sessionId 对话id
     */
    @Override
    public void zipContext(String sessionId) {
        String key = HISTORY_PREFIX + sessionId;
        List<Message> history = getHistory(sessionId, false);
        history.add(new Message(
                "user",
                """
                        总结之前的对话，生成一段摘要，要求：
                        1. 保留关键信息。
                        2. 不超过300字。
                        摘要格式：
                        - 用户的目标
                        - 已经讨论过的内容
                        - 当前谈论到的内容。
                        
                        只需回复摘要内容。
                        """
        ));
        ChatRequest request = new ChatRequest(
                modelService.getCurrentModel().get(),
                history,
                false,
                false
        );
        try {
            ChatResponse response = webClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatResponse.class)
                    .block();
            Message message = response.getMessage();
            SystemPrompt systemPrompt = new SystemPrompt();
            systemPrompt.setSessionId(sessionId);
            systemPrompt.setType("summary");
            systemPrompt.setContent(message.getContent());
            promptService.saveOrUpdate(systemPrompt);
            // 裁剪redis中的历史记录
            history.subList(MAX_CONTEXT / 2, history.size() - 1);
            redisTemplate.opsForValue().set(key, mapper.writeValueAsString(history));
            redisTemplate.expire(key, Duration.ofHours(1));
        } catch (Exception e) {
            throw new BizIllegalException("压缩上下文失败");
        }
    }

    @Override
    public R<PromptDTO> queryGlobalSystemPrompt() {
        Long userId = UserContext.getUser();
        if (userId == null) throw new BadRequestException("未登录用户无法使用此功能");
        return R.ok(querySystemPrompt(
                userId,
                (uid) -> promptRepository.findSystemPromptByUserIdAndType(uid, "global_persona"),
                (uid) -> promptRepository.findSystemPromptByUserIdAndType(uid, "global_rules")
        ));
    }

    @Override
    public R<PromptDTO> querySessionSystemPrompt(String id) {
        return R.ok(querySystemPrompt(
                id,
                (sessionId) -> promptRepository.findSystemPromptBySessionIdAndType(sessionId, "persona"),
                (sessionId) -> promptRepository.findSystemPromptBySessionIdAndType(sessionId, "rules")
        ));
    }

    @Override
    public R<Void> updateGlobalSystemPrompt(PromptDTO prompt) {
        Long userId = UserContext.getUser();
        if (userId == null) throw new BadRequestException("未登录用户无法使用此功能");
        updateSystemPrompt(userId, prompt, SystemPrompt::setUserId);
        return R.ok();
    }

    @Override
    public R<Void> updateSessionSystemPrompt(String id, PromptDTO prompt) {
        updateSystemPrompt(id, prompt, SystemPrompt::setSessionId);
        return R.ok();
    }

    @Override
    public String getPersona(String sessionId) {
        Long userId = UserContext.getUser();
        if (userId == null) return null;
        return getSystemPrompt(
                PERSONA_PREFIX,
                sessionId,
                (sid) -> {
                    SystemPrompt prompt = promptRepository.findSystemPromptBySessionIdAndType(sid, "persona");
                    if (prompt == null)
                        prompt = promptRepository.findSystemPromptByUserIdAndType(userId, "global_persona");
                    if (prompt == null) return null;
                    return prompt.getContent();
                }
        );
    }

    @Override
    public String getRules(String sessionId) {
        Long userId = UserContext.getUser();
        if (userId == null) return null;
        return getSystemPrompt(
                RULES_PREFIX,
                sessionId,
                (sid) -> {
                    SystemPrompt prompt = promptRepository.findSystemPromptBySessionIdAndType(sid, "rules");
                    if (prompt == null)
                        prompt = promptRepository.findSystemPromptByUserIdAndType(userId, "global_rules");
                    if (prompt == null) return null;
                    return prompt.getContent();
                }
        );
    }

    @Override
    public String getSummary(String sessionId) {
        Long userId = UserContext.getUser();
        if (userId == null) return null;
        return getSystemPrompt(
                SUMMARY_PREFIX,
                sessionId,
                (sid) -> {
                    SystemPrompt prompt = promptRepository.findSystemPromptBySessionIdAndType(sid, "summary");
                    if (prompt == null) return null;
                    return prompt.getContent();
                }
        );
    }

    private <T> void updateSystemPrompt(T id, PromptDTO prompt, BiConsumer<SystemPrompt, T> setId) {
        String personaContent = prompt.getPersona();
        String rulesContent = prompt.getRules();

        if (personaContent != null && !personaContent.isEmpty()) {
            SystemPrompt persona = new SystemPrompt();
            setId.accept(persona, id);
            persona.setContent(personaContent);
            if (id instanceof Long) {
                persona.setType("global_persona");
            } else {
                persona.setType("persona");
            }
            promptService.saveOrUpdate(persona);
        }
        if (rulesContent != null && !rulesContent.isEmpty()) {
            SystemPrompt rules = new SystemPrompt();
            setId.accept(rules, id);
            rules.setContent(rulesContent);
            if (id instanceof Long) {
                rules.setType("global_rules");
            } else {
                rules.setType("rules");
            }
            promptService.saveOrUpdate(rules);
        }
    }

    private <T> PromptDTO querySystemPrompt(T id, Function<T, SystemPrompt> queryPersona, Function<T, SystemPrompt> queryRules) {
        SystemPrompt persona = queryPersona.apply(id);
        SystemPrompt rules = queryRules.apply(id);
        PromptDTO promptDTO = new PromptDTO();
        if (persona != null)
            promptDTO.setPersona(persona.getContent());
        if (rules != null)
            promptDTO.setRules(rules.getContent());
        return promptDTO;
    }

    private String getSystemPrompt(String keyPrefix, String sessionId, Function<String, String> getFromDb) {
        String key = keyPrefix + sessionId;
        String prompt = redisTemplate.opsForValue().get(key);
        if (prompt == null) {
            prompt = getFromDb.apply(sessionId);
            if (prompt != null)
                redisTemplate.opsForValue().set(key, prompt);
        }
        return prompt;
    }
}
