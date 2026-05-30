import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPooled;

public class TestLink {
    @Test
    public void test() {
        // Redis Stack 连接配置
        String host = "192.168.80.128";
        int port = 63790;
        String password = "111222";

        // 创建 Redis 连接 —— 密码嵌入 URI
        try (JedisPooled jedis = new JedisPooled("redis://:" + password + "@" + host + ":" + port)) {
            // 测试连接是否成功
            String pong = jedis.ping();
            System.out.println("✅ Redis Stack 连接成功！Ping 返回: " + pong);

            // 测试写入和读取数据
            jedis.set("test:key", "Hello Redis Stack!");
            String value = jedis.get("test:key");
            System.out.println("✅ 写入并读取数据成功: " + value);

            // 清理测试数据
            jedis.del("test:key");
            System.out.println("✅ 测试数据已清理");

        } catch (Exception e) {
            System.err.println("❌ Redis Stack 连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
