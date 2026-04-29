package work.daqian.myai.prompt;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import work.daqian.myai.domain.dto.Message;
import work.daqian.myai.tool.ToolDefinition;

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

    public String buildToolPrompt(List<ToolDefinition> tds, String ip) {
        StringBuilder sb = new StringBuilder();
        sb.append("你可以使用工具来获取结果。\n\n可用工具:\n\n");
        for (ToolDefinition td : tds) {
            sb.append("工具名: ").append(td.getName());
            sb.append("描述: ").append(td.getDescription());
            sb.append("参数: \n").append(td.getParameters()).append("\n\n");
        }
        sb.append("""
                当需要调用工具时，必须严格输出以下JSON格式:
                
                {
                    "tool": "toolName",
                    "arguments": {
                        "argName": "argValue"
                    }
                }
                
                不要输出任何其他内容。
                
                当前用户的ip地址为: “%s”
                
                之前的调用已经有结果且不再需要调用工具时，不要回复用户消息，直接输出: {}。
                """.formatted(ip));
        return sb.toString();
    }

    public String buildSearchPrompt() {
        ToolDefinition td = new ToolDefinition(
                "webSearch",
                "搜索互联网获取最新信息",
                """
                        {
                            "query": "要搜索的内容"
                        }
                        """
        );
        return """
                你可以使用工具来回答问题。
                
                可用工具:
                
                工具名: %s
                描述: %s
                参数:
                %s
                
                当需要调用工具时，必须严格输出以下JSON格式:
                
                {
                    "tool": "toolName",
                    "arguments": {
                        "argName": "argValue"
                    }
                }
                
                不要输出任何其他内容。
                """.formatted(td.getName(), td.getDescription(), td.getParameters());
    }
}
