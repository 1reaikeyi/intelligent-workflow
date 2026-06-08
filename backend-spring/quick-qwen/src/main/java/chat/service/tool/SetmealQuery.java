package chat.service.tool;

import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;

@Data
public class SetmealQuery {

    /**
     * 套餐ID（精确查询）
     */
    @ToolParam(required = false, description = "套餐ID（精确查询）")
    private Long id;

    /**
     * 分类ID（精确查询）
     */
    @ToolParam(required = false, description = "分类ID（精确查询）")
    private Long categoryId;

    /**
     * 套餐名称（模糊查询）
     */
    @ToolParam(required = false, description = "套餐名称（支持模糊查询）")
    private String name;

    /**
     * 最低价格（价格区间查询-左边界）
     */
    @ToolParam(required = false, description = "套餐最低价格（价格区间查询）")
    private java.math.BigDecimal minPrice;

    /**
     * 最高价格（价格区间查询-右边界）
     */
    @ToolParam(required = false, description = "套餐最高价格（价格区间查询）")
    private java.math.BigDecimal maxPrice;

    /**
     * 售卖状态（精确查询：0-停售, 1-起售）
     */
    @ToolParam(required = false, description = "售卖状态（0-停售, 1-起售）")
    private Integer status;

    /**
     * 描述信息（模糊查询）
     */
    @ToolParam(required = false, description = "描述信息（支持模糊查询）")
    private String description;
}