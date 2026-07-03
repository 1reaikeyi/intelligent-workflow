package chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

// 日志配置
@Slf4j
//主程序入口
@SpringBootApplication
public class LanguageApplication {
    public static void main(String[] args) {
        SpringApplication.run(LanguageApplication.class, args);
        log.info("开启成功");
    }
}