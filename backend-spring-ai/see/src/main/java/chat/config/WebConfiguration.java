package chat.config;

import chat.Interceptor.SensitiveWordInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class WebConfiguration implements WebMvcConfigurer {
    @Autowired
    private SensitiveWordInterceptor sensitiveWordInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册敏感词拦截器，拦截AI相关接口
        registry.addInterceptor(sensitiveWordInterceptor).addPathPatterns("/rag", "/tool");
    }
}
