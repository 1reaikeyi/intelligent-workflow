package service.tools;

import lombok.extern.slf4j.Slf4j;
import model.bo.CourseBO;
import constants.Constant;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class CourseTool {
    @Autowired
    private  CourseService courseService;
    private static final String FIELD_NAME_FORMAT = "{}_{}";  // 提取格式字符串常量

    @Tool(description = Constant.Tools.QUERY_COURSE_BY_ID)
    public CourseBO queryCourseById(@ToolParam(description = Constant.ToolParams.COURSE_ID) Long courseId, ToolContext toolContext) {
        return Optional.ofNullable(courseId)
                .map(id -> courseService.getById(id))
                .map(CourseBO::of)
                .map(courseBO -> {
                    // 存储数据的字段名：使用Java原生String.format
                    String className = CourseBO.class.getSimpleName();
                    // 将首字母转为小写
                    String lowerClassName = className.isEmpty() ? className 
                            : Character.toLowerCase(className.charAt(0)) + className.substring(1);
                    String field = lowerClassName + "_" + courseBO.getId();
                    // 存储的key
                    Object requestIdObj = toolContext.getContext().get(Constant.REQUEST_ID);
                    String requestId = requestIdObj != null ? String.valueOf(requestIdObj) : null;
                    ToolResultHolder.put(requestId, field, courseBO);
                    return courseBO;
                })
                .orElse(null);
    }
}