package start.service;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class Tool2 {

    @Tool(description = "根据城市id查询未来几天的天气预报")
    public String getWeatherForecast(
            @ToolParam(description = "城市id") String cityId,
            @ToolParam(description = "预报天数，默认3天，最多15天", required = false) Integer days) {
        int count = (days == null || days <= 0) ? 3 : Math.min(days, 15);
        String url = "http://t.weather.itboy.net/api/weather/city/" + cityId;
        String data = HttpUtil.get(url);
        JSONObject jsonObject = JSONUtil.parseObj(data);
        JSONArray forecast = jsonObject.getByPath("data.forecast", JSONArray.class);
        if (forecast == null || forecast.isEmpty()) {
            return "未获取到该城市的预报信息，请检查城市id：" + cityId;
        }
        StringBuilder sb = new StringBuilder("城市：")
                .append(jsonObject.getByPath("cityInfo.city", String.class))
                .append("，预报如下：");
        for (int i = 0; i < Math.min(count, forecast.size()); i++) {
            JSONObject item = forecast.getJSONObject(i);
            sb.append("\n").append(i + 1).append(") 日期：").append(item.getStr("ymd"))
                    .append("，天气：").append(item.getStr("type"))
                    .append("，").append(item.getStr("high"))
                    .append(" ").append(item.getStr("low"))
                    .append("，提示：").append(item.getStr("notice"));
        }
        return sb.toString();
    }
}
