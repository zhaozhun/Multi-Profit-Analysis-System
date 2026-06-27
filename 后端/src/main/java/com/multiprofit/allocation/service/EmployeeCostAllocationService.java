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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 员工费用分摊服务
 * 将公共费用（运营费、房租、水电、福利等）分摊到每个员工
 */
@Slf4j
@Service
public class EmployeeCostAllocationService {

    @Autowired
    private EmployeeMasterMapper employeeMapper;

    @Autowired
    private EmployeeWorkHoursMapper workHoursMapper;

    @Autowired
    private EmployeeCostAllocationMapper allocationMapper;

    @Autowired
    private CostTypeConfigMapper costTypeMapper;

    /**
     * 执行员工费用分摊
     * @param period 期间
     * @param costType 成本类型
     * @param factorType 分摊因子类型(EMPLOYEE_COUNT/WORK_HOURS/SALARY/WORKSTATION_AREA)
     * @return 分摊结果
     */
    @Transactional
    public EmployeeAllocationResult executeAllocation(String period, String costType, String factorType) {
        log.info("开始员工费用分摊: 期间={}, 成本类型={}, 因子类型={}", period, costType, factorType);

        // 1. 获取待分摊的费用总额
        BigDecimal totalCost = getTotalCost(period, costType);
        if (totalCost.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("未找到待分摊的费用");
            return EmployeeAllocationResult.empty(period, costType);
        }

        // 2. 获取员工列表
        List<EmployeeMaster> employees = getActiveEmployees();
        if (employees.isEmpty()) {
            log.warn("未找到有效员工");
            return EmployeeAllocationResult.empty(period, costType);
        }

        // 3. 获取分摊因子数据
        Map<String, BigDecimal> factorValues = getFactorValues(period, factorType, employees);

        // 4. 计算因子总和
        BigDecimal factorTotal = factorValues.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (factorTotal.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("因子总和为零");
            return EmployeeAllocationResult.empty(period, costType);
        }

        // 5. 执行分摊计算
        String batchNo = generateBatchNo();
        List<EmployeeCostAllocation> allocations = new ArrayList<>();

        for (EmployeeMaster employee : employees) {
            String empCode = employee.getEmployeeCode();
            BigDecimal factorValue = factorValues.getOrDefault(empCode, BigDecimal.ZERO);

            // 计算分摊比例
            BigDecimal ratio = factorValue.divide(factorTotal, 10, RoundingMode.HALF_UP);

            // 计算分摊金额
            BigDecimal allocatedAmount = totalCost.multiply(ratio).setScale(2, RoundingMode.HALF_UP);

            // 构建分摊记录
            EmployeeCostAllocation allocation = new EmployeeCostAllocation();
            allocation.setBatchNo(batchNo);
            allocation.setPeriod(period);
            allocation.setEmployeeCode(empCode);
            allocation.setEmployeeName(employee.getEmployeeName());
            allocation.setOrgCode(employee.getOrgCode());
            allocation.setDeptCode(employee.getDeptCode());
            allocation.setCostType(costType);
            allocation.setCostTypeName(getCostTypeName(costType));
            allocation.setOriginalAmount(totalCost);
            allocation.setAllocatedAmount(allocatedAmount);
            allocation.setAllocationFactor(factorType);
            allocation.setFactorValue(factorValue);
            allocation.setFactorRatio(ratio);

            allocations.add(allocation);
        }

        // 6. 批量保存分摊结果
        for (EmployeeCostAllocation allocation : allocations) {
            allocationMapper.insert(allocation);
        }

        log.info("员工费用分摊完成: 批次号={}, 员工数={}, 总金额={}, 已分摊={}",
            batchNo, allocations.size(), totalCost,
            allocations.stream().map(EmployeeCostAllocation::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 7. 返回结果
        return EmployeeAllocationResult.builder()
            .batchNo(batchNo)
            .period(period)
            .costType(costType)
            .costTypeName(getCostTypeName(costType))
            .factorType(factorType)
            .totalCost(totalCost)
            .employeeCount(employees.size())
            .allocations(allocations)
            .build();
    }

    /**
     * 查询员工费用分摊结果
     */
    public List<EmployeeCostAllocation> getAllocations(String period, String costType, String employeeCode) {
        LambdaQueryWrapper<EmployeeCostAllocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmployeeCostAllocation::getPeriod, period);
        if (costType != null) {
            wrapper.eq(EmployeeCostAllocation::getCostType, costType);
        }
        if (employeeCode != null) {
            wrapper.eq(EmployeeCostAllocation::getEmployeeCode, employeeCode);
        }
        wrapper.orderByDesc(EmployeeCostAllocation::getAllocatedAmount);
        return allocationMapper.selectList(wrapper);
    }

    /**
     * 查询员工费用汇总
     */
    public List<Map<String, Object>> getEmployeeSummary(String period) {
        List<EmployeeCostAllocation> allocations = getAllocations(period, null, null);

        // 按员工分组汇总
        Map<String, BigDecimal> summary = new LinkedHashMap<>();
        Map<String, EmployeeCostAllocation> employeeInfo = new LinkedHashMap<>();

        for (EmployeeCostAllocation allocation : allocations) {
            String empCode = allocation.getEmployeeCode();
            summary.merge(empCode, allocation.getAllocatedAmount(), BigDecimal::add);
            employeeInfo.putIfAbsent(empCode, allocation);
        }

        // 转换为列表
        return summary.entrySet().stream()
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                EmployeeCostAllocation info = employeeInfo.get(entry.getKey());
                item.put("employeeCode", entry.getKey());
                item.put("employeeName", info.getEmployeeName());
                item.put("orgCode", info.getOrgCode());
                item.put("deptCode", info.getDeptCode());
                item.put("totalAllocated", entry.getValue());
                return item;
            })
            .sorted((a, b) -> ((BigDecimal) b.get("totalAllocated")).compareTo((BigDecimal) a.get("totalAllocated")))
            .collect(Collectors.toList());
    }

    /**
     * 查询部门费用汇总
     */
    public List<Map<String, Object>> getDeptSummary(String period) {
        List<EmployeeCostAllocation> allocations = getAllocations(period, null, null);

        // 按部门分组汇总
        Map<String, BigDecimal> summary = new LinkedHashMap<>();
        Map<String, String> deptNames = new LinkedHashMap<>();

        for (EmployeeCostAllocation allocation : allocations) {
            String deptCode = allocation.getDeptCode();
            summary.merge(deptCode, allocation.getAllocatedAmount(), BigDecimal::add);
            deptNames.putIfAbsent(deptCode, allocation.getDeptCode());
        }

        // 转换为列表
        return summary.entrySet().stream()
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("deptCode", entry.getKey());
                item.put("deptName", deptNames.get(entry.getKey()));
                item.put("totalAllocated", entry.getValue());
                return item;
            })
            .sorted((a, b) -> ((BigDecimal) b.get("totalAllocated")).compareTo((BigDecimal) a.get("totalAllocated")))
            .collect(Collectors.toList());
    }

    // ========== 私有方法 ==========

    /**
     * 获取待分摊的费用总额
     */
    private BigDecimal getTotalCost(String period, String costType) {
        // TODO: 从实际业务表获取费用数据
        // 这里使用模拟数据
        Map<String, BigDecimal> costData = new HashMap<>();
        costData.put("OPERATION", new BigDecimal("50000"));  // 运营费
        costData.put("RENT", new BigDecimal("100000"));      // 房租
        costData.put("UTILITIES", new BigDecimal("20000"));   // 水电
        costData.put("WELFARE", new BigDecimal("30000"));     // 福利
        costData.put("TRAINING", new BigDecimal("15000"));    // 培训费
        costData.put("ADMIN", new BigDecimal("25000"));       // 行政费

        return costData.getOrDefault(costType, BigDecimal.ZERO);
    }

    /**
     * 获取有效员工列表
     */
    private List<EmployeeMaster> getActiveEmployees() {
        return employeeMapper.selectList(
            new LambdaQueryWrapper<EmployeeMaster>()
                .eq(EmployeeMaster::getStatus, "ACTIVE")
        );
    }

    /**
     * 获取分摊因子数据
     */
    private Map<String, BigDecimal> getFactorValues(String period, String factorType, List<EmployeeMaster> employees) {
        Map<String, BigDecimal> factorValues = new LinkedHashMap<>();

        switch (factorType) {
            case "EMPLOYEE_COUNT":
                // 按员工人数（简单计数，每人权重为1）
                for (EmployeeMaster emp : employees) {
                    factorValues.put(emp.getEmployeeCode(), BigDecimal.ONE);
                }
                break;

            case "WORK_HOURS":
                // 按工时
                for (EmployeeMaster emp : employees) {
                    BigDecimal hours = getWorkHours(emp.getEmployeeCode(), period);
                    factorValues.put(emp.getEmployeeCode(), hours);
                }
                break;

            case "SALARY":
                // 按薪资
                for (EmployeeMaster emp : employees) {
                    factorValues.put(emp.getEmployeeCode(), emp.getSalary());
                }
                break;

            case "WORKSTATION_AREA":
                // 按工位面积
                for (EmployeeMaster emp : employees) {
                    factorValues.put(emp.getEmployeeCode(), emp.getWorkstationArea());
                }
                break;

            default:
                // 默认按人数
                for (EmployeeMaster emp : employees) {
                    factorValues.put(emp.getEmployeeCode(), BigDecimal.ONE);
                }
        }

        return factorValues;
    }

    /**
     * 获取员工工时
     */
    private BigDecimal getWorkHours(String employeeCode, String period) {
        // TODO: 从工时表获取实际数据
        // 这里使用模拟数据
        Map<String, BigDecimal> workHours = new HashMap<>();
        workHours.put("EMP001", new BigDecimal("186"));
        workHours.put("EMP002", new BigDecimal("191"));
        workHours.put("EMP003", new BigDecimal("177"));
        workHours.put("EMP004", new BigDecimal("184"));
        workHours.put("EMP005", new BigDecimal("196"));

        return workHours.getOrDefault(employeeCode, new BigDecimal("176"));
    }

    /**
     * 获取成本类型名称
     */
    private String getCostTypeName(String costType) {
        // TODO: 从配置表获取
        Map<String, String> names = new HashMap<>();
        names.put("OPERATION", "运营费");
        names.put("RENT", "房租");
        names.put("UTILITIES", "水电");
        names.put("WELFARE", "福利");
        names.put("TRAINING", "培训费");
        names.put("ADMIN", "行政费");

        return names.getOrDefault(costType, costType);
    }

    /**
     * 生成批次号
     */
    private String generateBatchNo() {
        return "EMP_ALLOC_" + System.currentTimeMillis();
    }

    // ========== 内部类 ==========

    /**
     * 员工分摊结果
     */
    @lombok.Data
    @lombok.Builder
    public static class EmployeeAllocationResult {
        private String batchNo;
        private String period;
        private String costType;
        private String costTypeName;
        private String factorType;
        private BigDecimal totalCost;
        private int employeeCount;
        private List<EmployeeCostAllocation> allocations;

        public static EmployeeAllocationResult empty(String period, String costType) {
            return EmployeeAllocationResult.builder()
                .period(period)
                .costType(costType)
                .totalCost(BigDecimal.ZERO)
                .employeeCount(0)
                .allocations(Collections.emptyList())
                .build();
        }
    }
}
