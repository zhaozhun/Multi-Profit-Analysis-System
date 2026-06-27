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
 * 运营费用分摊服务
 * 支持多种费用类型的精细化分摊
 */
@Slf4j
@Service
public class OperationCostAllocationService {

    @Autowired
    private OperationCostAllocationRuleMapper ruleMapper;

    @Autowired
    private CostActualRecordMapper actualMapper;

    @Autowired
    private OperationCostAllocationResultMapper resultMapper;

    @Autowired
    private EmployeeMasterMapper employeeMapper;

    /**
     * 执行运营费用分摊
     * @param period 期间
     * @param costType 费用类型(可选，为空则分摊所有类型)
     * @return 分摊结果
     */
    @Transactional
    public OperationCostAllocationResult executeAllocation(String period, String costType) {
        log.info("开始运营费用分摊: 期间={}, 费用类型={}", period, costType);

        // 1. 获取费用实际发生数据
        List<CostActualRecord> actualCosts = getActualCosts(period, costType);
        if (actualCosts.isEmpty()) {
            log.warn("未找到运营费用数据");
            return OperationCostAllocationResult.empty(period);
        }

        // 2. 获取分摊规则
        Map<String, OperationCostAllocationRule> ruleMap = getAllocationRules();

        // 3. 获取员工数据（用于按人头分摊）
        List<EmployeeMaster> employees = getActiveEmployees();
        Map<String, List<EmployeeMaster>> deptEmployees = employees.stream()
            .collect(Collectors.groupingBy(EmployeeMaster::getDeptCode));

        // 4. 执行分摊
        String batchNo = generateBatchNo();
        List<CostAllocationDetail> allDetails = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CostActualRecord actual : actualCosts) {
            OperationCostAllocationRule rule = ruleMap.get(actual.getCostCode());
            if (rule == null) {
                log.warn("未找到费用分摊规则: {}", actual.getCostCode());
                continue;
            }

            // 根据分摊方法执行分摊
            List<CostAllocationDetail> details = allocateCost(actual, rule, employees, deptEmployees, batchNo);
            allDetails.addAll(details);

            totalAmount = totalAmount.add(actual.getAmount());

            // 更新费用状态
            actual.setStatus("ALLOCATED");
            actualMapper.updateById(actual);
        }

        // 5. 批量保存分摊结果
        for (CostAllocationDetail detail : allDetails) {
            resultMapper.insert(detail.toResult());
        }

        log.info("运营费用分摊完成: 批次号={}, 费用类型数={}, 总金额={}, 分摊记录数={}",
            batchNo, actualCosts.size(), totalAmount, allDetails.size());

