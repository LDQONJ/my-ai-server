package work.daqian.myai.tool.impl;

import org.springframework.stereotype.Service;
import work.daqian.myai.tool.Tool;
import work.daqian.myai.tool.ToolDefinition;
import work.daqian.myai.util.IpUtils;

@Service
public class CityTool implements Tool {

    public String getCity(String ip) {
        return IpUtils.getCityFromIp(ip);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return new ToolDefinition(
                "getCity",
                "根据 IP 获取城市名称",
                """
                        {
                            "ip": "ip地址" /* ipv4或者ipv6地址值，无需特殊处理 */
                        }
                        """
        );
    }
}
