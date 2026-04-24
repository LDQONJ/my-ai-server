package work.daqian.myai.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import work.daqian.myai.domain.po.ChatSession;
import work.daqian.myai.repository.ChatMessageRepository;
import work.daqian.myai.repository.SystemPromptRepository;
import work.daqian.myai.service.IChatSessionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmptySessionSweeper {

    private final IChatSessionService sessionService;

    private final ChatMessageRepository messageRepository;

    private final SystemPromptRepository promptRepository;

    // 每半小时清理一下10分钟之前的空对话以及uid为空的message
    @Scheduled(cron = "0 0/30 * * * ?")
    public void cleanEmptySession() {
        // log.info("定时清理空对话");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tenMinutesBefore = now.minusMinutes(10);
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getMessageCount, 0)
                .or()
                .isNull(ChatSession::getUserId);
        List<ChatSession> sessions = sessionService.list(wrapper);
        Set<String> sids = sessions.stream().map(ChatSession::getId).collect(Collectors.toSet());
        sessionService.removeBatchByIds(sessions);
        messageRepository.deleteChatMessagesByUserIdNullAndCreateTimeBefore(tenMinutesBefore);
        promptRepository.deleteAllBySessionIdIn(sids);
    }
}
