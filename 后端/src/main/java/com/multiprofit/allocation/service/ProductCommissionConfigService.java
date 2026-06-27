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
import java.util.*;
import java.util.stream.Collectors;

/**
 * 产品分润配置服务
 * 支持精细化的产品分润配置和计算
 */
@Slf4j
@Service
public class ProductCommissionConfigService {

    @Autowired
    private ProductCommissionConfigMapper configMapper;

    @Autowired
    private ProductIncomeDataMapper incomeMapper;

    /**
     * 获取所有产品分润配置
     */
    public List<ProductCommissionConfig> getAllConfigs() {
        return configMapper.selectList(
            new LambdaQueryWrapper<ProductCommissionConfig>()
                .orderByAsc(ProductCommissionConfig::getProductType)
                .orderByAsc(ProductCommissionConfig::getProductCode)
        );
    }

    /**
     * 获取需要分润的产品配置
     */
    public List<ProductCommissionConfig> getCommissionConfigs() {
        return configMapper.selectList(
            new LambdaQueryWrapper<ProductCommissionConfig>()
                .eq(ProductCommissionConfig::getNeedCommission, true)
                .eq(ProductCommissionConfig::getStatus, "ACTIVE")
                .orderByAsc(ProductCommissionConfig::getProductCode)
        );
    }

    /**
     * 获取不需要分润的产品配置
     */
    public List<ProductCommissionConfig> getNonCommissionConfigs() {
        return configMapper.selectList(
            new LambdaQueryWrapper<ProductCommissionConfig>()
                .eq(ProductCommissionConfig::getNeedCommission, false)
                .eq(ProductCommissionConfig::getStatus, "ACTIVE")
                .orderByAsc(ProductCommissionConfig::getProductCode)
        );
    }

    /**
     * 更新产品分润配置
     */
    @Transactional
    public ProductCommissionConfig updateConfig(String productCode, ProductCommissionConfig config) {
        ProductCommissionConfig existing = configMapper.selectOne(
            new LambdaQueryWrapper<ProductCommissionConfig>()
                .eq(ProductCommissionConfig::getProductCode, productCode)
        );

        if (existing == null) {
            throw new RuntimeException("产品配置不存在: " + productCode);
        }

        config.setId(existing.getId());
        config.setProductCode(productCode); // 编码不可修改
        configMapper.updateById(config);

        log.info("更新产品分润配置: {}", productCode);
        return config;
    }

    /**
     * 计算产品分润（基于配置）
     */
    public CommissionCalcResult calculateCommission(String period) {
        log.info("开始计算产品分润: 期间={}", period);

        // 1. 获取需要分润的产品配置
        List<ProductCommissionConfig> commissionConfigs = getCommissionConfigs();
        if (commissionConfigs.isEmpty()) {
            log.warn("未找到需要分润的产品配置");
            return CommissionCalcResult.empty(period);
        }

        // 2. 获取产品收入数据
        List<ProductIncomeData> incomeDataList = getIncomeData(period);
        Map<String, ProductIncomeData> incomeDataMap = incomeDataList.stream()
            .collect(Collectors.toMap(ProductIncomeData::getProductCode, d -> d));

        // 3. 计算每个产品的分润
        List<CommissionDetail> details = new ArrayList<>();
        BigDecimal totalCommission = BigDecimal.ZERO;

        for (ProductCommissionConfig config : commissionConfigs) {
            ProductIncomeData incomeData = incomeDataMap.get(config.getProductCode());
            if (incomeData == null) {
                log.warn("未找到产品收入数据: {}", config.getProductCode());
                continue;
            }

            // 获取计算基数
            BigDecimal baseAmount = getCalcBaseAmount(incomeData, config.getCalcBase());
            if (baseAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.info("产品{}计算基数为零或负数，跳过分润计算", config.getProductCode());
                continue;
            }

            // 计算分润金额
            BigDecimal commissionAmount = baseAmount.multiply(config.getCommissionRate())
                .setScale(2, RoundingMode.HALF_UP);

            // 应用最低/最高限制
            if (config.getMinCommission() != null && commissionAmount.compareTo(config.getMinCommission()) < 0) {
                commissionAmount = config.getMinCommission();
            }
            if (config.getMaxCommission() != null && commissionAmount.compareTo(config.getMaxCommission()) > 0) {
                commissionAmount = config.getMaxCommission();
            }

            // 构建分润明细
            CommissionDetail detail = CommissionDetail.builder()
                .productCode(config.getProductCode())
                .productName(config.getProductName())
                .productType(config.getProductType())
                .commissionType(config.getCommissionType())
                .calcBase(config.getCalcBase())
                .baseAmount(baseAmount)
                .commissionRate(config.getCommissionRate())
                .commissionAmount(commissionAmount)
                .receiverType(config.getReceiverType())
                .receiverCode(config.getReceiverCode())
                .receiverName(config.getReceiverName())
                .build();

            details.add(detail);
            totalCommission = totalCommission.add(commissionAmount);
        }

        log.info("产品分润计算完成: 产品数={}, 总分润={}", details.size(), totalCommission);

        return CommissionCalcResult.builder()
            .period(period)
            .totalProducts(commissionConfigs.size())
            .commissionProducts(details.size())
            .totalCommission(totalCommission)
            .details(details)
            .build();
    }

