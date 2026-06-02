package service.tools;

import common.constants.Constant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import service.tools.result.Course;
import service.tools.result.CourseService;

import java.util.Optional;

@Service
public class CourseTools {
    private static final Logger log = LoggerFactory.getLogger(CourseTools.class);
    @Autowired
    private  CourseService courseService;

    @Tool(description = Constant.Tools.QUERY_COURSE_BY_ID)
    public CourseInfo queryCourseById(@ToolParam(description = Constant.ToolParams.COURSE_ID) Long courseId) {
        return Optional.ofNullable(courseId)
                .map(id -> courseService.getById(id))
                .map(CourseInfo::of)
                .orElse(null);
    }
}