package service.node2.tool;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import service.node2.tool.service.CourseService;

import java.util.Optional;

@Service
@Slf4j
public class CourseTool {
    @Autowired
    private CourseService courseService;
    private static final String FIELD_NAME_FORMAT = "{}_{}";  // 提取格式字符串常量
    private static final String QUERY_COURSE_BY_ID = "根据课程id查询课程详细信息";
    private static final  String COURSE_ID = "课程id";
    @Tool(description = QUERY_COURSE_BY_ID)
    public CourseBO queryCourseById(@ToolParam(description = COURSE_ID) Long courseId,
                                    ToolContext toolContext) {
        return Optional.ofNullable(courseId)
                .map(id -> courseService.getById(id))
                .map(CourseBO::of)
//                .map(courseBO -> {
//                    // 存储数据的字段名：使用Java原生String.format
//                    String className = CourseBO.class.getSimpleName();
//                    // 将首字母转为小写
//                    String lowerClassName = className.isEmpty() ? className
//                            : Character.toLowerCase(className.charAt(0)) + className.substring(1);
//                    String field = String.format(FIELD_NAME_FORMAT, lowerClassName, courseBO.getId());
//                    // 存储的key
//                    Object requestIdObj = toolContext.getContext().get(REQUEST_ID);
//                    String requestId = requestIdObj != null ? String.valueOf(requestIdObj) : null;
//                    ToolResultHolder.put(requestId, field, courseBO);
//                    return courseBO;
//                })
                .orElse(null);
    }
}