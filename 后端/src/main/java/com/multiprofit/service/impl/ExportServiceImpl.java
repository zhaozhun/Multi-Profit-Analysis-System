package com.multiprofit.service.impl;

import com.multiprofit.service.ExportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Service
public class ExportServiceImpl implements ExportService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public byte[] exportDashboardToExcel(String period, String caliberType) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Sheet 1: KPI汇总
            Sheet kpiSheet = workbook.createSheet("KPI汇总");
            createKpiSheet(kpiSheet, period, caliberType);

            // Sheet 2: 机构排名
            Sheet orgSheet = workbook.createSheet("机构排名");
            createOrgRankingSheet(orgSheet, period, caliberType);

            // Sheet 3: 产品排名
            Sheet prodSheet = workbook.createSheet("产品排名");
            createProductRankingSheet(prodSheet, period, caliberType);

            return workbookToBytes(workbook);
        }
    }

    @Override
    public byte[] exportDimensionToExcel(String dimType, String startDate, String endDate, String caliberType) throws Exception {
        String idCol = getDimIdColumn(dimType);
        String dimLabel = getDimLabel(dimType);

        String sql = String.format("""
            SELECT dm.name, dm.code, dm.level,
              sum(bl.revenue) as revenue, sum(bl.ftp_cost) as ftp_cost,
              sum(bl.risk_cost) as risk_cost, sum(bl.op_cost) as op_cost,
              sum(bl.net_profit) as net_profit,
              sum(bl.loan_profit) as loan_profit, sum(bl.deposit_profit) as deposit_profit
            FROM biz_ledger bl
            JOIN dimension_master dm ON bl.%s = dm.id
            WHERE bl.stat_date >= ? AND bl.stat_date <= ? AND bl.caliber_type = ?
            GROUP BY dm.name, dm.code, dm.level
            ORDER BY dm.level, net_profit DESC
            """, idCol);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, startDate, endDate, caliberType);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(dimLabel + "分析");
            createDimensionSheet(sheet, rows, dimLabel);
            return workbookToBytes(workbook);
        }
    }

    @Override
    public byte[] exportLedgerToExcel(String period, String orgName, String productName) throws Exception {
        StringBuilder sql = new StringBuilder(
            "SELECT bl.biz_id, bl.stat_date, bl.account_period, " +
            "org.name as org_name, prod.name as product_name, biz_line.name as biz_line_name, " +
            "ch.name as channel_name, mgr.name as manager_name, " +
            "bl.biz_amount, bl.revenue, bl.interest_income, " +
            "bl.fee_income, bl.ftp_cost, bl.risk_cost, bl.op_cost, bl.net_profit " +
            "FROM biz_ledger bl " +
            "LEFT JOIN dimension_master org ON bl.org_id = org.id " +
            "LEFT JOIN dimension_master prod ON bl.product_id = prod.id " +
            "LEFT JOIN dimension_master biz_line ON bl.biz_line_id = biz_line.id " +
            "LEFT JOIN dimension_master ch ON bl.channel_id = ch.id " +
            "LEFT JOIN dimension_master mgr ON bl.manager_id = mgr.id " +
            "WHERE bl.account_period = ?"
        );

        if (orgName != null && !orgName.isEmpty()) {
            sql.append(" AND org.name = ?");
        }
        if (productName != null && !productName.isEmpty()) {
            sql.append(" AND prod.name = ?");
        }

        sql.append(" ORDER BY bl.net_profit DESC");

        List<Map<String, Object>> rows;
        if (orgName != null && !orgName.isEmpty() && productName != null && !productName.isEmpty()) {
            rows = jdbcTemplate.queryForList(sql.toString(), period, orgName, productName);
        } else if (orgName != null && !orgName.isEmpty()) {
            rows = jdbcTemplate.queryForList(sql.toString(), period, orgName);
        } else if (productName != null && !productName.isEmpty()) {
            rows = jdbcTemplate.queryForList(sql.toString(), period, productName);
        } else {
            rows = jdbcTemplate.queryForList(sql.toString(), period);
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("明细台账");
            createLedgerSheet(sheet, rows);
            return workbookToBytes(workbook);
        }
    }

    @Override
    public byte[] exportReportToExcel(String period, String caliberType) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Sheet 1: 机构利润
            Sheet orgSheet = workbook.createSheet("机构利润");
            createOrgProfitSheet(orgSheet, period, caliberType);

            // Sheet 2: 产品损益
            Sheet prodSheet = workbook.createSheet("产品损益");
            createProductProfitSheet(prodSheet, period, caliberType);

            // Sheet 3: 客户经理绩效
            Sheet mgrSheet = workbook.createSheet("客户经理绩效");
            createManagerProfitSheet(mgrSheet, period, caliberType);

            return workbookToBytes(workbook);
        }
    }

    @Override
    public byte[] exportOrgProfitToExcel(String period, String caliberType) throws Exception {
        String sql = String.format(
            "SELECT org.name as name, " +
            "sum(bl.revenue) as revenue, sum(bl.interest_income) as interest_income, " +
            "sum(bl.fee_income) as fee_income, sum(bl.non_interest_income) as non_interest_income, " +
            "sum(bl.ftp_cost) as ftp_cost, sum(bl.risk_cost) as risk_cost, sum(bl.op_cost) as op_cost, " +
            "sum(bl.net_profit) as net_profit, " +
            "CASE WHEN sum(bl.revenue) > 0 THEN round(sum(bl.ftp_cost+bl.risk_cost+bl.op_cost)*100.0/sum(bl.revenue),2) ELSE 0 end as cost_income_ratio, " +
            "CASE WHEN sum(bl.revenue) > 0 THEN round(sum(bl.net_profit)*100.0/sum(bl.revenue),2) ELSE 0 end as profit_margin " +
            "FROM biz_ledger bl " +
            "LEFT JOIN dimension_master org ON bl.org_id = org.id " +
            "WHERE bl.account_period = '%s' AND bl.caliber_type = '%s' " +
            "GROUP BY org.name ORDER BY net_profit DESC",
            period, caliberType
        );

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("机构利润报表");
            createOrgProfitDetailSheet(sheet, rows);
            return workbookToBytes(workbook);
        }
    }

    @Override
    public byte[] exportProductProfitToExcel(String period, String caliberType) throws Exception {
        String sql = String.format(
            "SELECT prod.name as name, " +
            "sum(bl.biz_amount) as biz_amount, sum(bl.revenue) as revenue, " +
            "sum(bl.ftp_cost) as ftp_cost, sum(bl.risk_cost) as risk_cost, sum(bl.op_cost) as op_cost, " +
            "sum(bl.net_profit) as net_profit " +
            "FROM biz_ledger bl " +
            "LEFT JOIN dimension_master prod ON bl.product_id = prod.id " +
            "WHERE bl.account_period = '%s' AND bl.caliber_type = '%s' " +
            "GROUP BY prod.name ORDER BY net_profit DESC",
            period, caliberType
        );

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("产品损益报表");
            createProductProfitDetailSheet(sheet, rows);
            return workbookToBytes(workbook);
        }
    }

    // === Helper Methods ===

    private void createKpiSheet(Sheet sheet, String period, String caliberType) {
        String sql = String.format(
            "SELECT " +
            "sum(revenue) as revenue, sum(ftp_cost) as ftp_cost, sum(risk_cost) as risk_cost, " +
            "sum(op_cost) as op_cost, sum(net_profit) as net_profit, " +
            "sum(loan_revenue) as loan_revenue, sum(loan_profit) as loan_profit, " +
            "sum(deposit_revenue) as deposit_revenue, sum(deposit_profit) as deposit_profit " +
            "FROM biz_ledger WHERE account_period = '%s' AND caliber_type = '%s'",
            period, caliberType
        );

        Map<String, Object> data = jdbcTemplate.queryForMap(sql);

        // 创建表头
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("指标");
        headerRow.createCell(1).setCellValue("金额（万元）");

        // 设置表头样式
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerRow.getCell(0).setCellStyle(headerStyle);
        headerRow.getCell(1).setCellStyle(headerStyle);

        // 填充数据
        int rowIdx = 1;
        createKpiRow(sheet, rowIdx++, "总收入", data.get("revenue"));
        createKpiRow(sheet, rowIdx++, "FTP成本", data.get("ftp_cost"));
        createKpiRow(sheet, rowIdx++, "风险成本", data.get("risk_cost"));
        createKpiRow(sheet, rowIdx++, "运营成本", data.get("op_cost"));
        createKpiRow(sheet, rowIdx++, "净利润", data.get("net_profit"));
        createKpiRow(sheet, rowIdx++, "贷款收入", data.get("loan_revenue"));
        createKpiRow(sheet, rowIdx++, "贷款利润", data.get("loan_profit"));
        createKpiRow(sheet, rowIdx++, "存款收入", data.get("deposit_revenue"));
        createKpiRow(sheet, rowIdx++, "存款利润", data.get("deposit_profit"));

        // 调整列宽
        sheet.setColumnWidth(0, 3000);
        sheet.setColumnWidth(1, 4000);
    }

    private void createKpiRow(Sheet sheet, int rowIdx, String name, Object value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(name);
        row.createCell(1).setCellValue(toDouble(value));
    }

    private void createOrgRankingSheet(Sheet sheet, String period, String caliberType) {
        String sql = String.format(
            "SELECT org.name as name, sum(bl.net_profit) as net_profit " +
            "FROM biz_ledger bl " +
            "LEFT JOIN dimension_master org ON bl.org_id = org.id " +
            "WHERE bl.account_period = '%s' AND bl.caliber_type = '%s' " +
            "GROUP BY org.name ORDER BY net_profit DESC LIMIT 10",
            period, caliberType
        );

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("排名");
        headerRow.createCell(1).setCellValue("机构");
        headerRow.createCell(2).setCellValue("净利润（万元）");

        int rank = 1;
        for (int i = 0; i < rows.size(); i++) {
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(rank++);
            row.createCell(1).setCellValue(String.valueOf(rows.get(i).get("name")));
            row.createCell(2).setCellValue(toDouble(rows.get(i).get("net_profit")));
        }

        sheet.setColumnWidth(0, 1500);
        sheet.setColumnWidth(1, 4000);
        sheet.setColumnWidth(2, 4000);
    }

    private void createProductRankingSheet(Sheet sheet, String period, String caliberType) {
        String sql = String.format(
            "SELECT prod.name as name, sum(bl.net_profit) as net_profit " +
            "FROM biz_ledger bl " +
            "LEFT JOIN dimension_master prod ON bl.product_id = prod.id " +
            "WHERE bl.account_period = '%s' AND bl.caliber_type = '%s' " +
            "GROUP BY prod.name ORDER BY net_profit DESC LIMIT 10",
            period, caliberType
        );

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("排名");
        headerRow.createCell(1).setCellValue("产品");
        headerRow.createCell(2).setCellValue("净利润（万元）");

        int rank = 1;
        for (int i = 0; i < rows.size(); i++) {
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(rank++);
            row.createCell(1).setCellValue(String.valueOf(rows.get(i).get("name")));
            row.createCell(2).setCellValue(toDouble(rows.get(i).get("net_profit")));
        }

        sheet.setColumnWidth(0, 1500);
        sheet.setColumnWidth(1, 4000);
        sheet.setColumnWidth(2, 4000);
    }

    private void createDimensionSheet(Sheet sheet, List<Map<String, Object>> rows, String dimLabel) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"名称", "编码", "层级", "总收入", "FTP成本", "风险成本", "运营成本", "净利润", "贷款利润", "存款利润"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        for (int i = 0; i < headers.length; i++) {
            headerRow.getCell(i).setCellStyle(headerStyle);
        }

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            Row dataRow = sheet.createRow(i + 1);
            dataRow.createCell(0).setCellValue(String.valueOf(row.get("name")));
            dataRow.createCell(1).setCellValue(String.valueOf(row.get("code")));
            dataRow.createCell(2).setCellValue(((Number) row.get("level")).intValue());
            dataRow.createCell(3).setCellValue(toDouble(row.get("revenue")));
            dataRow.createCell(4).setCellValue(toDouble(row.get("ftp_cost")));
            dataRow.createCell(5).setCellValue(toDouble(row.get("risk_cost")));
            dataRow.createCell(6).setCellValue(toDouble(row.get("op_cost")));
            dataRow.createCell(7).setCellValue(toDouble(row.get("net_profit")));
            dataRow.createCell(8).setCellValue(toDouble(row.get("loan_profit")));
            dataRow.createCell(9).setCellValue(toDouble(row.get("deposit_profit")));
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 3000);
        }
    }

    private void createLedgerSheet(Sheet sheet, List<Map<String, Object>> rows) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"业务编号", "统计日期", "账期", "机构", "产品", "条线", "渠道", "客户经理", "业务金额", "收入", "利息收入", "手续费收入", "FTP成本", "风险成本", "运营成本", "净利润"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        for (int i = 0; i < headers.length; i++) {
            headerRow.getCell(i).setCellStyle(headerStyle);
        }

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            Row dataRow = sheet.createRow(i + 1);
            dataRow.createCell(0).setCellValue(String.valueOf(row.get("biz_id")));
            dataRow.createCell(1).setCellValue(String.valueOf(row.get("stat_date")));
            dataRow.createCell(2).setCellValue(String.valueOf(row.get("account_period")));
            dataRow.createCell(3).setCellValue(String.valueOf(row.get("org_name")));
            dataRow.createCell(4).setCellValue(String.valueOf(row.get("product_name")));
            dataRow.createCell(5).setCellValue(String.valueOf(row.get("biz_line_name")));
            dataRow.createCell(6).setCellValue(String.valueOf(row.get("channel_name")));
            dataRow.createCell(7).setCellValue(String.valueOf(row.get("manager_name")));
            dataRow.createCell(8).setCellValue(toDouble(row.get("biz_amount")));
            dataRow.createCell(9).setCellValue(toDouble(row.get("revenue")));
            dataRow.createCell(10).setCellValue(toDouble(row.get("interest_income")));
            dataRow.createCell(11).setCellValue(toDouble(row.get("fee_income")));
            dataRow.createCell(12).setCellValue(toDouble(row.get("ftp_cost")));
            dataRow.createCell(13).setCellValue(toDouble(row.get("risk_cost")));
            dataRow.createCell(14).setCellValue(toDouble(row.get("op_cost")));
            dataRow.createCell(15).setCellValue(toDouble(row.get("net_profit")));
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 3000);
        }
    }

    private void createOrgProfitSheet(Sheet sheet, String period, String caliberType) {
        String sql = String.format(
            "SELECT org.name as name, " +
            "sum(bl.revenue) as revenue, sum(bl.ftp_cost) as ftp_cost, " +
            "sum(bl.risk_cost) as risk_cost, sum(bl.op_cost) as op_cost, " +
            "sum(bl.net_profit) as net_profit " +
            "FROM biz_ledger bl " +
            "LEFT JOIN dimension_master org ON bl.org_id = org.id " +
            "WHERE bl.account_period = '%s' AND bl.caliber_type = '%s' " +
            "GROUP BY org.name ORDER BY net_profit DESC",
            period, caliberType
        );

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        Row headerRow = sheet.createRow(0);
        String[] headers = {"机构", "总收入", "FTP成本", "风险成本", "运营成本", "净利润"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            Row dataRow = sheet.createRow(i + 1);
            dataRow.createCell(0).setCellValue(String.valueOf(row.get("name")));
            dataRow.createCell(1).setCellValue(toDouble(row.get("revenue")));
            dataRow.createCell(2).setCellValue(toDouble(row.get("ftp_cost")));
            dataRow.createCell(3).setCellValue(toDouble(row.get("risk_cost")));
            dataRow.createCell(4).setCellValue(toDouble(row.get("op_cost")));
            dataRow.createCell(5).setCellValue(toDouble(row.get("net_profit")));
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 3000);
        }
    }

    private void createProductProfitSheet(Sheet sheet, String period, String caliberType) {
        String sql = String.format(
            "SELECT prod.name as name, " +
            "sum(bl.biz_amount) as biz_amount, sum(bl.revenue) as revenue, " +
            "sum(bl.ftp_cost) as ftp_cost, sum(bl.risk_cost) as risk_cost, " +
            "sum(bl.op_cost) as op_cost, sum(bl.net_profit) as net_profit " +
            "FROM biz_ledger bl " +
            "LEFT JOIN dimension_master prod ON bl.product_id = prod.id " +
            "WHERE bl.account_period = '%s' AND bl.caliber_type = '%s' " +
            "GROUP BY prod.name ORDER BY net_profit DESC",
            period, caliberType
        );

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        Row headerRow = sheet.createRow(0);
        String[] headers = {"产品", "业务金额", "收入", "FTP成本", "风险成本", "运营成本", "净利润"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            Row dataRow = sheet.createRow(i + 1);
            dataRow.createCell(0).setCellValue(String.valueOf(row.get("name")));
            dataRow.createCell(1).setCellValue(toDouble(row.get("biz_amount")));
            dataRow.createCell(2).setCellValue(toDouble(row.get("revenue")));
            dataRow.createCell(3).setCellValue(toDouble(row.get("ftp_cost")));
            dataRow.createCell(4).setCellValue(toDouble(row.get("risk_cost")));
            dataRow.createCell(5).setCellValue(toDouble(row.get("op_cost")));
            dataRow.createCell(6).setCellValue(toDouble(row.get("net_profit")));
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 3000);
        }
    }

    private void createManagerProfitSheet(Sheet sheet, String period, String caliberType) {
        String sql = String.format(
            "SELECT mgr.name as name, org.name as org_name, " +
            "sum(bl.revenue) as revenue, sum(bl.net_profit) as net_profit " +
            "FROM biz_ledger bl " +
            "LEFT JOIN dimension_master mgr ON bl.manager_id = mgr.id " +
            "LEFT JOIN dimension_master org ON bl.org_id = org.id " +
            "WHERE bl.account_period = '%s' AND bl.caliber_type = '%s' " +
            "GROUP BY mgr.name, org.name ORDER BY net_profit DESC",
            period, caliberType
        );

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        Row headerRow = sheet.createRow(0);
        String[] headers = {"客户经理", "所属机构", "收入", "净利润"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            Row dataRow = sheet.createRow(i + 1);
            dataRow.createCell(0).setCellValue(String.valueOf(row.get("name")));
            dataRow.createCell(1).setCellValue(String.valueOf(row.get("org_name")));
            dataRow.createCell(2).setCellValue(toDouble(row.get("revenue")));
            dataRow.createCell(3).setCellValue(toDouble(row.get("net_profit")));
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 3000);
        }
    }

    private void createOrgProfitDetailSheet(Sheet sheet, List<Map<String, Object>> rows) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"机构", "总收入", "利息收入", "手续费收入", "非利息收入", "FTP成本", "风险成本", "运营成本", "净利润", "成本收入比", "利润率"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            Row dataRow = sheet.createRow(i + 1);
            dataRow.createCell(0).setCellValue(String.valueOf(row.get("name")));
            dataRow.createCell(1).setCellValue(toDouble(row.get("revenue")));
            dataRow.createCell(2).setCellValue(toDouble(row.get("interest_income")));
            dataRow.createCell(3).setCellValue(toDouble(row.get("fee_income")));
            dataRow.createCell(4).setCellValue(toDouble(row.get("non_interest_income")));
            dataRow.createCell(5).setCellValue(toDouble(row.get("ftp_cost")));
            dataRow.createCell(6).setCellValue(toDouble(row.get("risk_cost")));
            dataRow.createCell(7).setCellValue(toDouble(row.get("op_cost")));
            dataRow.createCell(8).setCellValue(toDouble(row.get("net_profit")));
            dataRow.createCell(9).setCellValue(toDouble(row.get("cost_income_ratio")));
            dataRow.createCell(10).setCellValue(toDouble(row.get("profit_margin")));
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 3000);
        }
    }

    private void createProductProfitDetailSheet(Sheet sheet, List<Map<String, Object>> rows) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"产品", "业务金额", "收入", "FTP成本", "风险成本", "运营成本", "净利润"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            Row dataRow = sheet.createRow(i + 1);
            dataRow.createCell(0).setCellValue(String.valueOf(row.get("name")));
            dataRow.createCell(1).setCellValue(toDouble(row.get("biz_amount")));
            dataRow.createCell(2).setCellValue(toDouble(row.get("revenue")));
            dataRow.createCell(3).setCellValue(toDouble(row.get("ftp_cost")));
            dataRow.createCell(4).setCellValue(toDouble(row.get("risk_cost")));
            dataRow.createCell(5).setCellValue(toDouble(row.get("op_cost")));
            dataRow.createCell(6).setCellValue(toDouble(row.get("net_profit")));
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 3000);
        }
    }

    private byte[] workbookToBytes(Workbook workbook) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        return outputStream.toByteArray();
    }

    private String getDimIdColumn(String dimType) {
        return switch (dimType) {
            case "ORG" -> "org_id";
            case "BIZ_LINE" -> "biz_line_id";
            case "DEPT" -> "dept_id";
            case "PRODUCT" -> "product_id";
            case "CHANNEL" -> "channel_id";
            case "MANAGER" -> "manager_id";
            case "CUSTOMER" -> "customer_id";
            default -> "org_id";
        };
    }

    private String getDimLabel(String dimType) {
        return switch (dimType) {
            case "ORG" -> "机构";
            case "BIZ_LINE" -> "条线";
            case "DEPT" -> "部门";
            case "PRODUCT" -> "产品";
            case "CHANNEL" -> "渠道";
            case "MANAGER" -> "客户经理";
            case "CUSTOMER" -> "客户";
            default -> "维度";
        };
    }

    private double toDouble(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0; }
    }
}
