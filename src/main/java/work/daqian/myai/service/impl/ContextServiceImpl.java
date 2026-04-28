package work.daqian.myai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import work.daqian.myai.adapter.NonStreamResponse;
import work.daqian.myai.adapter.OllamaModelAdapter;
import work.daqian.myai.common.R;
import work.daqian.myai.domain.dto.Message;
import work.daqian.myai.domain.dto.PromptDTO;
import work.daqian.myai.domain.po.SystemPrompt;
import work.daqian.myai.domain.vo.ChatMessageVO;
import work.daqian.myai.exception.BadRequestException;
import work.daqian.myai.exception.BizIllegalException;
import work.daqian.myai.enums.PromptType;
import work.daqian.myai.repository.SystemPromptRepository;
import work.daqian.myai.service.ChatMessageService;
import work.daqian.myai.service.ContextService;
import work.daqian.myai.service.IModelService;
import work.daqian.myai.service.SystemPromptService;
import work.daqian.myai.util.BeanUtils;
import work.daqian.myai.util.RedisUtil;
import work.daqian.myai.util.SecurityUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class ContextServiceImpl implements ContextService {

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper mapper;

    private static final int MAX_CONTEXT = 20;

    private final ChatMessageService messageService;

    private final SystemPromptService promptService;

    private final SystemPromptRepository promptRepository;

    private final IModelService modelService;

    private final RedisUtil redisUtil;

    private final OllamaModelAdapter ollamaModelAdapter;

    @Override
    public List<Message> getHistory(String sessionId, boolean onlyCache) {
        String key = PromptType.HISTORY.getKeyPrefix() + sessionId;
        String json = redisTemplate.opsForValue().get(key);
        if (json != null && json.isEmpty()) return new ArrayList<>();
        if (json == null) {
            if (onlyCache) return new ArrayList<>();
            json = redisUtil.cacheEmptyIfNE(PromptType.HISTORY.getKeyPrefix(), sessionId, Duration.ofHours(1), (sid) -> {
                String j;
                List<ChatMessageVO> vos = messageService.listMessagePage(sessionId, MAX_CONTEXT);
                if (vos == null || vos.isEmpty()) return "";
                List<Message> messages = BeanUtils.copyList(vos, Message.class);
                try {
                    j = mapper.writeValueAsString(messages);
                } catch (JsonProcessingException e) {
                    return "";
                }
                return j;
            });
        }
        if (json.isEmpty()) return new ArrayList<>();
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
            zipHistory(sessionId);
            return;
        }
        String key = PromptType.HISTORY.getKeyPrefix() + sessionId;
        try {
            redisTemplate.opsForValue().set(key, mapper.writeValueAsString(messages), Duration.ofHours(1));
        } catch (JsonProcessingException e) {
            log.error("保存消息到redis失败");
        }
    }

    @Override
    public void clear(String sessionId) {
        promptRepository.deleteAllBySessionId(sessionId);
        redisTemplate.delete(PromptType.PERSONA.getKeyPrefix() + sessionId);
        redisTemplate.delete(PromptType.RULES.getKeyPrefix() + sessionId);
        redisTemplate.delete(PromptType.SUMMARY.getKeyPrefix() + sessionId);
        redisTemplate.delete(PromptType.HISTORY.getKeyPrefix() + sessionId);
    }

    /**
     * 根据redis的历史记录生成摘要并压缩redis缓存
     * @param sessionId 对话id
     */
    @Override
    public void zipHistory(String sessionId) {
        String key = PromptType.HISTORY.getKeyPrefix() + sessionId;
        List<Message> history = getHistory(sessionId, false);
        history.add(new Message("user", """
                总结之前的对话，生成一段摘要，要求：
                1. 保留关键信息。
                2. 不超过300字。
                摘要格式：
                - 用户的目标
                - 已经讨论过的内容
                - 当前谈论到的内容。
                
                只需回复摘要内容。
                """));
        Object request = Map.of(
                "model", modelService.getCurrentModel().get(),
                "messages", history,
                "stream", false,
                "think", false
        );
        try {
            Class<? extends NonStreamResponse> responseClass = ollamaModelAdapter.getNonStreamResponseClass();
            NonStreamResponse response = ollamaModelAdapter.buildWebClient().post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(responseClass)
                    .block();
            SystemPrompt systemPrompt = new SystemPrompt();
            systemPrompt.setSessionId(sessionId);
            systemPrompt.setType(PromptType.SUMMARY);
            systemPrompt.setContent(response.getContent());
            promptService.saveOrUpdate(systemPrompt);
            // 裁剪redis中的历史记录
            history = history.subList(MAX_CONTEXT / 2, history.size() - 1);
            redisTemplate.opsForValue().set(key, mapper.writeValueAsString(history), Duration.ofHours(1));
        } catch (Exception e) {
            throw new BizIllegalException("压缩上下文失败");
        }
    }

    @Override
    public R<PromptDTO> queryGlobalSystemPrompt() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new BadRequestException("未登录用户无法使用此功能");
        return R.ok(querySystemPrompt(userId));
    }

    @Override
    public R<PromptDTO> querySessionSystemPrompt(String id) {
        return R.ok(querySystemPrompt(id));
    }

    @Override
    public R<Void> updateGlobalSystemPrompt(PromptDTO prompt) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new BadRequestException("未登录用户无法使用此功能");
        updatePrompt(userId, prompt);
        return R.ok();
    }

    @Override
    public R<Void> updateSessionSystemPrompt(String id, PromptDTO prompt) {
        updatePrompt(id, prompt);
        return R.ok();
    }

    @Override
    public String getPersona(String sessionId) {
        return getSystemPrompt(PromptType.PERSONA, sessionId);
    }

    @Override
    public String getRules(String sessionId) {
        return getSystemPrompt(PromptType.RULES, sessionId);
    }

    @Override
    public String getExample(String sessionId) {
        return getSystemPrompt(PromptType.EXAMPLE, sessionId);
    }

    @Override
    public String getSummary(String sessionId) {
        return getSystemPrompt(PromptType.SUMMARY, sessionId);
    }

    private <T> void updatePrompt(T id, PromptDTO prompt) {
        doUpdatePrompt(id, prompt.getPersona(), PromptType.PERSONA);
        doUpdatePrompt(id, prompt.getRules(), PromptType.RULES);
        doUpdatePrompt(id, prompt.getExampleStr(), PromptType.EXAMPLE);
    }

    private <T> void doUpdatePrompt(T id, String promptContent, PromptType type) {
        if (promptContent != null && !promptContent.isEmpty()) {
            SystemPrompt prompt = new SystemPrompt();
            prompt.setContent(promptContent);
            PromptType globalType = PromptType.fromName("global_" + type.getName());
            if (id instanceof Long) {
                prompt.setUserId((Long) id);
                prompt.setType(globalType);
            } else if (id instanceof String) {
                prompt.setSessionId((String) id);
                prompt.setType(type);
            }
            promptService.saveOrUpdate(prompt);
            if (id instanceof Long) {
                redisTemplate.opsForValue().set(globalType.getKeyPrefix() + id, promptContent, Duration.ofHours(1));
            } else {
                redisTemplate.opsForValue().set(type.getKeyPrefix() + id, promptContent, Duration.ofHours(1));
            }
        }
    }

    private <T> PromptDTO querySystemPrompt(T id) {
        String personaContent = null;
        String rulesContent = null;
        String exampleContent = null;

        if (id instanceof Long) {
            personaContent = redisUtil.cacheEmptyIfNE(PromptType.GLOBAL_PERSONA.getKeyPrefix(), (Long) id, Duration.ofHours(1), (uid) -> {
                SystemPrompt globalPersona = promptRepository.findSystemPromptByUserIdAndType(uid, PromptType.GLOBAL_PERSONA);
                return globalPersona != null ? globalPersona.getContent() : "";
            });
            rulesContent = redisUtil.cacheEmptyIfNE(PromptType.GLOBAL_RULES.getKeyPrefix(), (Long) id, Duration.ofHours(1), (uid) -> {
                SystemPrompt globalRules = promptRepository.findSystemPromptByUserIdAndType(uid, PromptType.GLOBAL_RULES);
                return globalRules != null ? globalRules.getContent() : "";
            });
            exampleContent = redisUtil.cacheEmptyIfNE(PromptType.GLOBAL_EXAMPLE.getKeyPrefix(), (Long) id, Duration.ofHours(1), (uid) -> {
                SystemPrompt globalExample = promptRepository.findSystemPromptByUserIdAndType(uid, PromptType.GLOBAL_EXAMPLE);
                return globalExample != null ? globalExample.getContent() : "";
            });
        } else if (id instanceof String) {
            personaContent = redisUtil.cacheEmptyIfNE(PromptType.PERSONA.getKeyPrefix(), (String) id, Duration.ofHours(1), (sid) -> {
                SystemPrompt persona = promptRepository.findSystemPromptBySessionIdAndType(sid, PromptType.PERSONA);
                return persona != null ? persona.getContent() : "";
            });
            rulesContent = redisUtil.cacheEmptyIfNE(PromptType.RULES.getKeyPrefix(), (String) id, Duration.ofHours(1), (sid) -> {
                SystemPrompt rules = promptRepository.findSystemPromptBySessionIdAndType(sid, PromptType.RULES);
                return rules != null ? rules.getContent() : "";
            });
            exampleContent = redisUtil.cacheEmptyIfNE(PromptType.EXAMPLE.getKeyPrefix(), (String) id, Duration.ofHours(1), (sid) -> {
                SystemPrompt example = promptRepository.findSystemPromptBySessionIdAndType(sid, PromptType.EXAMPLE);
                return example != null ? example.getContent() : "";
            });
        }

        PromptDTO promptDTO = new PromptDTO();
        promptDTO.setPersona(personaContent);
        promptDTO.setRules(rulesContent);
        promptDTO.setExampleFromStr(exampleContent);
        return promptDTO;
    }

    private String getSystemPrompt(PromptType promptType, String sessionId) {
        String promptContent = redisUtil.cacheEmptyIfNE(promptType.getKeyPrefix(), sessionId, Duration.ofHours(1), (sid) -> {
            SystemPrompt prompt = promptRepository.findSystemPromptBySessionIdAndType(sid, promptType);
            return prompt != null ? prompt.getContent() : "";
        });
        if (!promptContent.isEmpty() || promptType.equals(PromptType.SUMMARY)) return promptContent;
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return "";
        PromptType globalType = PromptType.fromName("global_" + promptType.getName());
        promptContent = redisUtil.cacheEmptyIfNE(globalType.getKeyPrefix(), userId, Duration.ofHours(1), (uid) -> {
            SystemPrompt prompt = promptRepository.findSystemPromptByUserIdAndType(uid, globalType);
            return prompt != null ? prompt.getContent() : "";
        });
        return promptContent;
    }
}
