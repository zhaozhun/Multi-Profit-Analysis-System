package com.multiprofit.service;

public interface ExportService {

    byte[] exportDashboardToExcel(String period, String caliberType) throws Exception;

    byte[] exportDimensionToExcel(String dimType, String startDate, String endDate, String caliberType) throws Exception;

    byte[] exportLedgerToExcel(String period, String orgName, String productName) throws Exception;

    byte[] exportReportToExcel(String period, String caliberType) throws Exception;

    byte[] exportOrgProfitToExcel(String period, String caliberType) throws Exception;

    byte[] exportProductProfitToExcel(String period, String caliberType) throws Exception;
}
