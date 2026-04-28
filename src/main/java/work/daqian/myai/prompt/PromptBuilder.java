package work.daqian.myai.prompt;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import work.daqian.myai.domain.dto.Message;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromptBuilder {

    public List<Message> build(PromptContext ctx) {
        List<Message> result = new ArrayList<>();

        // 人格设定
        if (notEmpty(ctx.getPersona())) {
            result.add(system("""
                    这是你的角色设定：
                    """ + ctx.getPersona())
            );
        }

        // 规则限制
        if (notEmpty(ctx.getRules())) {
            result.add(system("""
                    你必须严格遵守以下规则：
                    """ + ctx.getRules()));
        }

        // 对话示例
        if (notEmpty(ctx.getExample())) {
            result.add(system("""
                    按以下示例进行回复：
                    """ + ctx.getExample()));
        }

        // 用户画像
        /* if (notEmpty(ctx.getMemory())) {
            result.add(system("""
                    以下是用户的背景信息，请在回答时参考：
                    """ + trim(ctx.getMemory(), PromptConfig.MEMORY_MAX)));
        } */

        // 历史摘要
        if (notEmpty(ctx.getSummary())) {
            result.add(system(""" 
                    以下是本轮对话历史摘要：
                    """ + ctx.getSummary()));
        }

        // 聊天记录
        result.addAll(ctx.getHistory());

        result.add(ctx.getUserInput());

        return result;
    }

    private Message system(String content) {
        return new Message("system", content);
    }

    private boolean notEmpty(String s) {
        return s != null && !s.isEmpty();
    }
}
