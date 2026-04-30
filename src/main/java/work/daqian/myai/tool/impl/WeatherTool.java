package work.daqian.myai.tool.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import work.daqian.myai.tool.Tool;
import work.daqian.myai.tool.ToolDefinition;
import work.daqian.myai.websocket.WebSocketService;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherTool implements Tool,InitializingBean {

    private final WebSocketService webSocketService;
    private WebClient webClient;

    @Value("${api.key.weather.id}")
    private String appId;

    @Value("${api.key.weather.secret}")
    private String appSecret;

    private final ObjectMapper mapper;

    public String getWeather(String city, String version) {
        String uri = "/api?unescape=1&hours=no&index=no&version=" + version +
                "&appid=" + appId + "&appsecret=" + appSecret +
                "&city=" + city;
        String json = webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        try {
            JsonNode node = mapper.readTree(json);
            JsonNode data = node.path("data");
            WeatherResponse weatherResponse = null;
            if (data != null && !data.isEmpty()) {
                // 使用自定义类获取需要的字段，并把每一天的具体数据拼接到一起，减少重复的字段名称，进而减少token消耗
                weatherResponse = mapper.convertValue(node, WeatherResponse.class);
                List<WeatherResponse.EachDay> days = weatherResponse.getData();
                WeatherResponse.EachDay all = days.get(0);
                for (int i = 1; i < days.size(); i++) {
                    WeatherResponse.EachDay day = days.get(i);
                    all = all.addDay(day);
                }
                json = mapper.writeValueAsString(all);
            }
        } catch (Exception e) {
            log.error("转换七日天气失败");
        }
        return json;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.webClient = WebClient.builder()
                .baseUrl("http://pddfps.tianqiapi.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return new ToolDefinition(
                "getWeather",
                "获取指定城市的当日天气或七日天气",
                """
                        {
                            "city": "城市名或区县名", /* 如北京、上海、深圳，不带“市”、“县”等后缀 */
                            "version": "版本号" /* “v9” 或 “v63”，v9是查询七日天气，v63是查询当日天气 */
                        }
                        """
        );
    }

    @Override
    public String doTool(String wsId, Map<String, Object> arguments) {
        webSocketService.sendMessageToClient(wsId, "正在获取天气信息...");
        return getWeather((String) arguments.get("city"), (String) arguments.get("version"));
    }

    @Data
    static class RequestParam {
        /**
         * appid	是	string	用户appid	注册开发账号
         * appsecret	是	string	用户appsecret
         * version	是	string	接口版本标识	固定值: v63 每个接口的version值都不一样
         * adcode	否	string	国家统计局城市ID	如：130200000000 请参考 全国统计用区划代码表
         * cityid	否	string	城市ID	请参考 城市ID列表
         * city	否	string	城市名称	不要带市和区; 如: 青岛、铁西
         * province	否	string	所在省	如果您担心city重名可传此参数, 不要带省和市; 如: 山东、上海
         * ip	否	string	IP地址	查询IP所在城市天气
         * lng	否	String	经度	如: 119.545023 (需额外付费开通, 500/年, 1500永久)
         * lat	否	String	纬度	如: 36.044254
         * point	否	String	坐标体系	默认百度坐标, 如使用高德坐标, 请传参: gaode
         * life	否	String	是否展示生活指数	不为空为展示, 如: 1
         * hours	否	String	是否隐藏小时预报	不为空为隐藏, 如: 1
         * callback	否	string	jsonp参数	如: jQuery.Callbacks
         * vue	否	string	跨域参数	如果您使用的是react、vue、angular请填写值: 1
         * unescape	否	Int	是否转义中文	如果您希望json不被unicode, 直接输出中文, 请传此参数: 1
         */
        private String appid;
        private String appsecret;
        private String version;
        private String adcode;
        private String cityid;
        private String city;
        private String province;
        private String ip;
        private String lng;
        private String lat;
        private String point;
        private String life;
        private String hours;
        private String callback;
        private String vue;
        private String unescape;
    }

    @Data
    static class WeatherResponse {
        List<EachDay> data;

        @Data
        static class EachDay {
            private String day = "无";
            private String date = "无";
            private String week = "无";
            private String wea = "无";
            private String wea_day = "无";
            private String wea_night = "无";
            private String tem = "无";
            private String tem1 = "无";
            private String tem2 = "无";
            private String humidity = "无";
            private String win_speed = "无";
            private String sunrise = "无";
            private String sunset = "无";
            private String air = "无";
            private String air_level = "无";
            private String uvIndex = "无";
            private String uvDescription = "无";

            public EachDay addDay(EachDay day) {
                Field[] fields = EachDay.class.getDeclaredFields();
                try {
                    if (fields[0].get(this) != null) {
                        for (Field field : fields) {
                            field.setAccessible(true);
                            Object o = field.get(this);
                            String allDayValue = o.toString();
                            String nextDayValue = field.get(day).toString();
                            field.set(this, allDayValue + "," + nextDayValue);
                        }
                    }
                } catch (Exception e) {
                    log.error("拼接失败");
                }
                return this;
            }
        }
    }
}
