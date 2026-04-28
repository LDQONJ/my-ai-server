package work.daqian.myai.tool.impl;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import work.daqian.myai.util.IpUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherTool implements InitializingBean {

    private WebClient webClient;

    @Value("${api.key.weather.id}")
    private String appId;

    @Value("${api.key.weather.secret}")
    private String appSecret;

    public String getWeather(String city, String version, String ip) {
        if (city.equals("当地")) city = IpUtils.getCityFromIp(ip);
        String uri = "/api?unescape=1&hours=1&version=" + version +
                "&appid=" + appId + "&appsecret=" + appSecret +
                "&city=" + city;
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.webClient = WebClient.builder()
                .baseUrl("http://pddfps.tianqiapi.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
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
}
