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
@ComponentScan(basePackages = {"start","config","node","audio"})
public class FlowApplication {
//    sk-a9bf52d2beea4c028213126b2ef492fc
    public static void main(String[] args) throws UnknownHostException {
        SpringApplication.run(FlowApplication.class, args);
        log.info("开启成功");
    }
}