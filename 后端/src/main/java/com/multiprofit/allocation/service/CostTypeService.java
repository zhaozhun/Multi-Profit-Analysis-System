package com.multiprofit.allocation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.multiprofit.allocation.mapper.*;
import com.multiprofit.allocation.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 费用类型管理服务
 */
@Slf4j
@Service
public class CostTypeService {

    @Autowired
    private CostTypeMasterMapper costTypeMapper;

    @Autowired
    private CostAllocationRuleConfigMapper ruleConfigMapper;

    @Autowired
    private CostActualRecordMapper actualRecordMapper;

    /**
     * 获取所有费用类型
     */
    public List<CostTypeMaster> getAllCostTypes() {
        return costTypeMapper.selectList(
            new LambdaQueryWrapper<CostTypeMaster>()
                .eq(CostTypeMaster::getStatus, "ACTIVE")
                .orderByAsc(CostTypeMaster::getLevel)
                .orderByAsc(CostTypeMaster::getSortOrder)
                .orderByAsc(CostTypeMaster::getCostCode)
        );
    }

    /**
     * 获取费用类型层级结构
     */
    public List<Map<String, Object>> getCostTypeHierarchy() {
        List<CostTypeMaster> allTypes = getAllCostTypes();

        // 按层级分组
        Map<Integer, List<CostTypeMaster>> levelMap = allTypes.stream()
            .collect(Collectors.groupingBy(CostTypeMaster::getLevel));

        // 构建层级结构
        List<Map<String, Object>> hierarchy = new ArrayList<>();
        List<CostTypeMaster> level1 = levelMap.getOrDefault(1, new ArrayList<>());

        for (CostTypeMaster l1 : level1) {
            Map<String, Object> l1Node = buildNode(l1);
            List<Map<String, Object>> l1Children = new ArrayList<>();

            // 查找二级
            List<CostTypeMaster> level2 = allTypes.stream()
                .filter(t -> l1.getCostCode().equals(t.getParentCode()))
                .collect(Collectors.toList());

            for (CostTypeMaster l2 : level2) {
                Map<String, Object> l2Node = buildNode(l2);
                List<Map<String, Object>> l2Children = new ArrayList<>();

                // 查找三级
                List<CostTypeMaster> level3 = allTypes.stream()
                    .filter(t -> l2.getCostCode().equals(t.getParentCode()))
                    .collect(Collectors.toList());

                for (CostTypeMaster l3 : level3) {
                    l2Children.add(buildNode(l3));
                }

                if (!l2Children.isEmpty()) {
                    l2Node.put("children", l2Children);
                }
                l1Children.add(l2Node);
            }

            if (!l1Children.isEmpty()) {
                l1Node.put("children", l1Children);
            }
            hierarchy.add(l1Node);
        }

        return hierarchy;
    }

    /**
     * 获取指定层级的费用类型
     */
    public List<CostTypeMaster> getCostTypesByLevel(Integer level) {
        return costTypeMapper.selectList(
            new LambdaQueryWrapper<CostTypeMaster>()
                .eq(CostTypeMaster::getLevel, level)
                .eq(CostTypeMaster::getStatus, "ACTIVE")
                .orderByAsc(CostTypeMaster::getSortOrder)
                .orderByAsc(CostTypeMaster::getCostCode)
        );
    }

    /**
     * 获取指定大类下的费用类型
     */
    public List<CostTypeMaster> getCostTypesByParent(String parentCode) {
        return costTypeMapper.selectList(
            new LambdaQueryWrapper<CostTypeMaster>()
                .eq(CostTypeMaster::getParentCode, parentCode)
                .eq(CostTypeMaster::getStatus, "ACTIVE")
                .orderByAsc(CostTypeMaster::getSortOrder)
                .orderByAsc(CostTypeMaster::getCostCode)
        );
    }

    /**
     * 获取需要分摊的费用类型
     */
    public List<CostTypeMaster> getAllocationRequiredTypes() {
        return costTypeMapper.selectList(
            new LambdaQueryWrapper<CostTypeMaster>()
                .eq(CostTypeMaster::getAllocationRequired, true)
                .eq(CostTypeMaster::getStatus, "ACTIVE")
                .orderByAsc(CostTypeMaster::getLevel)
                .orderByAsc(CostTypeMaster::getSortOrder)
        );
    }

    /**
     * 根据费用性质获取费用类型
     */
    public List<CostTypeMaster> getCostTypesByNature(String costNature) {
        return costTypeMapper.selectList(
            new LambdaQueryWrapper<CostTypeMaster>()
                .eq(CostTypeMaster::getCostNature, costNature)
                .eq(CostTypeMaster::getStatus, "ACTIVE")
                .orderByAsc(CostTypeMaster::getLevel)
                .orderByAsc(CostTypeMaster::getSortOrder)
        );
    }

