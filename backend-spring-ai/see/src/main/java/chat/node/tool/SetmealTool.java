package chat.node.tool;

import chat.node.tool.entity.Setmeal;
import chat.node.tool.mapper.SetmealMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SetmealTool {

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 按套餐ID精确查询
     * @param id 套餐ID
     * @return 单个套餐信息（无结果返回null）
     */
    @Tool(description = "按套餐ID精确查询，返回单个套餐信息")
    public Setmeal queryById(@ToolParam(description = "套餐ID（必填，精确匹配）") Long id) {
        if (id == null) {
            return null;
        }
        return setmealMapper.selectById(id);
    }

    /**
     * 按套餐名称模糊查询
     * @param name 套餐名称关键词
     * @return 符合条件的套餐列表
     */
    @Tool(description = "按套餐名称模糊查询，支持关键词匹配（如传入'宫保鸡丁'会匹配所有名称含宫保鸡丁的套餐）")
    public List<Setmeal> queryByName(@ToolParam(description = "套餐名称关键词") String name) {
        LambdaQueryWrapper<Setmeal> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(Setmeal::getName, name);
        }
        return setmealMapper.selectList(wrapper);
    }

    /**
     * 按分类ID精确查询
     * @param categoryId 分类ID
     * @return 符合分类的套餐列表
     */
    @Tool(description = "按分类ID精确查询，返回该分类下的所有套餐")
    public List<Setmeal> queryByCategoryId(@ToolParam(description = "分类ID（必填，精确匹配）") Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        LambdaQueryWrapper<Setmeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Setmeal::getCategoryId, categoryId);
        return setmealMapper.selectList(wrapper);
    }

    /**
     * 按价格区间查询
     * @param minPrice 最低价格（非必填）
     * @param maxPrice 最高价格（非必填）
     * @return 符合价格区间的套餐列表
     */
    @Tool(description = "按套餐价格区间查询，支持仅传最低价格/仅传最高价格/同时传高低价格")
    public List<Setmeal> queryByPriceRange(
            @ToolParam(description = "最低价格（非必填，>= 该价格）")
            java.math.BigDecimal minPrice,
            @ToolParam(description = "最高价格（非必填，<= 该价格）")
            java.math.BigDecimal maxPrice
    ) {
        LambdaQueryWrapper<Setmeal> wrapper = new LambdaQueryWrapper<>();
        if (minPrice != null) {
            wrapper.ge(Setmeal::getPrice, minPrice);
        }
        if (maxPrice != null) {
            wrapper.le(Setmeal::getPrice, maxPrice);
        }
        return setmealMapper.selectList(wrapper);
    }

    /**
     * 按售卖状态精确查询
     * @param status 状态：0-停售, 1-起售
     * @return 符合状态的套餐列表
     */
    @Tool(description = "按售卖状态精确查询，状态值：0-停售, 1-起售")
    public List<Setmeal> queryByStatus(@ToolParam(description = "售卖状态（必填，0-停售, 1-起售）") Long status) {
        LambdaQueryWrapper<Setmeal> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Setmeal::getStatus, status);
        }
        return setmealMapper.selectList(wrapper);
    }

    /**
     * 根据图片识别的食材/饮品关键词模糊查询套餐
     * 匹配套餐表description菜品描述字段，支持鱼、虾、鸡、啤酒、凉茶等食材关键词检索
     *  图片识别提取的食材、饮料关键词（示例：鱼、大虾、牛蛙、啤酒、烤鱼、辣椒等）
     * @return 匹配描述包含关键词的套餐列表
     */
    @Tool(description = "根据图片识别出的食材、饮品关键词，模糊匹配套餐的菜品描述字段，检索对应套餐")
    public List<Setmeal> queryByDescription(@ToolParam(description = "图片识别得到的食物、饮料关键词，例如：鱼、虾、牛蛙、啤酒、烤鱼、辣椒、豆腐等")
                                                SetmealToolParam setmealToolParam) {
        LambdaQueryWrapper<Setmeal> wrapper = new LambdaQueryWrapper<>();
        // 去除前后空格，防止空字符串、纯空格查询
        String key = setmealToolParam.getDescription() == null ? "" : setmealToolParam.getDescription().trim();
        // 非空才执行模糊匹配，避免全表无筛选查询
        if (!key.isEmpty()) {
            // 模糊匹配description菜品描述字段，包含关键词即命中
            wrapper.like(Setmeal::getDescription, key);
        }
        wrapper.like(Setmeal::getName, key);

        return setmealMapper.selectList(wrapper);
    }


    /**
     * 多条件组合查询
     * @param setmealToolParam 组合查询条件
     * @return 符合所有条件的套餐列表
     */
    @Tool(description = "套餐多条件组合查询，支持ID/分类ID/名称/价格/状态/描述/时间的任意组合")
    public List<Setmeal> queryByMultiCondition(
            @ToolParam(description = "组合查询条件")
            SetmealToolParam setmealToolParam
    ) {
        try {
            LambdaQueryWrapper<Setmeal> wrapper = new LambdaQueryWrapper<>();
            // ID精确查询
            if (setmealToolParam.getId() != null) {
                wrapper.eq(Setmeal::getId, setmealToolParam.getId());
            }
            // 分类ID精确查询
            if (setmealToolParam.getCategoryId() != null) {
                wrapper.eq(Setmeal::getCategoryId, setmealToolParam.getCategoryId());
            }
            // 名称模糊查询
            if (setmealToolParam.getName() != null && !setmealToolParam.getName().isEmpty()) {
                wrapper.like(Setmeal::getName, setmealToolParam.getName());
            }
            // 价格区间查询
            if (setmealToolParam.getMinPrice() != null) {
                wrapper.ge(Setmeal::getPrice, setmealToolParam.getMinPrice());
            }
            if (setmealToolParam.getMaxPrice() != null) {
                wrapper.le(Setmeal::getPrice, setmealToolParam.getMaxPrice());
            }
            // 状态精确查询
            if (setmealToolParam.getStatus() != null) {
                wrapper.eq(Setmeal::getStatus, setmealToolParam.getStatus());
            }
            // 描述模糊查询
            if (setmealToolParam.getDescription() != null && !setmealToolParam.getDescription().isEmpty()) {
                wrapper.like(Setmeal::getDescription, setmealToolParam.getDescription());
            }
            return setmealMapper.selectList(wrapper);
        } catch (Exception e) {
            System.err.println("数据库查询异常：" + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("数据库查询失败：" + e.getMessage(), e);
        }
    }
}