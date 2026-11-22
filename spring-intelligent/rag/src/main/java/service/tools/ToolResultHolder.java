package service.tools;

import cn.hutool.core.lang.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


public class ToolResultHolder {

    private static final Map<String, Map<String, Object>> HANDLER_MAP = new ConcurrentHashMap<>();
   /**
    * 第一处
    * 参数	        实际值	                            说明
    * key	    "requestId，如 ""a1b2c3d4..."""	    本次对话请求的唯一 ID，来自 toolContext
    * field	    courseBO_101	                    生成：类名首字母小写 + _ + 课程 id
    * result	CourseBO 对象	                    存储的 result 是一个 CourseBO 对的所有信息的所有信息
    * 第二处处
    参数	        实际值	                            说明
    key	    messageId，如 chatcmpl-435e81b-...	    大模型响应的消息 id，来自 ChatResponse metadata
    field	"""requestId"""	                        "固定字符串，表示""这里存的是 requestId 指针"""
    result	requestId 字符串	                    本次请求的 UUID
    */

    private ToolResultHolder() {
    }

    public static void put(String key, String field, Object result) {
        Assert.notNull(key, "key is not null!");
        Assert.notNull(field, "field is not null!");
        HANDLER_MAP.computeIfAbsent(key, k -> new HashMap<>()).put(field, result);
    }

    public static Map<String, Object> get(String key) {
        return key == null ? null : HANDLER_MAP.get(key);
    }

    public static Object get(String key, String field) {
        Assert.notNull(key, "key is not null!");
        Assert.notNull(field, "field is not null!");
        return Optional.ofNullable(HANDLER_MAP.get(key))
                .map(map -> map.get(field))
                .orElse(null);
    }

    public static void remove(String key) {
        Assert.notNull(key, "key is not null!");
        HANDLER_MAP.remove(key);
    }

}