        return OperationCostAllocationResult.builder()
            .batchNo(batchNo)
            .period(period)
            .costTypeCount(actualCosts.size())
            .totalAmount(totalAmount)
            .allocationCount(allDetails.size())
            .details(allDetails)
            .build();
    }

    /**
     * 查询运营费用分摊结果
     */
    public List<OperationCostAllocationResultEntity> getAllocationResults(String period, String costType) {
        LambdaQueryWrapper<OperationCostAllocationResultEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperationCostAllocationResultEntity::getPeriod, period);
        if (costType != null) {
            wrapper.eq(OperationCostAllocationResultEntity::getCostType, costType);
        }
        wrapper.orderByAsc(OperationCostAllocationResultEntity::getCostCode)
               .orderByDesc(OperationCostAllocationResultEntity::getAllocatedAmount);
        return resultMapper.selectList(wrapper);
    }

    /**
     * 按费用类型汇总
     */
    public List<Map<String, Object>> getSummaryByCostType(String period) {
        List<OperationCostAllocationResultEntity> results = getAllocationResults(period, null);

        Map<String, BigDecimal> summary = new LinkedHashMap<>();
        Map<String, String> costNames = new LinkedHashMap<>();

        for (OperationCostAllocationResultEntity result : results) {
            summary.merge(result.getCostType(), result.getAllocatedAmount(), BigDecimal::add);
            costNames.putIfAbsent(result.getCostType(), result.getCostName());
        }

        return summary.entrySet().stream()
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("costType", entry.getKey());
                item.put("costName", costNames.get(entry.getKey()));
                item.put("totalAmount", entry.getValue());
                return item;
            })
            .sorted((a, b) -> ((BigDecimal) b.get("totalAmount")).compareTo((BigDecimal) a.get("totalAmount")))
            .collect(Collectors.toList());
    }

    /**
     * 按员工汇总
     */
    public List<Map<String, Object>> getSummaryByEmployee(String period) {
        List<OperationCostAllocationResultEntity> results = getAllocationResults(period, null);

        Map<String, BigDecimal> summary = new LinkedHashMap<>();
        Map<String, OperationCostAllocationResultEntity> employeeInfo = new LinkedHashMap<>();

        for (OperationCostAllocationResultEntity result : results) {
            if ("EMPLOYEE".equals(result.getTargetType())) {
                summary.merge(result.getTargetCode(), result.getAllocatedAmount(), BigDecimal::add);
                employeeInfo.putIfAbsent(result.getTargetCode(), result);
            }
        }

        return summary.entrySet().stream()
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                OperationCostAllocationResultEntity info = employeeInfo.get(entry.getKey());
                item.put("employeeCode", entry.getKey());
                item.put("employeeName", info.getTargetName());
                item.put("totalAllocated", entry.getValue());
                return item;
            })
            .sorted((a, b) -> ((BigDecimal) b.get("totalAllocated")).compareTo((BigDecimal) a.get("totalAllocated")))
            .collect(Collectors.toList());
    }

    // ========== 私有方法 ==========

    /**
     * 获取费用实际发生数据
     */
    private List<CostActualRecord> getActualCosts(String period, String costType) {
        LambdaQueryWrapper<CostActualRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CostActualRecord::getPeriod, period);
        if (costType != null) {
            wrapper.eq(CostActualRecord::getCostType, costType);
        }
        wrapper.eq(CostActualRecord::getStatus, "PENDING");
        return actualMapper.selectList(wrapper);
    }

    /**
     * 获取分摊规则
     */
    private Map<String, OperationCostAllocationRule> getAllocationRules() {
        List<OperationCostAllocationRule> rules = ruleMapper.selectList(
            new LambdaQueryWrapper<OperationCostAllocationRule>()
                .eq(OperationCostAllocationRule::getStatus, "ACTIVE")
        );
        return rules.stream()
            .collect(Collectors.toMap(OperationCostAllocationRule::getCostCode, r -> r));
    }

    /**
     * 获取有效员工
     */
    private List<EmployeeMaster> getActiveEmployees() {
        return employeeMapper.selectList(
            new LambdaQueryWrapper<EmployeeMaster>()
                .eq(EmployeeMaster::getStatus, "ACTIVE")
        );
    }

    /**
     * 执行单笔费用分摊
     */
    private List<CostAllocationDetail> allocateCost(
            CostActualRecord actual,
            OperationCostAllocationRule rule,
            List<EmployeeMaster> employees,
            Map<String, List<EmployeeMaster>> deptEmployees,
            String batchNo) {

        List<CostAllocationDetail> details = new ArrayList<>();

        switch (rule.getAllocationMethod()) {
            case "EMPLOYEE_COUNT":
                // 按员工人数平均分摊
                details = allocateByEmployeeCount(actual, rule, employees, batchNo);
                break;

            case "WORK_HOURS":
                // 按工时分摊
                details = allocateByWorkHours(actual, rule, employees, batchNo);
                break;

            case "SALARY":
                // 按薪资分摊
                details = allocateBySalary(actual, rule, employees, batchNo);
                break;

            case "AREA":
                // 按面积分摊
                details = allocateByArea(actual, rule, employees, batchNo);
                break;

            case "BIZ_VOLUME":
                // 按业务量分摊
                details = allocateByBizVolume(actual, rule, employees, batchNo);
                break;

            case "DEPT_DIRECT":
                // 按部门直接归属
                details = allocateByDeptDirect(actual, rule, deptEmployees, batchNo);
                break;

            case "CUSTOM":
                // 自定义分摊
                details = allocateByCustom(actual, rule, employees, batchNo);
                break;

            default:
                // 默认按人数分摊
                details = allocateByEmployeeCount(actual, rule, employees, batchNo);
        }

        return details;
    }

    /**
     * 按员工人数分摊
     */
    private List<CostAllocationDetail> allocateByEmployeeCount(
            CostActualRecord actual,
            OperationCostAllocationRule rule,
            List<EmployeeMaster> employees,
            String batchNo) {

        List<CostAllocationDetail> details = new ArrayList<>();
        BigDecimal totalAmount = actual.getAmount();
        int employeeCount = employees.size();

        if (employeeCount == 0) return details;

        BigDecimal perEmployee = totalAmount.divide(BigDecimal.valueOf(employeeCount), 2, RoundingMode.HALF_UP);

        for (EmployeeMaster emp : employees) {
            CostAllocationDetail detail = new CostAllocationDetail();
            detail.setBatchNo(batchNo);
            detail.setPeriod(actual.getPeriod());
            detail.setCostCode(actual.getCostCode());
            detail.setCostName(actual.getCostName());
            detail.setCostType(actual.getCostType());
            detail.setTotalAmount(totalAmount);
            detail.setTargetType("EMPLOYEE");
            detail.setTargetCode(emp.getEmployeeCode());
            detail.setTargetName(emp.getEmployeeName());
            detail.setAllocatedAmount(perEmployee);
            detail.setAllocationFactor("EMPLOYEE_COUNT");
            detail.setFactorValue(BigDecimal.ONE);
            detail.setFactorRatio(BigDecimal.ONE.divide(BigDecimal.valueOf(employeeCount), 10, RoundingMode.HALF_UP));

            details.add(detail);
        }

        return details;
    }

    /**
     * 按薪资分摊
     */
    private List<CostAllocationDetail> allocateBySalary(
            CostActualRecord actual,
            OperationCostAllocationRule rule,
            List<EmployeeMaster> employees,
            String batchNo) {

        List<CostAllocationDetail> details = new ArrayList<>();
        BigDecimal totalAmount = actual.getAmount();

        // 计算薪资总和
        BigDecimal totalSalary = employees.stream()
            .map(EmployeeMaster::getSalary)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSalary.compareTo(BigDecimal.ZERO) == 0) return details;

        for (EmployeeMaster emp : employees) {
            BigDecimal ratio = emp.getSalary().divide(totalSalary, 10, RoundingMode.HALF_UP);
            BigDecimal allocatedAmount = totalAmount.multiply(ratio).setScale(2, RoundingMode.HALF_UP);

            CostAllocationDetail detail = new CostAllocationDetail();
            detail.setBatchNo(batchNo);
            detail.setPeriod(actual.getPeriod());
            detail.setCostCode(actual.getCostCode());
            detail.setCostName(actual.getCostName());
            detail.setCostType(actual.getCostType());
            detail.setTotalAmount(totalAmount);
            detail.setTargetType("EMPLOYEE");
            detail.setTargetCode(emp.getEmployeeCode());
            detail.setTargetName(emp.getEmployeeName());
            detail.setAllocatedAmount(allocatedAmount);
            detail.setAllocationFactor("SALARY");
            detail.setFactorValue(emp.getSalary());
            detail.setFactorRatio(ratio);

            details.add(detail);
        }

        return details;
    }

    /**
     * 按面积分摊
     */
    private List<CostAllocationDetail> allocateByArea(
            CostActualRecord actual,
            OperationCostAllocationRule rule,
            List<EmployeeMaster> employees,
            String batchNo) {

        List<CostAllocationDetail> details = new ArrayList<>();
        BigDecimal totalAmount = actual.getAmount();

        // 计算面积总和
        BigDecimal totalArea = employees.stream()
            .map(EmployeeMaster::getWorkstationArea)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalArea.compareTo(BigDecimal.ZERO) == 0) return details;

        for (EmployeeMaster emp : employees) {
            BigDecimal ratio = emp.getWorkstationArea().divide(totalArea, 10, RoundingMode.HALF_UP);
            BigDecimal allocatedAmount = totalAmount.multiply(ratio).setScale(2, RoundingMode.HALF_UP);

            CostAllocationDetail detail = new CostAllocationDetail();
            detail.setBatchNo(batchNo);
            detail.setPeriod(actual.getPeriod());
            detail.setCostCode(actual.getCostCode());
            detail.setCostName(actual.getCostName());
            detail.setCostType(actual.getCostType());
            detail.setTotalAmount(totalAmount);
            detail.setTargetType("EMPLOYEE");
            detail.setTargetCode(emp.getEmployeeCode());
            detail.setTargetName(emp.getEmployeeName());
            detail.setAllocatedAmount(allocatedAmount);
            detail.setAllocationFactor("WORKSTATION_AREA");
            detail.setFactorValue(emp.getWorkstationArea());
            detail.setFactorRatio(ratio);

            details.add(detail);
        }

        return details;
    }

    /**
     * 按工时分摊
     */
    private List<CostAllocationDetail> allocateByWorkHours(
            CostActualRecord actual,
            OperationCostAllocationRule rule,
            List<EmployeeMaster> employees,
            String batchNo) {

        // TODO: 从工时表获取实际工时数据
        // 这里简化为按标准工时分摊
        return allocateByEmployeeCount(actual, rule, employees, batchNo);
    }

    /**
     * 按业务量分摊
     */
    private List<CostAllocationDetail> allocateByBizVolume(
            CostActualRecord actual,
            OperationCostAllocationRule rule,
            List<EmployeeMaster> employees,
            String batchNo) {

        // TODO: 从业务表获取业务量数据
        // 这里简化为按人数分摊
        return allocateByEmployeeCount(actual, rule, employees, batchNo);
    }

    /**
     * 按部门直接归属
     */
    private List<CostAllocationDetail> allocateByDeptDirect(
            CostActualRecord actual,
            OperationCostAllocationRule rule,
            Map<String, List<EmployeeMaster>> deptEmployees,
            String batchNo) {

        List<CostAllocationDetail> details = new ArrayList<>();
        BigDecimal totalAmount = actual.getAmount();

        // 如果费用已指定部门，直接归属到该部门的员工
        if (actual.getDeptCode() != null) {
            List<EmployeeMaster> deptEmps = deptEmployees.get(actual.getDeptCode());
            if (deptEmps != null && !deptEmps.isEmpty()) {
                BigDecimal perEmployee = totalAmount.divide(BigDecimal.valueOf(deptEmps.size()), 2, RoundingMode.HALF_UP);

                for (EmployeeMaster emp : deptEmps) {
                    CostAllocationDetail detail = new CostAllocationDetail();
                    detail.setBatchNo(batchNo);
                    detail.setPeriod(actual.getPeriod());
                    detail.setCostCode(actual.getCostCode());
                    detail.setCostName(actual.getCostName());
                    detail.setCostType(actual.getCostType());
                    detail.setTotalAmount(totalAmount);
                    detail.setTargetType("EMPLOYEE");
                    detail.setTargetCode(emp.getEmployeeCode());
                    detail.setTargetName(emp.getEmployeeName());
                    detail.setAllocatedAmount(perEmployee);
                    detail.setAllocationFactor("DEPT_DIRECT");
                    detail.setFactorValue(BigDecimal.ONE);
                    detail.setFactorRatio(BigDecimal.ONE.divide(BigDecimal.valueOf(deptEmps.size()), 10, RoundingMode.HALF_UP));

                    details.add(detail);
                }
            }
        }

        return details;
    }

    /**
     * 自定义分摊
     */
    private List<CostAllocationDetail> allocateByCustom(
            CostActualRecord actual,
            OperationCostAllocationRule rule,
            List<EmployeeMaster> employees,
            String batchNo) {

        // TODO: 实现自定义公式分摊
        return allocateByEmployeeCount(actual, rule, employees, batchNo);
    }

    /**
     * 生成批次号
     */
    private String generateBatchNo() {
        return "OP_COST_" + System.currentTimeMillis();
    }

    // ========== 内部类 ==========

    /**
     * 分摊明细
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CostAllocationDetail {
        private String batchNo;
        private String period;
        private String costCode;
        private String costName;
        private String costType;
        private BigDecimal totalAmount;
        private String targetType;
        private String targetCode;
        private String targetName;
        private BigDecimal allocatedAmount;
        private String allocationFactor;
        private BigDecimal factorValue;
        private BigDecimal factorRatio;

        public OperationCostAllocationResultEntity toResult() {
            OperationCostAllocationResultEntity entity = new OperationCostAllocationResultEntity();
            entity.setBatchNo(batchNo);
            entity.setPeriod(period);
            entity.setCostCode(costCode);
            entity.setCostName(costName);
            entity.setCostType(costType);
            entity.setTotalAmount(totalAmount);
            entity.setTargetType(targetType);
            entity.setTargetCode(targetCode);
            entity.setTargetName(targetName);
            entity.setAllocatedAmount(allocatedAmount);
            entity.setAllocationFactor(allocationFactor);
            entity.setFactorValue(factorValue);
            entity.setFactorRatio(factorRatio);
            return entity;
        }
    }

    /**
     * 分摊结果
     */
    @lombok.Data
    @lombok.Builder
    public static class OperationCostAllocationResult {
        private String batchNo;
        private String period;
        private int costTypeCount;
        private BigDecimal totalAmount;
        private int allocationCount;
        private List<CostAllocationDetail> details;

        public static OperationCostAllocationResult empty(String period) {
            return OperationCostAllocationResult.builder()
                .period(period)
                .costTypeCount(0)
                .totalAmount(BigDecimal.ZERO)
                .allocationCount(0)
                .details(Collections.emptyList())
                .build();
        }
    }
}
