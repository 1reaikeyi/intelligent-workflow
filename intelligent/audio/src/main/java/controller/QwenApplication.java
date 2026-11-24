package controller;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"service","config","controller","interceptor"})
@MapperScan("service.node2")
public class QwenApplication {

    public static void main(String[] args) {
        SpringApplication.run(QwenApplication.class, args);
    }

}
