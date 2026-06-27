package chat.service.tool;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("setmeal")
public class Setmeal {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分类ID
     */
    @TableField(value = "category_id")
    private Long categoryId;

    /**
     * 套餐名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 套餐价格
     * 对应数据库decimal(10,2)类型，Java中使用BigDecimal保证精度
     */
    @TableField(value = "price")
    private BigDecimal price;

    /**
     * 售卖状态 0:停售 1:起售
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 描述信息
     */
    @TableField(value = "description")
    private String description;

    /**
     * 图片
     */
    @TableField(value = "image")
    private String image;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @TableField(value = "create_user")
    private Long createUser;

    /**
     * 修改人
     */
    @TableField(value = "update_user")
    private Long updateUser;
}