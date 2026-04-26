package work.daqian.myai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import work.daqian.myai.common.R;
import work.daqian.myai.domain.dto.ChatRequest;
import work.daqian.myai.domain.dto.Message;
import work.daqian.myai.domain.po.ChatSession;
import work.daqian.myai.domain.vo.ChatMessageVO;
import work.daqian.myai.domain.vo.ChatSessionVO;
import work.daqian.myai.exception.BadRequestException;
import work.daqian.myai.exception.BizIllegalException;
import work.daqian.myai.mapper.ChatSessionMapper;
import work.daqian.myai.prompt.PromptBuilder;
import work.daqian.myai.prompt.PromptContext;
import work.daqian.myai.repository.ChatMessageRepository;
import work.daqian.myai.service.ChatMessageService;
import work.daqian.myai.service.ContextService;
import work.daqian.myai.service.IChatSessionService;
import work.daqian.myai.service.IModelService;
import work.daqian.myai.util.BeanUtils;
import work.daqian.myai.util.RedisUtil;
import work.daqian.myai.util.SecurityUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static work.daqian.myai.service.impl.ChatServiceImpl.ollamaClient;
import static work.daqian.myai.service.impl.ChatServiceImpl.toSSEDone;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 李达千
 * @since 2026-04-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements IChatSessionService {

    private final ChatMessageService messageService;

    private final ChatMessageRepository messageRepository;

    private final ContextService contextService;

    private final IModelService modelService;

    private final RedisUtil redisUtil;

    private final StringRedisTemplate redisTemplate;

    private static final String SESSION_LIST_PREFIX = "session:list:uid:";

    private final ObjectMapper mapper;

    private final PromptBuilder promptBuilder;

    @Autowired
    @Qualifier("taskExecutor")
    private Executor taskExecutor;

    @Override
    public R<String> create() {
        Long userId = SecurityUtils.getCurrentUserId();
        ChatSession chatSession = new ChatSession();
        chatSession.setUserId(userId); // 未登录用户 userId 为 null
        chatSession.setTitle("新对话");
        save(chatSession);
        String sessionId = chatSession.getId();
        redisTemplate.delete(SESSION_LIST_PREFIX + userId);
        return R.ok(sessionId);
    }

    @Override
    public R<Void> rename(String sessionId, String title) {
        ChatSession chatSession = new ChatSession();
        chatSession.setId(sessionId);
        chatSession.setTitle(title);
        updateById(chatSession);
        return R.ok();
    }

    @Override
    public R<String> listMySessions() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.ok("");
        String vosJson = redisUtil.cacheEmptyIfNE(SESSION_LIST_PREFIX, userId, Duration.ofHours(1), (uid) -> {
            List<ChatSession> sessions = lambdaQuery()
                    .eq(ChatSession::getUserId, uid)
                    .orderBy(true, false, ChatSession::getUpdateTime)
                    .list();
            List<ChatSessionVO> vos = sessions.stream()
                    .filter(s -> !s.getTitle().equals("新对话"))
                    .map(s -> BeanUtils.copyBean(s, ChatSessionVO.class))
                    .toList();
            String result;
            try {
                result = mapper.writeValueAsString(vos);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            return result;
        });
        return R.ok(vosJson);
    }

    @Override
    @Transactional
    public R<Void> delete(String id) {
        removeById(id);
        contextService.clear(id);
        messageRepository.deleteChatMessagesBySessionId(id);
        redisTemplate.delete(SESSION_LIST_PREFIX + SecurityUtils.getCurrentUserId());
        return R.ok();
    }

    @Override
    public R<ChatSessionVO> queryById(String id) {
        ChatSession session = getById(id);
        if (session == null) throw new BadRequestException("对话已过期");
        List<ChatMessageVO> messages = messageService.listAllMessage(id);
        ChatSessionVO chatSessionVO = new ChatSessionVO();
        chatSessionVO.setMessages(messages);
        chatSessionVO.setTitle(session.getTitle());
        return R.ok(chatSessionVO);
    }

    @Override
    public Flux<String> generateTitle(String id) {
        List<Message> history = new ArrayList<>();
        int count = 0;
        try {
            while (history.isEmpty() && count < 100) {
                count++;
                Thread.sleep(100);
                history = contextService.getHistory(id, true);
            }
        } catch (InterruptedException e) {
            throw new BizIllegalException("生成标题失败");
        }
        if (history.isEmpty()) throw new BadRequestException("对话记录为空，无法生成标题");
        PromptContext promptContext = PromptContext.builder()
                .rules("""
                        为以下对话生成一个标题，要求：
                        1. 只能输出标题本身
                        2. 不要添加任何解释或前缀
                        3. 不要出现“好的”、“标题是”等内容
                        4. 标题不超过10个字
                        """)
                .history(history)
                .userInput(new Message("user", "为以上对话生成标题，只需回复标题内容"))
                .build();
        List<Message> prompt = promptBuilder.build(promptContext);
        String currentModel = modelService.getCurrentModel().get();
        ChatRequest request = new ChatRequest(
                // deepseek-r1 无法关闭思考模式，导致生成标题的过程属于思考内容，往往不会服从指令，需要使用其他轻量模型生成标题
                currentModel.startsWith("deepseek") ? "qwen3.5:9b" : currentModel,
                prompt,
                true,
                false
        );
        StringBuilder contentBuilder = new StringBuilder();
        return ollamaClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(chunk -> parseChunk(chunk, contentBuilder))
                // .delayElements(java.time.Duration.ofMillis(100)) 让前端延迟，不然数据容易乱序到达前端
                .filter(content -> content != null && !content.isEmpty())
                .concatWith(Flux.defer(() -> {
                    String title = contentBuilder.toString();
                    CompletableFuture.runAsync(() -> lambdaUpdate()
                            .eq(ChatSession::getId, id)
                            .set(ChatSession::getTitle, title)
                            .update(), taskExecutor);
                    return Flux.just(toSSEDone());
                }));
    }

    // 解析 Ollama chunk
    public Flux<String> parseChunk(String chunk, StringBuilder contentBuilder) {
        try {
            String[] lines = chunk.split("\n");
            List<String> result = new ArrayList<>();
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                JsonNode node = mapper.readTree(line);
                JsonNode message = node.path("message");
                String content = message.path("content").asText();
                if (content != null && !content.isEmpty()) {
                    content.codePoints().forEach(cp -> result.add(toJson(new String(Character.toChars(cp)))));
                    contentBuilder.append(content);
                }
            }
            return Flux.fromIterable(result);
        } catch (Exception e) {
            return Flux.empty();
        }
    }

    private String toJson(String text) {
        try {
            return mapper.writeValueAsString(Map.of("type", "content", "content", text));
        } catch (Exception e) {
            return "";
        }
    }
}
