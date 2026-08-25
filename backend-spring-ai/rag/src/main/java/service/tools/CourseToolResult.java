package service.tools;

import lombok.extern.slf4j.Slf4j;
import start.constants.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import service.tools.result.CourseService;

import java.util.Optional;

@Service
@Slf4j
public class CourseToolResult {
    @Autowired
    private  CourseService courseService;
    private static final String FIELD_NAME_FORMAT = "{}_{}";  // 提取格式字符串常量

    @Tool(description = Constant.Tools.QUERY_COURSE_BY_ID)
    public CourseJson queryCourseById(@ToolParam(description = Constant.ToolParams.COURSE_ID) Long courseId,
                                      ToolContext toolContext) {
        return Optional.ofNullable(courseId)
                .map(id -> courseService.getById(id))
                .map(CourseJson::of)
                .map(courseJson -> {
                    // 存储数据的字段名：使用Java原生String.format
                    String className = CourseJson.class.getSimpleName();
                    // 将首字母转为小写
                    String lowerClassName = className.isEmpty() ? className 
                            : Character.toLowerCase(className.charAt(0)) + className.substring(1);
                    String field = String.format(FIELD_NAME_FORMAT, lowerClassName, courseJson.getId());
                    // 存储的key
                    Object requestIdObj = toolContext.getContext().get(Constant.REQUEST_ID);
                    String requestId = requestIdObj != null ? String.valueOf(requestIdObj) : null;
                    ToolResultHolder.put(requestId, field, courseJson);
                    return courseJson;
                })
                .orElse(null);
    }
}