package chat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("chat.service.tool")
public class QwenApplication {

    public static void main(String[] args) {
        SpringApplication.run(QwenApplication.class, args);
    }

}
