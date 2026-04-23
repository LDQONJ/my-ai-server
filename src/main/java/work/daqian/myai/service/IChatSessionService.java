package work.daqian.myai.service;

import reactor.core.publisher.Flux;
import work.daqian.myai.common.R;
import work.daqian.myai.domain.po.ChatSession;
import com.baomidou.mybatisplus.extension.service.IService;
import work.daqian.myai.domain.vo.ChatSessionVO;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 李达千
 * @since 2026-04-18
 */
public interface IChatSessionService extends IService<ChatSession> {

    R<String> create();

    R<Void> rename(String sessionId, String title);

    R<List<ChatSessionVO>> listMySessions();

    R<Void> delete(String id);

    R<ChatSessionVO> queryById(String id);

    Flux<String> generateTitle(String id);
}
