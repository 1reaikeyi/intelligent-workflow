package service.tools;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.NumberUtil;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import service.tools.result.Course;

import java.util.Optional;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseInfo {

    @JsonPropertyDescription("课程id")
    private Long id;
    @JsonPropertyDescription("课程名称")
    private String name;
    @JsonPropertyDescription("课程价格，单位为元，货币为人民币")
    private double price;
    @JsonPropertyDescription("课程学习有效期，单位：月")
    private Integer validDuration;
    @JsonPropertyDescription("适用人群，例如：初学者")
    private String usePeople;
    @JsonPropertyDescription("课程详细介绍")
    private String detail;

    public static CourseInfo of(Course course) {
        if (null == course) {
            throw new IllegalArgumentException("没有查询到数据");
        }
        return CourseInfo.builder()
                .id(course.getId())
                .name(course.getName())
                .price(course.getPrice())
                .validDuration(course.getValidDuration())
                .usePeople(course.getUsePeople())
                .detail(course.getDetail())
                .build();
    }

}
