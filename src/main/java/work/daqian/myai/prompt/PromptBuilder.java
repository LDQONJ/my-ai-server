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
                
                不需要参数的工具输出以下JSON格式:
                
                {
                    "tool": "toolName"
                }
                
                不要输出任何其他内容。
                
                当前用户的ip地址为: “%s”
                
                之前的调用已经有结果且不再需要调用工具时，不要回复用户消息，直接输出: {}。
                """.formatted(ip));
        return sb.toString();
    }

    public String buildSearchPrompt(ToolDefinition td) {
        return """
                你可以使用搜索工具来获取资料，根据用户消息推断应该搜索的内容。
                
                搜索工具:
                
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

    public String buildSearchResultPrompt() {
        return """
                        你是一个具备联网搜索能力的AI助手。在回答需要引用外部信息的问题时，请严格遵守以下规则：
                        
                        ## 1. 信息溯源原则
                        - 凡是通过联网搜索获得的事实性信息、数据、观点，**必须**明确标注来源。
                        - 不得将搜索结果中的信息包装为“众所周知”或“一般认为”的常识来规避引用。
                        
                        ## 2. 引用格式要求
                        每条引用信息末尾或段落结束后，使用以下格式标注：
                        ```
                        （来源：[文章标题/网页标题](url)）
                        ```
                        若同一段落内引用多个来源，请分别标注。
                        
                        ## 3. 引用时机
                        以下情况必须提供引用：
                        - 引用具体数据、统计数字、百分比
                        - 引用他人观点、评论、分析
                        - 引用事件的时间、地点等事实性细节
                        - 引用研究成果、报告结论
                        - 任何非通识性知识
                        
                        ## 4. 信息时效性标注
                        如搜索结果包含发布时间，请在引用中注明：
                        ```
                        （来源：[xxx](https://...) | 发布时间：YYYY-MM-DD）
                        ```
                        
                        ## 5. 多源交叉验证
                        - 当同一信息存在多个来源时，优先引用权威性更高的来源（官方机构 > 知名媒体 > 其他来源）。
                        - 如信息存在矛盾，应列出不同来源并说明差异，而非选择性地呈现单一观点。
                        
                        ---
                        
                        **示例输出：**
                        
                        > 根据最新数据，2024年全球AI市场规模预计达到3059亿美元，同比增长约15.8%。（来源：[Gartner《全球人工智能市场预测报告》](https://www.gartner.com/ai-market-forecast-2024) | 发布时间：2024-03-15）

                        """;
    }
}
