package start;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("node.tool")
public class QwenApplication {

    public static void main(String[] args) {
        SpringApplication.run(QwenApplication.class, args);
    }

}
