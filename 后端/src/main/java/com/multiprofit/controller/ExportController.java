package com.multiprofit.controller;

import com.multiprofit.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Autowired
    private ExportService exportService;

    /**
     * 导出经营驾驶舱数据
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Resource> exportDashboard(
            @RequestParam(required = false, defaultValue = "2026-05") String period,
            @RequestParam(required = false, defaultValue = "BOOK") String caliberType) {

        try {
            byte[] data = exportService.exportDashboardToExcel(period, caliberType);
            ByteArrayResource resource = new ByteArrayResource(data);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=dashboard_" + period + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(data.length)
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 导出维度分析数据
     */
    @GetMapping("/dimension/{dimType}")
    public ResponseEntity<Resource> exportDimension(
            @PathVariable String dimType,
            @RequestParam(required = false, defaultValue = "2026-05-01") String startDate,
            @RequestParam(required = false, defaultValue = "2026-05-31") String endDate,
            @RequestParam(required = false, defaultValue = "BOOK") String caliberType) {

        try {
            byte[] data = exportService.exportDimensionToExcel(dimType, startDate, endDate, caliberType);
            ByteArrayResource resource = new ByteArrayResource(data);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=dimension_" + dimType + "_" + startDate + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(data.length)
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 导出明细台账数据
     */
    @GetMapping("/ledger")
    public ResponseEntity<Resource> exportLedger(
            @RequestParam(required = false, defaultValue = "2026-05") String period,
            @RequestParam(required = false) String orgName,
            @RequestParam(required = false) String productName) {

        try {
            byte[] data = exportService.exportLedgerToExcel(period, orgName, productName);
            ByteArrayResource resource = new ByteArrayResource(data);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=ledger_" + period + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(data.length)
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 导出分析报告
     */
    @GetMapping("/report")
    public ResponseEntity<Resource> exportReport(
            @RequestParam(required = false, defaultValue = "2026-05") String period,
            @RequestParam(required = false, defaultValue = "BOOK") String caliberType) {

        try {
            byte[] data = exportService.exportReportToExcel(period, caliberType);
            ByteArrayResource resource = new ByteArrayResource(data);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=report_" + period + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(data.length)
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 导出机构利润报表
     */
    @GetMapping("/profit/org")
    public ResponseEntity<Resource> exportOrgProfit(
            @RequestParam(required = false, defaultValue = "2026-05") String period,
            @RequestParam(required = false, defaultValue = "BOOK") String caliberType) {

        try {
            byte[] data = exportService.exportOrgProfitToExcel(period, caliberType);
            ByteArrayResource resource = new ByteArrayResource(data);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=org_profit_" + period + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(data.length)
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 导出产品损益报表
     */
    @GetMapping("/profit/product")
    public ResponseEntity<Resource> exportProductProfit(
            @RequestParam(required = false, defaultValue = "2026-05") String period,
            @RequestParam(required = false, defaultValue = "BOOK") String caliberType) {

        try {
            byte[] data = exportService.exportProductProfitToExcel(period, caliberType);
            ByteArrayResource resource = new ByteArrayResource(data);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=product_profit_" + period + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(data.length)
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
