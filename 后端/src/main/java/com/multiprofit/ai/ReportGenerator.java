package com.multiprofit.ai;

import com.multiprofit.dto.DashboardDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * AI报表生成器 - 自动生成Excel报表
 */
@Component
public class ReportGenerator {

    /**
     * 生成经营简报Excel
     */
    public byte[] generateExcelReport(DashboardDTO dashboard, String period) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Sheet1: 核心指标概览
            createKpiSheet(workbook, dashboard, period);
            // Sheet2: 维度排名
            createRankingSheet(workbook, dashboard);
            // Sheet3: 预警信息
            createAlertSheet(workbook, dashboard);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createKpiSheet(Workbook workbook, DashboardDTO data, String period) {
        Sheet sheet = workbook.createSheet("核心指标-" + period);

        // 标题行
        Row titleRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(workbook);
        String[] headers = {"指标名称", "当期值", "同比增速", "环比增速", "预算完成率"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = titleRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据行
        if (data.getKpiCards() != null) {
            int rowNum = 1;
            for (DashboardDTO.KpiCard card : data.getKpiCards()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(card.getMetricName());
                row.createCell(1).setCellValue(card.getValue().doubleValue());
                row.createCell(2).setCellValue(formatPercent(card.getYoyGrowth()));
                row.createCell(3).setCellValue(formatPercent(card.getMomGrowth()));
                row.createCell(4).setCellValue(formatPercent(card.getBudgetRate()));
            }
        }

        // 自动列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createRankingSheet(Workbook workbook, DashboardDTO data) {
        Sheet sheet = workbook.createSheet("维度盈利排名");
        Row titleRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(workbook);
        String[] headers = {"维度类型", "名称", "净利润", "同比增速"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = titleRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        if (data.getDimOverviews() != null) {
            for (DashboardDTO.DimOverview overview : data.getDimOverviews()) {
                if (overview.getTopItems() != null) {
                    for (DashboardDTO.DimTopItem item : overview.getTopItems()) {
                        Row row = sheet.createRow(rowNum++);
                        row.createCell(0).setCellValue(overview.getDimName());
                        row.createCell(1).setCellValue(item.getName());
                        row.createCell(2).setCellValue(item.getNetProfit().doubleValue());
                        row.createCell(3).setCellValue(formatPercent(item.getGrowth()));
                    }
                }
            }
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createAlertSheet(Workbook workbook, DashboardDTO data) {
        Sheet sheet = workbook.createSheet("异常预警");
        Row titleRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(workbook);
        String[] headers = {"预警等级", "预警类型", "预警内容", "异常幅度", "涉及维度", "处理状态"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = titleRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        if (data.getAlerts() != null) {
            for (DashboardDTO.AlertDTO alert : data.getAlerts()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(alert.getLevel());
                row.createCell(1).setCellValue(alert.getAlertType());
                row.createCell(2).setCellValue(alert.getContent());
                row.createCell(3).setCellValue(alert.getAnomalyValue() != null ?
                    alert.getAnomalyValue().doubleValue() : 0);
                row.createCell(4).setCellValue(alert.getDimName());
                row.createCell(5).setCellValue(alert.getStatus());
            }
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private String formatPercent(BigDecimal value) {
        if (value == null) return "-";
        return value.setScale(2, BigDecimal.ROUND_HALF_UP) + "%";
    }
}
