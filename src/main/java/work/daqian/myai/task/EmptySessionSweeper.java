package work.daqian.myai.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import work.daqian.myai.domain.po.ChatSession;
import work.daqian.myai.repository.ChatMessageRepository;
import work.daqian.myai.service.IChatSessionService;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmptySessionSweeper {

    private final IChatSessionService sessionService;

    private final ChatMessageRepository messageRepository;

    // 每半小时清理一下10分钟之前的空对话以及uid为空的message
    @Scheduled(cron = "0 0/30 * * * ?")
    public void cleanEmptySession() {
        // log.info("定时清理空对话");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tenMinutesBefore = now.minusMinutes(10);
        LambdaUpdateWrapper<ChatSession> wrapper = new LambdaUpdateWrapper<ChatSession>()
                .eq(ChatSession::getMessageCount, 0)
                .or()
                .isNull(ChatSession::getUserId);

        sessionService.remove(wrapper);
        messageRepository.deleteChatMessagesByUserIdNullAndCreateTimeBefore(tenMinutesBefore);
    }
}