    /**
     * 根据费用性质获取费用类型（FIXED/VARIABLE/DIRECT）
     */
    public List<CostTypeMaster> getCostTypesByCategory(String costCategory) {
        return costTypeMapper.selectList(
            new LambdaQueryWrapper<CostTypeMaster>()
                .eq(CostTypeMaster::getCostCategory, costCategory)
                .eq(CostTypeMaster::getStatus, "ACTIVE")
                .orderByAsc(CostTypeMaster::getLevel)
                .orderByAsc(CostTypeMaster::getSortOrder)
        );
    }

    /**
     * 获取费用类型详情
     */
    public CostTypeMaster getCostType(String costCode) {
        return costTypeMapper.selectOne(
            new LambdaQueryWrapper<CostTypeMaster>()
                .eq(CostTypeMaster::getCostCode, costCode)
        );
    }

    /**
     * 创建费用类型
     */
    @Transactional
    public CostTypeMaster createCostType(CostTypeMaster costType) {
        // 检查编码唯一性
        CostTypeMaster existing = getCostType(costType.getCostCode());
        if (existing != null) {
            throw new RuntimeException("费用编码已存在: " + costType.getCostCode());
        }

        // 校验父级编码
        if (costType.getParentCode() != null && costType.getLevel() > 1) {
            CostTypeMaster parent = getCostType(costType.getParentCode());
            if (parent == null) {
                throw new RuntimeException("父级费用类型不存在: " + costType.getParentCode());
            }
            if (parent.getLevel() >= costType.getLevel()) {
                throw new RuntimeException("父级层级必须小于当前层级");
            }
        }

        costType.setStatus("ACTIVE");
        costTypeMapper.insert(costType);

        log.info("创建费用类型: {} - {}", costType.getCostCode(), costType.getCostName());
        return costType;
    }

    /**
     * 更新费用类型
     */
    @Transactional
    public CostTypeMaster updateCostType(String costCode, CostTypeMaster costType) {
        CostTypeMaster existing = getCostType(costCode);
        if (existing == null) {
            throw new RuntimeException("费用类型不存在: " + costCode);
        }

        costType.setId(existing.getId());
        costType.setCostCode(costCode); // 编码不可修改
        costTypeMapper.updateById(costType);

        log.info("更新费用类型: {}", costCode);
        return costType;
    }

    /**
     * 删除费用类型
     */
    @Transactional
    public void deleteCostType(String costCode) {
        CostTypeMaster existing = getCostType(costCode);
        if (existing == null) {
            throw new RuntimeException("费用类型不存在: " + costCode);
        }

        // 检查是否有子类型
        Long childCount = costTypeMapper.selectCount(
            new LambdaQueryWrapper<CostTypeMaster>()
                .eq(CostTypeMaster::getParentCode, costCode)
        );
        if (childCount > 0) {
            throw new RuntimeException("该费用类型下有子类型，无法删除");
        }

        // 检查是否有关联的费用记录
        Long recordCount = actualRecordMapper.selectCount(
            new LambdaQueryWrapper<CostActualRecord>()
                .eq(CostActualRecord::getCostCode, costCode)
        );
        if (recordCount > 0) {
            throw new RuntimeException("该费用类型已有费用记录，无法删除");
        }

        // 逻辑删除
        existing.setStatus("INACTIVE");
        costTypeMapper.updateById(existing);

        log.info("删除费用类型: {}", costCode);
    }

    /**
     * 获取费用分摊规则
     */
    public List<CostAllocationRuleConfig> getAllocationRules() {
        return ruleConfigMapper.selectList(
            new LambdaQueryWrapper<CostAllocationRuleConfig>()
                .eq(CostAllocationRuleConfig::getStatus, "ACTIVE")
                .orderByAsc(CostAllocationRuleConfig::getCostCode)
        );
    }

    /**
     * 获取费用分摊规则详情
     */
    public CostAllocationRuleConfig getAllocationRule(String costCode) {
        return ruleConfigMapper.selectOne(
            new LambdaQueryWrapper<CostAllocationRuleConfig>()
                .eq(CostAllocationRuleConfig::getCostCode, costCode)
        );
    }

    /**
     * 更新费用分摊规则
     */
    @Transactional
    public CostAllocationRuleConfig updateAllocationRule(String costCode, CostAllocationRuleConfig rule) {
        CostAllocationRuleConfig existing = getAllocationRule(costCode);
        if (existing == null) {
            throw new RuntimeException("分摊规则不存在: " + costCode);
        }

        rule.setId(existing.getId());
        rule.setCostCode(costCode);
        ruleConfigMapper.updateById(rule);

        log.info("更新费用分摊规则: {}", costCode);
        return rule;
    }

    /**
     * 创建费用实际记录
     */
    @Transactional
    public CostActualRecord createActualRecord(CostActualRecord record) {
        // 校验费用类型存在
        CostTypeMaster costType = getCostType(record.getCostCode());
        if (costType == null) {
            throw new RuntimeException("费用类型不存在: " + record.getCostCode());
        }

        record.setCostName(costType.getCostName());
        record.setCostType(getParentLevel1Code(costType));
        record.setStatus("PENDING");
        actualRecordMapper.insert(record);

        log.info("创建费用记录: {} - {} - {}", record.getPeriod(), record.getCostCode(), record.getAmount());
        return record;
    }

