package com.multiprofit.allocation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.multiprofit.allocation.mapper.*;
import com.multiprofit.allocation.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 产品分润服务
 * 计算产品相关的分润费用
 */
@Slf4j
@Service
public class ProductCommissionService {

    @Autowired
    private ProductMasterMapper productMapper;

    @Autowired
    private ProductCommissionRuleMapper ruleMapper;

    @Autowired
    private ProductCommissionDetailMapper detailMapper;

    /**
     * 执行产品分润计算
     * @param period 期间
     * @param productType 产品类型(可选)
     * @return 分润结果
     */
    @Transactional
    public CommissionResult executeCommission(String period, String productType) {
        log.info("开始产品分润计算: 期间={}, 产品类型={}", period, productType);

        // 1. 获取分润规则
        List<ProductCommissionRule> rules = getEffectiveRules(productType);
        if (rules.isEmpty()) {
            log.warn("未找到有效的分润规则");
            return CommissionResult.empty(period);
        }

        // 2. 获取产品列表
        List<ProductMaster> products = getActiveProducts(productType);

        // 3. 执行分润计算
        String batchNo = generateBatchNo();
        List<ProductCommissionDetail> details = new ArrayList<>();

        for (ProductCommissionRule rule : rules) {
            // 获取适用的产品列表
            List<ProductMaster> applicableProducts = getApplicableProducts(products, rule);

            for (ProductMaster product : applicableProducts) {
                // 获取计算基数
                BigDecimal baseAmount = getCalcBaseAmount(period, product.getProductCode(), rule.getCalcBase());
                if (baseAmount.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }

                // 计算分润金额
                BigDecimal commissionAmount = calculateCommission(baseAmount, rule);

                // 构建分润明细
                ProductCommissionDetail detail = new ProductCommissionDetail();
                detail.setBatchNo(batchNo);
                detail.setPeriod(period);
                detail.setProductCode(product.getProductCode());
                detail.setProductName(product.getProductName());
                detail.setRuleCode(rule.getRuleCode());
                detail.setCalcBase(rule.getCalcBase());
                detail.setBaseAmount(baseAmount);
                detail.setCommissionRate(rule.getRate());
                detail.setCommissionAmount(commissionAmount);
                detail.setReceiverType("PRODUCT");
                detail.setReceiverCode(product.getProductCode());
                detail.setReceiverName(product.getProductName());
                detail.setStatus("CALCULATED");

                details.add(detail);
            }
        }

        // 4. 批量保存分润明细
        for (ProductCommissionDetail detail : details) {
            detailMapper.insert(detail);
        }

        log.info("产品分润计算完成: 批次号={}, 产品数={}, 总分润={}",
            batchNo, details.size(),
            details.stream().map(ProductCommissionDetail::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 5. 返回结果
        return CommissionResult.builder()
            .batchNo(batchNo)
            .period(period)
            .ruleCount(rules.size())
            .productCount(details.stream()
                .map(ProductCommissionDetail::getProductCode)
                .distinct().count())
            .totalCommission(details.stream()
                .map(ProductCommissionDetail::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
            .details(details)
            .build();
    }

    /**
     * 按产品汇总分润
     */
    public List<Map<String, Object>> getProductCommissionSummary(String period) {
        List<ProductCommissionDetail> details = getCommissionDetails(period, null);

        // 按产品分组汇总
        Map<String, BigDecimal> summary = new LinkedHashMap<>();
        Map<String, ProductCommissionDetail> productInfo = new LinkedHashMap<>();

        for (ProductCommissionDetail detail : details) {
            String productCode = detail.getProductCode();
            summary.merge(productCode, detail.getCommissionAmount(), BigDecimal::add);
            productInfo.putIfAbsent(productCode, detail);
        }

        // 转换为列表
        return summary.entrySet().stream()
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                ProductCommissionDetail info = productInfo.get(entry.getKey());
                item.put("productCode", entry.getKey());
                item.put("productName", info.getProductName());
                item.put("totalCommission", entry.getValue());
                return item;
            })
            .sorted((a, b) -> ((BigDecimal) b.get("totalCommission")).compareTo((BigDecimal) a.get("totalCommission")))
            .collect(Collectors.toList());
    }

    /**
     * 按产品类型汇总分润
     */
    public List<Map<String, Object>> getProductTypeCommissionSummary(String period) {
        List<ProductCommissionDetail> details = getCommissionDetails(period, null);

        // 获取产品类型信息
        Map<String, String> productTypes = new HashMap<>();
        for (ProductCommissionDetail detail : details) {
            ProductMaster product = productMapper.selectOne(
                new LambdaQueryWrapper<ProductMaster>()
                    .eq(ProductMaster::getProductCode, detail.getProductCode())
            );
            if (product != null) {
                productTypes.put(detail.getProductCode(), product.getProductType());
            }
        }

        // 按产品类型分组汇总
        Map<String, BigDecimal> summary = new LinkedHashMap<>();
        for (ProductCommissionDetail detail : details) {
            String productType = productTypes.getOrDefault(detail.getProductCode(), "UNKNOWN");
            summary.merge(productType, detail.getCommissionAmount(), BigDecimal::add);
        }

        // 转换为列表
        return summary.entrySet().stream()
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("productType", entry.getKey());
                item.put("totalCommission", entry.getValue());
                return item;
            })
            .sorted((a, b) -> ((BigDecimal) b.get("totalCommission")).compareTo((BigDecimal) a.get("totalCommission")))
            .collect(Collectors.toList());
    }

