import React, { useState, useEffect } from 'react';
import { Card, Table, Select, Space, Row, Col, message } from 'antd';
import ReactECharts from 'echarts-for-react';
import {
  getLoanIndicatorSummary,
  getLoanIndicatorDetailList,
  getLoanIndicatorByDimension,
} from '../../../services/indicatorApi';

const { Option } = Select;

const MONTH_OPTIONS = [
  { value: '2025-01', label: '2025年1月' },
  { value: '2025-02', label: '2025年2月' },
  { value: '2025-03', label: '2025年3月' },
  { value: '2025-04', label: '2025年4月' },
  { value: '2025-05', label: '2025年5月' },
  { value: '2025-06', label: '2025年6月' },
  { value: '2025-07', label: '2025年7月' },
  { value: '2025-08', label: '2025年8月' },
  { value: '2025-09', label: '2025年9月' },
  { value: '2025-10', label: '2025年10月' },
  { value: '2025-11', label: '2025年11月' },
  { value: '2025-12', label: '2025年12月' },
  { value: '2026-01', label: '2026年1月' },
  { value: '2026-02', label: '2026年2月' },
  { value: '2026-03', label: '2026年3月' },
  { value: '2026-04', label: '2026年4月' },
  { value: '2026-05', label: '2026年5月' },
  { value: '2026-06', label: '2026年6月' },
];

const DIMENSIONS = [
  { value: 'ORG', label: '机构' },
  { value: 'BIZ_LINE', label: '条线' },
  { value: 'PRODUCT', label: '产品' },
  { value: 'CHANNEL', label: '渠道' },
  { value: 'MANAGER', label: '客户经理' },
  { value: 'CUSTOMER', label: '客户' },
];

