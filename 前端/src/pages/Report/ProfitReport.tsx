import React, { useState, useEffect, useCallback } from 'react';
import { Card, Table, Button, Space, Select, Tag, Tabs, message } from 'antd';
import { DownloadOutlined, PrinterOutlined, ReloadOutlined } from '@ant-design/icons';
import api from '../../services/api';

const { Option } = Select;

const reportTypes = [
  { key: 'org', label: '机构利润表', api: '/report/profit/org' },
  { key: 'product', label: '产品损益表', api: '/report/profit/product' },
  { key: 'manager', label: '客户经理绩效表', api: '/report/profit/manager' },
];

const ProfitReport: React.FC = () => {
  const [reportType, setReportType] = useState('org');
  const [period, setPeriod] = useState('2026-05');
  const [caliber, setCaliber] = useState('BOOK');
  const [data, setData] = useState<any[]>([]);
  const [total, setTotal] = useState<any>(null);
  const [loading, setLoading] = useState(false);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const apiPath = reportTypes.find(t => t.key === reportType)?.api || '/report/profit/org';
      const result: any = await api.get(apiPath, { params: { period, caliberType: caliber } });
      setData(result?.list || []);
      setTotal(result?.total || null);
    } catch (error) {
      console.error('Failed to fetch report:', error);
    } finally {
      setLoading(false);
    }
  }, [reportType, period, caliber]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleExport = async () => {
    try {
      const exportApi = reportType === 'org' ? '/export/profit/org' : '/export/profit/product';
      const response = await fetch(`/api${exportApi}?period=${period}&caliberType=${caliber}`);
      if (!response.ok) throw new Error('导出失败');

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${reportType}_profit_${period}.xlsx`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      message.success('导出成功');
    } catch (error) {
      message.error('导出失败');
    }
  };

  const orgColumns = [
    { title: '机构名称', dataIndex: 'name', fixed: 'left' as const, width: 120 },
    { title: '业务收入', dataIndex: 'revenue', render: (v: number) => v?.toLocaleString() },
    { title: '利息收入', dataIndex: 'interest_income', render: (v: number) => v?.toLocaleString() },
    { title: '手续费收入', dataIndex: 'fee_income', render: (v: number) => v?.toLocaleString() },
    { title: 'FTP成本', dataIndex: 'ftp_cost', render: (v: number) => v?.toLocaleString() },
    { title: '风险成本', dataIndex: 'risk_cost', render: (v: number) => v?.toLocaleString() },
    { title: '运营成本', dataIndex: 'op_cost', render: (v: number) => v?.toLocaleString() },
    { title: '净利润', dataIndex: 'net_profit', render: (v: number) => (
      <span style={{ color: v >= 0 ? '#52c41a' : '#f5222d', fontWeight: 'bold' }}>{v?.toLocaleString()}</span>
    )},
    { title: '成本收入比', dataIndex: 'cost_income_ratio', render: (v: number) => v ? v + '%' : '-' },
    { title: '利润率', dataIndex: 'profit_margin', render: (v: number) => v ? v + '%' : '-' },
  ];

  const productColumns = [
    { title: '产品名称', dataIndex: 'name', fixed: 'left' as const, width: 150 },
    { title: '业务规模', dataIndex: 'biz_amount', render: (v: number) => v?.toLocaleString() },
    { title: '业务收入', dataIndex: 'revenue', render: (v: number) => v?.toLocaleString() },
    { title: 'FTP成本', dataIndex: 'ftp_cost', render: (v: number) => v?.toLocaleString() },
    { title: '风险成本', dataIndex: 'risk_cost', render: (v: number) => v?.toLocaleString() },
    { title: '运营成本', dataIndex: 'op_cost', render: (v: number) => v?.toLocaleString() },
    { title: '净利润', dataIndex: 'net_profit', render: (v: number) => (
      <span style={{ color: v >= 0 ? '#52c41a' : '#f5222d', fontWeight: 'bold' }}>{v?.toLocaleString()}</span>
    )},
  ];

  const managerColumns = [
    { title: '客户经理', dataIndex: 'name', width: 100 },
    { title: '所属机构', dataIndex: 'org_name', width: 120 },
    { title: '客户数', dataIndex: 'customer_cnt', width: 80 },
    { title: '业务收入', dataIndex: 'revenue', render: (v: number) => v?.toLocaleString() },
    { title: '净利润', dataIndex: 'net_profit', render: (v: number) => (
      <span style={{ color: v >= 0 ? '#52c41a' : '#f5222d', fontWeight: 'bold' }}>{v?.toLocaleString()}</span>
    )},
  ];

  const getColumns = () => {
    switch (reportType) {
      case 'product': return productColumns;
      case 'manager': return managerColumns;
      default: return orgColumns;
    }
  };

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>📊 盈利报表查询</h2>
        <p style={{ color: '#999', margin: '4px 0 0' }}>查看标准盈利报表，支持导出</p>
      </div>

      <Card>
        <div style={{ marginBottom: 16, display: 'flex', gap: 12, alignItems: 'center' }}>
          <Select value={period} onChange={setPeriod} style={{ width: 130 }}>
            <Option value="2026-05">2026年5月</Option>
            <Option value="2026-04">2026年4月</Option>
            <Option value="2026-03">2026年3月</Option>
          </Select>
          <Select value={caliber} onChange={setCaliber} style={{ width: 120 }}>
            <Option value="BOOK">账面口径</Option>
            <Option value="ASSESS">考核口径</Option>
          </Select>
          <Button icon={<ReloadOutlined />} onClick={fetchData}>刷新</Button>
          <Button icon={<DownloadOutlined />} type="primary" onClick={handleExport}>导出Excel</Button>
          <Button icon={<PrinterOutlined />}>打印</Button>
        </div>

        <Tabs
          activeKey={reportType}
          onChange={setReportType}
          items={reportTypes.map(t => ({ key: t.key, label: t.label }))}
        />

        <Table
          dataSource={total ? [...data, { ...total, name: '合计', isTotal: true }] : data}
          columns={getColumns()}
          rowKey="name"
          loading={loading}
          size="small"
          scroll={{ x: 1200 }}
          pagination={false}
          rowClassName={(record) => record.isTotal ? 'ant-table-summary' : ''}
        />
      </Card>
    </div>
  );
};

export default ProfitReport;