    /**
     * 获取费用实际记录
     */
    public List<CostActualRecord> getActualRecords(String period, String costCode, String costType) {
        LambdaQueryWrapper<CostActualRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CostActualRecord::getPeriod, period);
        if (costCode != null) {
            wrapper.eq(CostActualRecord::getCostCode, costCode);
        }
        if (costType != null) {
            wrapper.eq(CostActualRecord::getCostType, costType);
        }
        wrapper.orderByAsc(CostActualRecord::getCostCode);
        return actualRecordMapper.selectList(wrapper);
    }

    /**
     * 获取费用汇总统计
     */
    public List<Map<String, Object>> getCostSummary(String period) {
        List<CostActualRecord> records = getActualRecords(period, null, null);
        List<CostTypeMaster> allTypes = getAllCostTypes();
        Map<String, CostTypeMaster> typeMap = allTypes.stream()
            .collect(Collectors.toMap(CostTypeMaster::getCostCode, t -> t));

        // 按费用类型汇总
        Map<String, BigDecimal> summary = new LinkedHashMap<>();
        Map<String, String> typeNames = new LinkedHashMap<>();

        for (CostActualRecord record : records) {
            summary.merge(record.getCostCode(), record.getAmount(), BigDecimal::add);
            typeNames.putIfAbsent(record.getCostCode(), record.getCostName());
        }

        return summary.entrySet().stream()
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                CostTypeMaster type = typeMap.get(entry.getKey());
                item.put("costCode", entry.getKey());
                item.put("costName", typeNames.get(entry.getKey()));
                item.put("totalAmount", entry.getValue());
                if (type != null) {
                    item.put("level", type.getLevel());
                    item.put("costCategory", type.getCostCategory());
                    item.put("costNature", type.getCostNature());
                    item.put("allocationRequired", type.getAllocationRequired());
                }
                return item;
            })
            .sorted((a, b) -> {
                // 按层级、性质、金额排序
                int levelCompare = ((Integer) a.get("level")).compareTo((Integer) b.get("level"));
                if (levelCompare != 0) return levelCompare;
                return ((BigDecimal) b.get("totalAmount")).compareTo((BigDecimal) a.get("totalAmount"));
            })
            .collect(Collectors.toList());
    }

    /**
     * 按费用性质汇总
     */
    public Map<String, BigDecimal> getSummaryByNature(String period) {
        List<CostActualRecord> records = getActualRecords(period, null, null);
        List<CostTypeMaster> allTypes = getAllCostTypes();
        Map<String, CostTypeMaster> typeMap = allTypes.stream()
            .collect(Collectors.toMap(CostTypeMaster::getCostCode, t -> t));

        Map<String, BigDecimal> summary = new LinkedHashMap<>();

        for (CostActualRecord record : records) {
            CostTypeMaster type = typeMap.get(record.getCostCode());
            if (type != null) {
                String nature = type.getCostNature();
                summary.merge(nature, record.getAmount(), BigDecimal::add);
            }
        }

        return summary;
    }

    /**
     * 按费用性质（固定/变动/直接）汇总
     */
    public Map<String, BigDecimal> getSummaryByCategory(String period) {
        List<CostActualRecord> records = getActualRecords(period, null, null);
        List<CostTypeMaster> allTypes = getAllCostTypes();
        Map<String, CostTypeMaster> typeMap = allTypes.stream()
            .collect(Collectors.toMap(CostTypeMaster::getCostCode, t -> t));

        Map<String, BigDecimal> summary = new LinkedHashMap<>();

        for (CostActualRecord record : records) {
            CostTypeMaster type = typeMap.get(record.getCostCode());
            if (type != null) {
                String category = type.getCostCategory();
                summary.merge(category, record.getAmount(), BigDecimal::add);
            }
        }

        return summary;
    }

    // ========== 私有方法 ==========

    /**
     * 构建节点
     */
    private Map<String, Object> buildNode(CostTypeMaster type) {
        Map<String, Object> node = new HashMap<>();
        node.put("costCode", type.getCostCode());
        node.put("costName", type.getCostName());
        node.put("level", type.getLevel());
        node.put("costCategory", type.getCostCategory());
        node.put("costNature", type.getCostNature());
        node.put("allocationRequired", type.getAllocationRequired());
        node.put("allocationMethod", type.getAllocationMethod());
        node.put("description", type.getDescription());
        return node;
    }

    /**
     * 获取一级父级编码
     */
    private String getParentLevel1Code(CostTypeMaster type) {
        if (type.getLevel() == 1) {
            return type.getCostCode();
        }
        CostTypeMaster parent = getCostType(type.getParentCode());
        if (parent == null) {
            return type.getCostCode();
        }
        return getParentLevel1Code(parent);
    }
}
