package start; // 注意：包不要写错了

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.net.InetAddress;
import java.net.UnknownHostException;

// 日志配置
@Slf4j
//主程序入口
@SpringBootApplication
// 扫描bean组件
@ComponentScan(basePackages = {"common","service", "start"})
// 扫描mapper接口
@MapperScan(basePackages = {"mapper"})
// 开启事务管理
@EnableTransactionManagement
// 开启配置属性
@EnableConfigurationProperties
public class AIGCApplication {
    public static void main(String[] args) throws UnknownHostException {
        SpringApplication.run(AIGCApplication.class, args);
        log.info("开启成功");
    }
}