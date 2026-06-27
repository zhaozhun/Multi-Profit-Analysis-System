package com.multiprofit.allocation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.multiprofit.allocation.mapper.*;
import com.multiprofit.allocation.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 分摊配置管理服务
 */
@Slf4j
@Service
public class AllocationConfigService {

    @Autowired
    private CostTypeConfigMapper costTypeMapper;

    @Autowired
    private AllocationFactorConfigMapper factorMapper;

    @Autowired
    private AllocationAlgorithmConfigMapper algorithmMapper;

    @Autowired
    private AllocationRuleConfigMapper ruleMapper;

    @Autowired
    private AllocationFactorWeightMapper weightMapper;

    // ========== 成本类型管理 ==========

    /**
     * 查询所有成本类型
     */
    public List<CostTypeConfig> listCostTypes(String status) {
        if (status != null) {
            return costTypeMapper.selectByStatus(status);
        }
        return costTypeMapper.selectAllActive();
    }

    /**
     * 获取成本类型详情
     */
    // @Cacheable(value = "allocation:costType", key = "#code")
    public CostTypeConfig getCostType(String code) {
        return costTypeMapper.selectOne(
            new LambdaQueryWrapper<CostTypeConfig>()
                .eq(CostTypeConfig::getCostTypeCode, code)
        );
    }

    /**
     * 创建成本类型
     */
    @Transactional
    // @CacheEvict(value = "allocation:costType", allEntries = true)
    public CostTypeConfig createCostType(CostTypeConfig costType) {
        // 检查编码唯一性
        CostTypeConfig existing = getCostType(costType.getCostTypeCode());
        if (existing != null) {
            throw new RuntimeException("成本类型编码已存在: " + costType.getCostTypeCode());
        }

        costType.setStatus("ACTIVE");
        costTypeMapper.insert(costType);
        log.info("创建成本类型: {} - {}", costType.getCostTypeCode(), costType.getCostTypeName());
        return costType;
    }

    /**
     * 更新成本类型
     */
    @Transactional
    // @CacheEvict(value = "allocation:costType", allEntries = true)
    public CostTypeConfig updateCostType(String code, CostTypeConfig costType) {
        CostTypeConfig existing = getCostType(code);
        if (existing == null) {
            throw new RuntimeException("成本类型不存在: " + code);
        }

        costType.setId(existing.getId());
        costType.setCostTypeCode(code); // 编码不可修改
        costTypeMapper.updateById(costType);
        log.info("更新成本类型: {}", code);
        return costType;
    }

    /**
     * 删除成本类型
     */
    @Transactional
    // @CacheEvict(value = "allocation:costType", allEntries = true)
    public void deleteCostType(String code) {
        // 检查是否被规则引用
        Long ruleCount = ruleMapper.selectCount(
            new LambdaQueryWrapper<AllocationRuleConfig>()
                .eq(AllocationRuleConfig::getCostType, code)
        );
        if (ruleCount > 0) {
            throw new RuntimeException("该成本类型已被分摊规则引用，无法删除");
        }

        costTypeMapper.delete(
            new LambdaQueryWrapper<CostTypeConfig>()
                .eq(CostTypeConfig::getCostTypeCode, code)
        );
        log.info("删除成本类型: {}", code);
    }

    // ========== 因子配置管理 ==========

    /**
     * 查询因子列表
     */
    public List<AllocationFactorConfig> listFactors(String factorType, String costType) {
        if (factorType != null) {
            return factorMapper.selectByFactorType(factorType);
        }
        if (costType != null) {
            return factorMapper.selectByCostType(costType);
        }
        return factorMapper.selectAllActive();
    }

    /**
     * 获取因子详情
     */
    // @Cacheable(value = "allocation:factor", key = "#code")
    public AllocationFactorConfig getFactor(String code) {
        return factorMapper.selectOne(
            new LambdaQueryWrapper<AllocationFactorConfig>()
                .eq(AllocationFactorConfig::getFactorCode, code)
        );
    }

    /**
     * 创建因子
     */
    @Transactional
    // @CacheEvict(value = "allocation:factor", allEntries = true)
    public AllocationFactorConfig createFactor(AllocationFactorConfig factor) {
        // 检查编码唯一性
        AllocationFactorConfig existing = getFactor(factor.getFactorCode());
        if (existing != null) {
            throw new RuntimeException("因子编码已存在: " + factor.getFactorCode());
        }

        factor.setStatus("ACTIVE");
        factorMapper.insert(factor);
        log.info("创建分摊因子: {} - {}", factor.getFactorCode(), factor.getFactorName());
        return factor;
    }

    /**
     * 更新因子
     */
    @Transactional
    // @CacheEvict(value = "allocation:factor", allEntries = true)
    public AllocationFactorConfig updateFactor(String code, AllocationFactorConfig factor) {
        AllocationFactorConfig existing = getFactor(code);
        if (existing == null) {
            throw new RuntimeException("因子不存在: " + code);
        }

        factor.setId(existing.getId());
        factor.setFactorCode(code); // 编码不可修改
        factorMapper.updateById(factor);
        log.info("更新分摊因子: {}", code);
        return factor;
    }

