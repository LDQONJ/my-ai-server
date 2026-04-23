package work.daqian.myai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import work.daqian.myai.mapper.ChatSessionMapper;
import work.daqian.myai.repository.ChatMessageRepository;
import work.daqian.myai.service.ChatMessageService;
import work.daqian.myai.service.ContextService;
import work.daqian.myai.service.IChatSessionService;
import work.daqian.myai.service.IModelService;
import work.daqian.myai.util.BeanUtils;
import work.daqian.myai.util.UserContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static work.daqian.myai.service.impl.ChatServiceImpl.toSSEDone;
import static work.daqian.myai.service.impl.ChatServiceImpl.webClient;

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

    private final ObjectMapper mapper;

    @Autowired
    @Qualifier("taskExecutor")
    private Executor taskExecutor;

    @Override
    public R<String> create() {
        Long userId = UserContext.getUser();
        ChatSession chatSession = new ChatSession();
        chatSession.setUserId(userId); // 未登录用户 userId 为 null
        chatSession.setTitle("新对话");
        save(chatSession);
        String sessionId = chatSession.getId();
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
    public R<List<ChatSessionVO>> listMySessions() {
        Long userId = UserContext.getUser();
        if (userId == null) return R.ok(List.of());
        List<ChatSession> sessions = lambdaQuery()
                .eq(ChatSession::getUserId, userId)
                .orderBy(true, false, ChatSession::getUpdateTime)
                .list();
        List<ChatSessionVO> vos = sessions.stream()
                .filter(s -> !s.getTitle().equals("新对话"))
                .map(s -> BeanUtils.copyBean(s, ChatSessionVO.class))
                .collect(Collectors.toList());
        return R.ok(vos);
    }

    @Override
    @Transactional
    public R<Void> delete(String id) {
        removeById(id);
        contextService.clear(id);
        messageRepository.deleteChatMessagesBySessionId(id);
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
        List<Message> context = new ArrayList<>();
        int count = 0;
        try {
            while (context.isEmpty() && count < 100) {
                count ++;
                Thread.sleep(100);
                context = contextService.getHistory(id, true);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (context.isEmpty()) throw new BadRequestException("对话记录为空，无法生成标题");
        context.add(new Message(
                "user",
                """
                        给以上对话生成一个标题，要求：
                        1. 10个字以内
                        2. 只需回复标题
                        """
        ));
        ChatRequest request = new ChatRequest(
                modelService.getCurrentModel().get(),
                context,
                true,
                false
        );
        StringBuilder contentBuilder = new StringBuilder();
        return webClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(chunk -> parseChunk(chunk, contentBuilder, null))
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
    public Flux<String> parseChunk(String chunk, StringBuilder contentBuilder, StringBuilder thinkingBuilder) {
        try {
            String[] lines = chunk.split("\n");
            List<String> result = new ArrayList<>();

            // 锁定 contentBuilder 确保 append 过程不被 doFinally 的 toString 打断
            synchronized (contentBuilder) {
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    JsonNode node = mapper.readTree(line);
                    JsonNode message = node.path("message");

                    // 1. 解析思考内容
                    String thinking = message.path("thinking").asText();
                    if (thinking != null && !thinking.isEmpty()) {
                        thinking.codePoints().forEach(cp -> result.add(toJson("thinking", new String(Character.toChars(cp)))));
                        if (thinkingBuilder != null)
                            thinkingBuilder.append(thinking);
                    }

                    // 2. 解析正常内容
                    String content = message.path("content").asText();
                    if (content != null && !content.isEmpty()) {
                        content.codePoints().forEach(cp -> result.add(toJson("content", new String(Character.toChars(cp)))));
                        contentBuilder.append(content);
                    }
                }
            }
            return Flux.fromIterable(result);
        } catch (Exception e) {
            return Flux.empty();
        }
    }

    private String toJson(String type, String text) {
        try {
            return mapper.writeValueAsString(
                    Map.of(
                            "type", type,
                            "content", text
                    )
            );
        } catch (Exception e) {
            return "";
        }
    }
}
