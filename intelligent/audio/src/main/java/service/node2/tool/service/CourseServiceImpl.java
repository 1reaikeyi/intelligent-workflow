package service.node2.tool.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.stereotype.Service;
import service.node2.tool.entity.Course;
import service.node2.tool.mapper.CourseMapper;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {
}