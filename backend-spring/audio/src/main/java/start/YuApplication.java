package start; // 注意：包不要写错了

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;


import java.net.UnknownHostException;

// 日志配置
@Slf4j
//主程序入口
@SpringBootApplication
@ComponentScan({"audio","start"})
public class YuApplication {
    public static void main(String[] args) throws UnknownHostException {
        SpringApplication.run(YuApplication.class, args);
        log.info("开启成功");
    }
}