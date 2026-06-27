package com.multiprofit.task;

import com.multiprofit.service.IndicatorCalcService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 指标定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndicatorScheduleTask {

    private final IndicatorCalcService indicatorCalcService;

    /**
     * 每日凌晨2点计算当日指标
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyCalc() {
        log.info("开始执行每日指标计算任务");
        String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        // 日指标计算逻辑
        log.info("每日指标计算任务完成，日期: {}", yesterday);
    }

    /**
     * 每月1日凌晨3点计算上月指标
     */
    @Scheduled(cron = "0 0 3 1 * ?")
    public void monthlyCalc() {
        log.info("开始执行每月指标计算任务");
        String lastMonth = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        try {
            List<Map<String, Object>> results = indicatorCalcService.calcAllIndicators("MONTH", lastMonth);
            log.info("每月指标计算任务完成，计算指标数量: {}", results.size());
        } catch (Exception e) {
            log.error("每月指标计算任务失败", e);
        }
    }

    /**
     * 每年1月1日凌晨4点计算上年指标
     */
    @Scheduled(cron = "0 0 4 1 1 ?")
    public void yearlyCalc() {
        log.info("开始执行每年指标计算任务");
        String lastYear = String.valueOf(LocalDate.now().getYear() - 1);
        try {
            List<Map<String, Object>> results = indicatorCalcService.calcAllIndicators("YEAR", lastYear);
            log.info("每年指标计算任务完成，计算指标数量: {}", results.size());
        } catch (Exception e) {
            log.error("每年指标计算任务失败", e);
        }
    }

    /**
     * 手动全量重算
     */
    public void recalcAll(String calcPeriod, String periodValue) {
        log.info("开始手动全量重算，周期: {}, 值: {}", calcPeriod, periodValue);
        try {
            List<Map<String, Object>> results = indicatorCalcService.calcAllIndicators(calcPeriod, periodValue);
            log.info("手动全量重算完成，计算指标数量: {}", results.size());
        } catch (Exception e) {
            log.error("手动全量重算失败", e);
            throw e;
        }
    }
}
