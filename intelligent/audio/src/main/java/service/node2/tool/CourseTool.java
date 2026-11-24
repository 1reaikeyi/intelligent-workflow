package service.node2.tool;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import service.node2.tool.entity.Course;
import service.node2.tool.service.CourseService;

import java.util.Optional;

@Service
@Slf4j
public class CourseTool {
    @Autowired
    private CourseService courseService;

    private static final  String COURSE_ID = "课程id";
    @Tool(description = "根据课程id查询课程详细信息")
    public Course queryCourseById(@ToolParam(description = COURSE_ID) Long courseId) {
        return courseService.getById(courseId);
    }
}