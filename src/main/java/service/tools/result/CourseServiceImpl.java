package service.tools.result;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import mapper.CourseMapper;
import org.springframework.stereotype.Service;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {
}