    /**
     * 获取分润明细
     */
    public List<ProductCommissionDetail> getCommissionDetails(String period, String productCode) {
        LambdaQueryWrapper<ProductCommissionDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductCommissionDetail::getPeriod, period);
        if (productCode != null) {
            wrapper.eq(ProductCommissionDetail::getProductCode, productCode);
        }
        wrapper.orderByDesc(ProductCommissionDetail::getCommissionAmount);
        return detailMapper.selectList(wrapper);
    }

    // ========== 私有方法 ==========

    /**
     * 获取有效的分润规则
     */
    private List<ProductCommissionRule> getEffectiveRules(String productType) {
        LambdaQueryWrapper<ProductCommissionRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductCommissionRule::getStatus, "ACTIVE");
        wrapper.and(w -> w
            .isNull(ProductCommissionRule::getEffectiveDate)
            .or()
            .le(ProductCommissionRule::getEffectiveDate, LocalDate.now())
        );
        wrapper.and(w -> w
            .isNull(ProductCommissionRule::getExpireDate)
            .or()
            .ge(ProductCommissionRule::getExpireDate, LocalDate.now())
        );
        if (productType != null) {
            wrapper.and(w -> w
                .isNull(ProductCommissionRule::getProductType)
                .or()
                .eq(ProductCommissionRule::getProductType, productType)
            );
        }
        return ruleMapper.selectList(wrapper);
    }

    /**
     * 获取有效产品列表
     */
    private List<ProductMaster> getActiveProducts(String productType) {
        LambdaQueryWrapper<ProductMaster> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductMaster::getStatus, "ACTIVE");
        if (productType != null) {
            wrapper.eq(ProductMaster::getProductType, productType);
        }
        return productMapper.selectList(wrapper);
    }

    /**
     * 获取规则适用的产品
     */
    private List<ProductMaster> getApplicableProducts(List<ProductMaster> allProducts, ProductCommissionRule rule) {
        return allProducts.stream()
            .filter(product -> {
                // 检查产品类型
                if (rule.getProductType() != null && !rule.getProductType().equals(product.getProductType())) {
                    return false;
                }
                // 检查产品编码
                if (rule.getProductCode() != null && !rule.getProductCode().equals(product.getProductCode())) {
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());
    }

    /**
     * 获取计算基数金额
     */
    private BigDecimal getCalcBaseAmount(String period, String productCode, String calcBase) {
        // TODO: 从实际业务表获取数据
        // 这里使用模拟数据
        Map<String, Map<String, BigDecimal>> mockData = new HashMap<>();

        // 贷款产品
        Map<String, BigDecimal> loanData = new HashMap<>();
        loanData.put("REVENUE", new BigDecimal("500000"));
        loanData.put("PROFIT", new BigDecimal("200000"));
        loanData.put("BIZ_AMOUNT", new BigDecimal("10000000"));
        mockData.put("LOAN001", loanData);

        // 存款产品
        Map<String, BigDecimal> depositData = new HashMap<>();
        depositData.put("REVENUE", new BigDecimal("300000"));
        depositData.put("PROFIT", new BigDecimal("150000"));
        depositData.put("BIZ_AMOUNT", new BigDecimal("50000000"));
        mockData.put("DEPOSIT001", depositData);

        // 理财产品
        Map<String, BigDecimal> wealthData = new HashMap<>();
        wealthData.put("REVENUE", new BigDecimal("400000"));
        wealthData.put("PROFIT", new BigDecimal("180000"));
        wealthData.put("BIZ_AMOUNT", new BigDecimal("8000000"));
        mockData.put("WEALTH001", wealthData);

        Map<String, BigDecimal> productData = mockData.getOrDefault(productCode, new HashMap<>());
        return productData.getOrDefault(calcBase, BigDecimal.ZERO);
    }

    /**
     * 计算分润金额
     */
    private BigDecimal calculateCommission(BigDecimal baseAmount, ProductCommissionRule rule) {
        BigDecimal commission = BigDecimal.ZERO;

        switch (rule.getCommissionType()) {
            case "REVENUE_SHARE":
            case "PROFIT_SHARE":
            case "FIXED_RATE":
                // 按固定费率计算
                commission = baseAmount.multiply(rule.getRate()).setScale(2, RoundingMode.HALF_UP);
                break;

            case "TIERED":
                // 阶梯费率计算
                commission = calculateTieredCommission(baseAmount, rule.getTierConfig());
                break;

            default:
                commission = baseAmount.multiply(rule.getRate()).setScale(2, RoundingMode.HALF_UP);
        }

        // 应用最低/最高限制
        if (rule.getMinAmount() != null && commission.compareTo(rule.getMinAmount()) < 0) {
            commission = rule.getMinAmount();
        }
        if (rule.getMaxAmount() != null && commission.compareTo(rule.getMaxAmount()) > 0) {
            commission = rule.getMaxAmount();
        }

        return commission;
    }

    /**
     * 计算阶梯分润
     */
    private BigDecimal calculateTieredCommission(BigDecimal baseAmount, String tierConfig) {
        // TODO: 解析阶梯配置并计算
        // 示例配置: [{"min":0,"max":100000,"rate":0.1},{"min":100000,"max":500000,"rate":0.15},{"min":500000,"rate":0.2}]
        return baseAmount.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 生成批次号
     */
    private String generateBatchNo() {
        return "COMM_" + System.currentTimeMillis();
    }

    // ========== 内部类 ==========

    /**
     * 分润结果
     */
    @lombok.Data
    @lombok.Builder
    public static class CommissionResult {
        private String batchNo;
        private String period;
        private int ruleCount;
        private long productCount;
        private BigDecimal totalCommission;
        private List<ProductCommissionDetail> details;

        public static CommissionResult empty(String period) {
            return CommissionResult.builder()
                .period(period)
                .ruleCount(0)
                .productCount(0)
                .totalCommission(BigDecimal.ZERO)
                .details(Collections.emptyList())
                .build();
        }
    }
}