const AssetRiskCost: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [period, setPeriod] = useState('2026-06');
  const [caliberType, setCaliberType] = useState('ASSESS');
  const [dimension, setDimension] = useState('ORG');
  const [dimensionValue, setDimensionValue] = useState<string | undefined>(undefined);
  const [summary, setSummary] = useState<Record<string, any>>({});
  const [detailList, setDetailList] = useState<any[]>([]);
  const [dimensionData, setDimensionData] = useState<any[]>([]);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });

  useEffect(() => {
    loadData();
  }, [period, caliberType, dimension, dimensionValue]);

  const loadData = async () => {
    setLoading(true);
    try {
      const [summaryRes, dimensionRes, detailRes] = await Promise.all([
        getLoanIndicatorSummary(period, caliberType),
        getLoanIndicatorByDimension(period, caliberType, dimension),
        getLoanIndicatorDetailList(period, caliberType, dimension, dimensionValue, 1, 20),
      ]);
      setSummary(summaryRes || {});
      setDimensionData(dimensionRes || []);
      setDetailList(detailRes?.list || []);
      setPagination({ ...pagination, total: detailRes?.total || 0, current: 1 });
    } catch (error) {
      message.error('加载数据失败');
    } finally {
      setLoading(false);
    }
  };

  const handlePageChange = async (page: number, pageSize: number) => {
    setLoading(true);
    try {
      const detailRes = await getLoanIndicatorDetailList(period, caliberType, dimension, dimensionValue, page, pageSize);
      setDetailList(detailRes?.list || []);
      setPagination({ ...pagination, current: page, pageSize, total: detailRes?.total || 0 });
    } catch (error) {
      message.error('获取明细数据失败');
    } finally {
      setLoading(false);
    }
  };

  // 柱状图配置
  const getBarChartOption = () => {
    const names = dimensionData.map(item => item.dim_name);
    const balances = dimensionData.map(item => (item.total_balance / 10000).toFixed(2));
    const riskCosts = dimensionData.map(item => (item.total_risk_cost / 10000).toFixed(2));

    return {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { data: ['余额(万)', '风险成本(万)'] },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: names, axisLabel: { rotate: 30 } },
      yAxis: [
        { type: 'value', name: '余额(万)', position: 'left' },
        { type: 'value', name: '风险成本(万)', position: 'right' },
      ],
      series: [
        { name: '余额(万)', type: 'bar', data: balances, itemStyle: { color: '#1890ff' } },
        { name: '风险成本(万)', type: 'bar', yAxisIndex: 1, data: riskCosts, itemStyle: { color: '#faad14' } },
      ],
    };
  };

  // 饼图配置
  const getPieChartOption = () => {
    const data = dimensionData.map(item => ({ name: item.dim_name, value: item.total_risk_cost }));
    return {
      tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c} ({d}%)' },
      legend: { orient: 'vertical', left: 'left' },
      series: [{ name: '风险成本', type: 'pie', radius: '50%', data: data }],
    };
  };

  const summaryData = [{
    key: 'summary',
    riskCost: summary.total_risk_cost || 0,
    riskRatio: summary.total_balance ? (summary.total_risk_cost / summary.total_balance * 100) : 0,
    balance: summary.total_balance || 0,
  }];

  const summaryColumns = [
    { title: '风险成本合计', dataIndex: 'riskCost', key: 'riskCost', width: 150, render: (val: number) => (val / 10000).toFixed(2) + ' 万' },
    { title: '风险成本率', dataIndex: 'riskRatio', key: 'riskRatio', width: 120, render: (val: number) => val.toFixed(2) + '%' },
    { title: '在贷余额', dataIndex: 'balance', key: 'balance', width: 150, render: (val: number) => (val / 10000).toFixed(2) + ' 万' },
  ];

  const dimensionColumns = [
    { title: '维度名称', dataIndex: 'dim_name', key: 'dim_name', width: 120 },
    { title: '数量', dataIndex: 'count', key: 'count', width: 80 },
    { title: '余额合计', dataIndex: 'total_balance', key: 'total_balance', width: 120, render: (val: number) => val ? (val / 10000).toFixed(2) + '万' : '-' },
    { title: '风险成本合计', dataIndex: 'total_risk_cost', key: 'total_risk_cost', width: 130, render: (val: number) => val ? (val / 10000).toFixed(2) + '万' : '-' },
  ];

  const detailColumns = [
    { title: '业务编号', dataIndex: 'bizId', key: 'bizId', width: 120 },
    { title: '客户名称', dataIndex: 'customerName', key: 'customerName', width: 120 },
    { title: '机构', dataIndex: 'orgName', key: 'orgName', width: 100 },
    { title: '条线', dataIndex: 'bizLineName', key: 'bizLineName', width: 100 },
    { title: '产品', dataIndex: 'productName', key: 'productName', width: 100 },
    { title: '在贷余额', dataIndex: 'loanBalance', key: 'loanBalance', width: 120, render: (val: number) => val ? (val / 10000).toFixed(2) + '万' : '-' },
    { title: '风险成本', dataIndex: 'riskCost', key: 'riskCost', width: 100, render: (val: number) => val ? (val / 10000).toFixed(2) + '万' : '-' },
  ];

  const getDimensionValueOptions = () => dimensionData.map(item => ({ value: item.dim_name, label: item.dim_name }));

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>风险成本</h2>
        <p style={{ color: '#999', margin: '4px 0 0' }}>资产条线风险计提成本</p>
      </div>

      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <span>账期：</span>
          <Select value={period} onChange={setPeriod} style={{ width: 150 }}>
            {MONTH_OPTIONS.map(opt => <Option key={opt.value} value={opt.value}>{opt.label}</Option>)}
          </Select>
          <span>口径：</span>
          <Select value={caliberType} onChange={setCaliberType} style={{ width: 100 }}>
            <Option value="BOOK">账面</Option>
            <Option value="ASSESS">考核</Option>
          </Select>
          <span>维度：</span>
          <Select value={dimension} onChange={setDimension} style={{ width: 100 }}>
            {DIMENSIONS.map(dim => <Option key={dim.value} value={dim.value}>{dim.label}</Option>)}
          </Select>
          <span>维度值：</span>
          <Select value={dimensionValue} onChange={setDimensionValue} style={{ width: 150 }} allowClear placeholder="全部">
            {getDimensionValueOptions().map(opt => <Option key={opt.value} value={opt.value}>{opt.label}</Option>)}
          </Select>
        </Space>
      </Card>

      <Card loading={loading} style={{ marginBottom: 16 }}>
        <Table dataSource={summaryData} columns={summaryColumns} pagination={false} size="small" scroll={{ x: 500 }} />
      </Card>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={14}>
          <Card title={`${DIMENSIONS.find(d => d.value === dimension)?.label}维度余额与风险成本分布`}>
            <ReactECharts option={getBarChartOption()} style={{ height: 300 }} />
          </Card>
        </Col>
        <Col span={10}>
          <Card title="风险成本占比">
            <ReactECharts option={getPieChartOption()} style={{ height: 300 }} />
          </Card>
        </Col>
      </Row>

      <Card title={`${DIMENSIONS.find(d => d.value === dimension)?.label}维度汇总`} style={{ marginBottom: 16 }}>
        <Table dataSource={dimensionData} columns={dimensionColumns} rowKey="dim_name" pagination={false} size="small" scroll={{ x: 500 }}
          onRow={record => ({ onClick: () => setDimensionValue(record.dim_name), style: { cursor: 'pointer' } })} />
      </Card>

      <Card title="风险成本明细">
        <Table dataSource={detailList} columns={detailColumns} rowKey="id"
          pagination={{ ...pagination, onChange: handlePageChange, showSizeChanger: true, showTotal: total => `共 ${total} 条` }}
          size="small" scroll={{ x: 1000 }} />
      </Card>
    </div>
  );
};

export default AssetRiskCost;
