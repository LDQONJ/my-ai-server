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

    public String buildToolPrompt(String ip) {
        List<ToolDefinition> tds = new ArrayList<>();
        tds.add(new ToolDefinition(
                "getWeather",
                "查询某个城市的天气",
                """
                        {
                            "city": "城市名称"
                        }
                        """
        ));
        return """
                你可以使用工具来回答问题。
                
                可用工具:
                
                工具名: getWeather
                描述: 查询某个城市的天气
                参数:
                {
                    "city": "城市名称", /* 如北京、深圳，如果用户没说具体位置就填“当地” */
                    "version" "v63", /* v63为当日天气，v9为七天天气 */
                    "ip": "%s" /* city参数为“当地”时才添加ip参数，参数值就用这里的值 */
                }
                
                当需要调用工具时，必须严格输出以下JSON格式:
                
                {
                    "tool": "toolName",
                    "arguments": {
                        "argName": "argValue"
                    }
                }
                
                不要输出任何其他内容。
                
                不需要调用工具时，输出: {}
                """.formatted(ip);
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
