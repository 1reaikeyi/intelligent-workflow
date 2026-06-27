package start;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.net.UnknownHostException;

// 日志配置
@Slf4j
//主程序入口
@SpringBootApplication
@ComponentScan(basePackages = {"start","config","node"})
public class FlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlowApplication.class, args);
        log.info("开启成功");
    }
}