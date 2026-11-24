package start.config;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import start.service.Tool1;
import start.service.Tool2;

import java.util.List;

@Configuration
public class McpConfig {

    /**
     * 申明对外提供服务的工具
     */
    @Bean
    public List<ToolCallback> mcpTools(Tool1 tool1, Tool2 tool2) {
        return List.of(ToolCallbacks.from(tool1, tool2));
    }

}
