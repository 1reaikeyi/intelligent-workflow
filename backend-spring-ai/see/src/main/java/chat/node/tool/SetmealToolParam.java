package chat.node.tool;

import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;

import java.math.BigDecimal;

@Data
public class SetmealToolParam {
    /**
     * 套餐ID
     */
    @ToolParam(required = false, description = "精确指定套餐主键ID，用于精准定位单套套餐")
    private Long id;

    /**
     * 分类ID（菜系/单人/酒水分类id）
     */
    @ToolParam(required = false, description = "套餐分类ID，区分菜系、单人餐、酒水饮品等类目")
    private Long categoryId;

    /**
     * 套餐名称关键词
     */
    @ToolParam(required = false, description = "套餐名称模糊关键词，可传入图片识别出的菜品名（烤鱼、牛蛙、大虾等）匹配套餐名称")
    private String name;

    /**
     * 最低价格（价格区间查询-左边界）
     */
    @ToolParam(required = false, description = "筛选套餐最低售价，用于价格区间过滤")
    private BigDecimal minPrice;

    /**
     * 最高价格
     */
    @ToolParam(required = false, description = "筛选套餐最高售价，用于价格区间过滤")
    private BigDecimal maxPrice;

    /**
     * 售卖状态
     */
    @ToolParam(required = false, description = "套餐售卖状态：0=停售下架，1=正常上架售卖，仅查询在售套餐传1")
    private Integer status;

    /**
     * 菜品描述关键词（图片识别核心字段）
     */
    @ToolParam(required = false, description = "图片识别提取的食材、饮品关键词，用于模糊匹配套餐详情描述，示例：鱼、大虾、啤酒、牛蛙、辣椒、豆腐")
    private String description;
}