    /**
     * 获取产品分润预览
     */
    public List<Map<String, Object>> previewCommission(String period) {
        List<ProductCommissionConfig> allConfigs = getAllConfigs();
        List<ProductIncomeData> incomeDataList = getIncomeData(period);
        Map<String, ProductIncomeData> incomeDataMap = incomeDataList.stream()
            .collect(Collectors.toMap(ProductIncomeData::getProductCode, d -> d));

        List<Map<String, Object>> preview = new ArrayList<>();

        for (ProductCommissionConfig config : allConfigs) {
            Map<String, Object> item = new HashMap<>();
            item.put("productCode", config.getProductCode());
            item.put("productName", config.getProductName());
            item.put("productType", config.getProductType());
            item.put("needCommission", config.getNeedCommission());
            item.put("commissionType", config.getCommissionType());
            item.put("calcBase", config.getCalcBase());
            item.put("commissionRate", config.getCommissionRate());
            item.put("receiverName", config.getReceiverName());

            // 获取收入数据
            ProductIncomeData incomeData = incomeDataMap.get(config.getProductCode());
            if (incomeData != null) {
                item.put("interestIncome", incomeData.getInterestIncome());
                item.put("loanBalance", incomeData.getLoanBalance());
                item.put("totalRevenue", incomeData.getTotalRevenue());
                item.put("netProfit", incomeData.getNetProfit());

                // 计算预估分润
                if (config.getNeedCommission() && config.getCommissionRate() != null) {
                    BigDecimal baseAmount = getCalcBaseAmount(incomeData, config.getCalcBase());
                    BigDecimal estimatedCommission = baseAmount.multiply(config.getCommissionRate())
                        .setScale(2, RoundingMode.HALF_UP);
                    item.put("estimatedCommission", estimatedCommission);
                } else {
                    item.put("estimatedCommission", BigDecimal.ZERO);
                }
            }

            preview.add(item);
        }

        return preview;
    }

    // ========== 私有方法 ==========

    /**
     * 获取产品收入数据
     */
    private List<ProductIncomeData> getIncomeData(String period) {
        return incomeMapper.selectList(
            new LambdaQueryWrapper<ProductIncomeData>()
                .eq(ProductIncomeData::getPeriod, period)
        );
    }

    /**
     * 获取计算基数金额
     */
    private BigDecimal getCalcBaseAmount(ProductIncomeData incomeData, String calcBase) {
        if (calcBase == null) return BigDecimal.ZERO;

        switch (calcBase) {
            case "INTEREST_INCOME":
                return incomeData.getInterestIncome() != null ? incomeData.getInterestIncome() : BigDecimal.ZERO;
            case "LOAN_BALANCE":
                return incomeData.getLoanBalance() != null ? incomeData.getLoanBalance() : BigDecimal.ZERO;
            case "FEE_INCOME":
                return incomeData.getFeeIncome() != null ? incomeData.getFeeIncome() : BigDecimal.ZERO;
            case "NET_PROFIT":
                return incomeData.getNetProfit() != null ? incomeData.getNetProfit() : BigDecimal.ZERO;
            case "TOTAL_REVENUE":
                return incomeData.getTotalRevenue() != null ? incomeData.getTotalRevenue() : BigDecimal.ZERO;
            default:
                return BigDecimal.ZERO;
        }
    }

    // ========== 内部类 ==========

    /**
     * 分润计算结果
     */
    @lombok.Data
    @lombok.Builder
    public static class CommissionCalcResult {
        private String period;
        private int totalProducts;
        private int commissionProducts;
        private BigDecimal totalCommission;
        private List<CommissionDetail> details;

        public static CommissionCalcResult empty(String period) {
            return CommissionCalcResult.builder()
                .period(period)
                .totalProducts(0)
                .commissionProducts(0)
                .totalCommission(BigDecimal.ZERO)
                .details(Collections.emptyList())
                .build();
        }
    }

    /**
     * 分润明细
     */
    @lombok.Data
    @lombok.Builder
    public static class CommissionDetail {
        private String productCode;
        private String productName;
        private String productType;
        private String commissionType;
        private String calcBase;
        private BigDecimal baseAmount;
        private BigDecimal commissionRate;
        private BigDecimal commissionAmount;
        private String receiverType;
        private String receiverCode;
        private String receiverName;
    }
}