    /**
     * 删除因子
     */
    @Transactional
    // @CacheEvict(value = "allocation:factor", allEntries = true)
    public void deleteFactor(String code) {
        // 检查是否被权重配置引用
        Long weightCount = weightMapper.selectCount(
            new LambdaQueryWrapper<AllocationFactorWeight>()
                .eq(AllocationFactorWeight::getFactorCode, code)
        );
        if (weightCount > 0) {
            throw new RuntimeException("该因子已被规则权重配置引用，无法删除");
        }

        factorMapper.delete(
            new LambdaQueryWrapper<AllocationFactorConfig>()
                .eq(AllocationFactorConfig::getFactorCode, code)
        );
        log.info("删除分摊因子: {}", code);
    }

    // ========== 算法配置管理 ==========

    /**
     * 查询算法列表
     */
    public List<AllocationAlgorithmConfig> listAlgorithms(String algorithmType) {
        if (algorithmType != null) {
            return algorithmMapper.selectByAlgorithmType(algorithmType);
        }
        return algorithmMapper.selectAllActive();
    }

    /**
     * 获取算法详情
     */
    // @Cacheable(value = "allocation:algorithm", key = "#code")
    public AllocationAlgorithmConfig getAlgorithm(String code) {
        return algorithmMapper.selectOne(
            new LambdaQueryWrapper<AllocationAlgorithmConfig>()
                .eq(AllocationAlgorithmConfig::getAlgorithmCode, code)
        );
    }

    /**
     * 创建算法
     */
    @Transactional
    // @CacheEvict(value = "allocation:algorithm", allEntries = true)
    public AllocationAlgorithmConfig createAlgorithm(AllocationAlgorithmConfig algorithm) {
        // 检查编码唯一性
        AllocationAlgorithmConfig existing = getAlgorithm(algorithm.getAlgorithmCode());
        if (existing != null) {
            throw new RuntimeException("算法编码已存在: " + algorithm.getAlgorithmCode());
        }

        algorithm.setStatus("ACTIVE");
        algorithm.setIsBuiltin(false); // 用户创建的不是内置算法
        algorithmMapper.insert(algorithm);
        log.info("创建分摊算法: {} - {}", algorithm.getAlgorithmCode(), algorithm.getAlgorithmName());
        return algorithm;
    }

    /**
     * 更新算法
     */
    @Transactional
    // @CacheEvict(value = "allocation:algorithm", allEntries = true)
    public AllocationAlgorithmConfig updateAlgorithm(String code, AllocationAlgorithmConfig algorithm) {
        AllocationAlgorithmConfig existing = getAlgorithm(code);
        if (existing == null) {
            throw new RuntimeException("算法不存在: " + code);
        }

        // 内置算法不允许修改
        if (existing.getIsBuiltin()) {
            throw new RuntimeException("内置算法不允许修改");
        }

        algorithm.setId(existing.getId());
        algorithm.setAlgorithmCode(code);
        algorithmMapper.updateById(algorithm);
        log.info("更新分摊算法: {}", code);
        return algorithm;
    }

    /**
     * 删除算法
     */
    @Transactional
    // @CacheEvict(value = "allocation:algorithm", allEntries = true)
    public void deleteAlgorithm(String code) {
        AllocationAlgorithmConfig existing = getAlgorithm(code);
        if (existing == null) {
            throw new RuntimeException("算法不存在: " + code);
        }

        // 内置算法不允许删除
        if (existing.getIsBuiltin()) {
            throw new RuntimeException("内置算法不允许删除");
        }

        // 检查是否被规则引用
        Long ruleCount = ruleMapper.selectCount(
            new LambdaQueryWrapper<AllocationRuleConfig>()
                .eq(AllocationRuleConfig::getAlgorithmCode, code)
        );
        if (ruleCount > 0) {
            throw new RuntimeException("该算法已被分摊规则引用，无法删除");
        }

        algorithmMapper.delete(
            new LambdaQueryWrapper<AllocationAlgorithmConfig>()
                .eq(AllocationAlgorithmConfig::getAlgorithmCode, code)
        );
        log.info("删除分摊算法: {}", code);
    }

    // ========== 规则配置管理 ==========

    /**
     * 查询规则列表
     */
    public List<AllocationRuleConfig> listRules(String costType, String status) {
        if (costType != null) {
            return ruleMapper.selectByCostType(costType);
        }
        if (status != null) {
            return ruleMapper.selectByStatus(status);
        }
        return ruleMapper.selectAllActive();
    }

    /**
     * 查询有效规则（指定日期）
     */
    public List<AllocationRuleConfig> listEffectiveRules(LocalDate date) {
        return ruleMapper.selectEffectiveRules(date);
    }

    /**
     * 查询自动执行的规则
     */
    public List<AllocationRuleConfig> listAutoExecuteRules() {
        return ruleMapper.selectAutoExecuteRules();
    }

    /**
     * 获取规则详情（包含权重配置）
     */
    // @Cacheable(value = "allocation:rule", key = "#id")
    public AllocationRuleConfig getRule(Long id) {
        return ruleMapper.selectById(id);
    }

    /**
     * 根据编码获取规则
     */
    // @Cacheable(value = "allocation:rule", key = "#code")
    public AllocationRuleConfig getRuleByCode(String code) {
        return ruleMapper.selectOne(
            new LambdaQueryWrapper<AllocationRuleConfig>()
                .eq(AllocationRuleConfig::getRuleCode, code)
        );
    }

    /**
     * 获取规则的权重配置
     */
    public List<AllocationFactorWeight> getRuleWeights(Long ruleId) {
        return weightMapper.selectByRuleId(ruleId);
    }

    /**
     * 创建规则
     */
    @Transactional
    // @CacheEvict(value = "allocation:rule", allEntries = true)
    public AllocationRuleConfig createRule(AllocationRuleConfig rule, List<AllocationFactorWeight> weights) {
        // 检查编码唯一性
        AllocationRuleConfig existing = getRuleByCode(rule.getRuleCode());
        if (existing != null) {
            throw new RuntimeException("规则编码已存在: " + rule.getRuleCode());
        }

        // 校验成本类型存在
        CostTypeConfig costType = getCostType(rule.getCostType());
        if (costType == null) {
            throw new RuntimeException("成本类型不存在: " + rule.getCostType());
        }

        // 校验算法存在
        AllocationAlgorithmConfig algorithm = getAlgorithm(rule.getAlgorithmCode());
        if (algorithm == null) {
            throw new RuntimeException("分摊算法不存在: " + rule.getAlgorithmCode());
        }

        // 设置默认值
        rule.setStatus(rule.getStatus() != null ? rule.getStatus() : "DRAFT");
        rule.setVersion(1);

        // 插入规则
        ruleMapper.insert(rule);
        log.info("创建分摊规则: {} - {}", rule.getRuleCode(), rule.getRuleName());

        // 保存权重配置
        if (weights != null && !weights.isEmpty()) {
            for (AllocationFactorWeight weight : weights) {
                weight.setRuleId(rule.getId());
                weightMapper.insert(weight);
            }
            log.info("保存规则权重配置: {} 条", weights.size());
        }

        return rule;
    }

    /**
     * 更新规则
     */
    @Transactional
    // @CacheEvict(value = "allocation:rule", allEntries = true)
    public AllocationRuleConfig updateRule(Long id, AllocationRuleConfig rule, List<AllocationFactorWeight> weights) {
        AllocationRuleConfig existing = getRule(id);
        if (existing == null) {
            throw new RuntimeException("规则不存在: " + id);
        }

        // 更新规则
        rule.setId(id);
        rule.setRuleCode(existing.getRuleCode()); // 编码不可修改
        rule.setVersion(existing.getVersion() + 1); // 版本号+1
        ruleMapper.updateById(rule);
        log.info("更新分摊规则: {}", id);

        // 更新权重配置
        if (weights != null) {
            // 删除旧权重
            weightMapper.deleteByRuleId(id);
            // 插入新权重
            for (AllocationFactorWeight weight : weights) {
                weight.setRuleId(id);
                weightMapper.insert(weight);
            }
            log.info("更新规则权重配置: {} 条", weights.size());
        }

        return rule;
    }

    /**
     * 删除规则
     */
    @Transactional
    // @CacheEvict(value = "allocation:rule", allEntries = true)
    public void deleteRule(Long id) {
        AllocationRuleConfig rule = getRule(id);
        if (rule == null) {
            throw new RuntimeException("规则不存在: " + id);
        }

        // 删除权重配置
        weightMapper.deleteByRuleId(id);
        // 删除规则
        ruleMapper.deleteById(id);
        log.info("删除分摊规则: {}", id);
    }

    /**
     * 启用规则
     */
    @Transactional
    // @CacheEvict(value = "allocation:rule", allEntries = true)
    public void enableRule(Long id) {
        AllocationRuleConfig rule = getRule(id);
        if (rule == null) {
            throw new RuntimeException("规则不存在: " + id);
        }

        rule.setStatus("ACTIVE");
        ruleMapper.updateById(rule);
        log.info("启用分摊规则: {}", id);
    }

    /**
     * 禁用规则
     */
    @Transactional
    // @CacheEvict(value = "allocation:rule", allEntries = true)
    public void disableRule(Long id) {
        AllocationRuleConfig rule = getRule(id);
        if (rule == null) {
            throw new RuntimeException("规则不存在: " + id);
        }

        rule.setStatus("INACTIVE");
        ruleMapper.updateById(rule);
        log.info("禁用分摊规则: {}", id);
    }
}
