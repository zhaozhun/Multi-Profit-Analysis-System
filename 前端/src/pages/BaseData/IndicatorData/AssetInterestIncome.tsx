import React, { useState, useEffect } from 'react';
import { Card, Table, Select, Space, Row, Col, message } from 'antd';
import ReactECharts from 'echarts-for-react';
import {
  getLoanIndicatorSummary,
  getLoanIndicatorByDimension,
  getLoanIndicatorDetailList,
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

const AssetInterestIncome: React.FC = () => {
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
      console.error('加载数据失败:', error);
      message.error('加载数据失败');
    } finally {
      setLoading(false);
    }
  };

  const handlePageChange = async (page: number, pageSize: number) => {
    // 明细数据暂不支持分页
  };

  // 柱状图配置 - 维度余额分布
  const getBarChartOption = () => {
    const names = dimensionData.map(item => item.dim_name);
    const balances = dimensionData.map(item => (item.total_balance / 10000).toFixed(2));
    const interests = dimensionData.map(item => (item.total_interest / 10000).toFixed(2));

    return {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
      },
      legend: {
        data: ['余额(万)', '利息(万)'],
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        data: names,
        axisLabel: { rotate: 30 },
      },
      yAxis: [
        { type: 'value', name: '余额(万)', position: 'left' },
        { type: 'value', name: '利息(万)', position: 'right' },
      ],
      series: [
        {
          name: '余额(万)',
          type: 'bar',
          data: balances,
          itemStyle: { color: '#1890ff' },
        },
        {
          name: '利息(万)',
          type: 'bar',
          yAxisIndex: 1,
          data: interests,
          itemStyle: { color: '#52c41a' },
        },
      ],
    };
  };

  // 饼图配置 - 利息占比
  const getPieChartOption = () => {
    const data = dimensionData.map(item => ({
      name: item.dim_name,
      value: item.total_interest,
    }));

    return {
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c} ({d}%)',
      },
      legend: {
        orient: 'vertical',
        left: 'left',
      },
      series: [
        {
          name: '利息收入',
          type: 'pie',
          radius: '50%',
          data: data,
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)',
            },
          },
        },
      ],
    };
  };

  // 汇总数据表格
  const summaryData = [{
    key: 'summary',
    balance: summary.total_balance || 0,
    avgRate: summary.avg_rate || 0,
    dailyInterest: summary.total_daily_interest || 0,
    monthlyInterest: summary.total_monthly_interest || 0,
    yearlyInterest: summary.total_cumulative_interest || 0,
    balanceDailyAvg: summary.total_balance ? summary.total_balance / 30 : 0,
    interestDailyAvg: summary.total_monthly_interest ? summary.total_monthly_interest / 30 : 0,
  }];

  const summaryColumns = [
    { title: '在贷余额', dataIndex: 'balance', key: 'balance', width: 150, render: (val: number) => (val / 10000).toFixed(2) + ' 万' },
    { title: '平均利率', dataIndex: 'avgRate', key: 'avgRate', width: 100, render: (val: number) => (val * 100).toFixed(2) + '%' },
    { title: '当日利息收入', dataIndex: 'dailyInterest', key: 'dailyInterest', width: 130, render: (val: number) => (val / 10000).toFixed(4) + ' 万' },
    { title: '当月利息收入', dataIndex: 'monthlyInterest', key: 'monthlyInterest', width: 130, render: (val: number) => (val / 10000).toFixed(2) + ' 万' },
    { title: '当年利息收入', dataIndex: 'yearlyInterest', key: 'yearlyInterest', width: 130, render: (val: number) => (val / 10000).toFixed(2) + ' 万' },
    { title: '余额月日均', dataIndex: 'balanceDailyAvg', key: 'balanceDailyAvg', width: 130, render: (val: number) => (val / 10000).toFixed(2) + ' 万' },
    { title: '利息月日均', dataIndex: 'interestDailyAvg', key: 'interestDailyAvg', width: 130, render: (val: number) => (val / 10000).toFixed(4) + ' 万' },
  ];

  const dimensionColumns = [
    { title: '维度名称', dataIndex: 'dim_name', key: 'dim_name', width: 120 },
    { title: '数量', dataIndex: 'count', key: 'count', width: 80 },
    { title: '余额合计', dataIndex: 'total_balance', key: 'total_balance', width: 120, render: (val: number) => val ? (val / 10000).toFixed(2) + '万' : '-' },
    { title: '平均利率', dataIndex: 'avg_rate', key: 'avg_rate', width: 100, render: (val: number) => val ? (val * 100).toFixed(2) + '%' : '-' },
    { title: '利息合计', dataIndex: 'total_interest', key: 'total_interest', width: 120, render: (val: number) => val ? (val / 10000).toFixed(2) + '万' : '-' },
  ];

  const detailColumns = [
    { title: '业务编号', dataIndex: 'bizId', key: 'bizId', width: 120 },
    { title: '客户名称', dataIndex: 'customerName', key: 'customerName', width: 120 },
    { title: '机构', dataIndex: 'orgName', key: 'orgName', width: 100 },
    { title: '条线', dataIndex: 'bizLineName', key: 'bizLineName', width: 100 },
    { title: '产品', dataIndex: 'productName', key: 'productName', width: 100 },
    { title: '在贷余额', dataIndex: 'loanBalance', key: 'loanBalance', width: 120, render: (val: number) => val ? (val / 10000).toFixed(2) + '万' : '-' },
    { title: '贷款利率', dataIndex: 'loanRate', key: 'loanRate', width: 100, render: (val: number) => val ? (val * 100).toFixed(2) + '%' : '-' },
    { title: '当日利息', dataIndex: 'loanDailyInterest', key: 'loanDailyInterest', width: 100, render: (val: number) => val ? (val / 10000).toFixed(4) + '万' : '-' },
    { title: '当月利息', dataIndex: 'loanMonthlyInterest', key: 'loanMonthlyInterest', width: 100, render: (val: number) => val ? (val / 10000).toFixed(4) + '万' : '-' },
    { title: '累计利息', dataIndex: 'loanCumulativeInterest', key: 'loanCumulativeInterest', width: 100, render: (val: number) => val ? (val / 10000).toFixed(4) + '万' : '-' },
  ];

  const getDimensionValueOptions = () => {
    return dimensionData.map((item) => ({ value: item.dim_name, label: item.dim_name }));
  };

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>利息收入</h2>
        <p style={{ color: '#999', margin: '4px 0 0' }}>资产条线利息收入指标及贷款余额信息</p>
      </div>

      {/* 筛选条件 */}
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <span>账期：</span>
          <Select value={period} onChange={setPeriod} style={{ width: 150 }}>
            {MONTH_OPTIONS.map((opt) => (<Option key={opt.value} value={opt.value}>{opt.label}</Option>))}
          </Select>
          <span>口径：</span>
          <Select value={caliberType} onChange={setCaliberType} style={{ width: 100 }}>
            <Option value="BOOK">账面</Option>
            <Option value="ASSESS">考核</Option>
          </Select>
          <span>维度：</span>
          <Select value={dimension} onChange={setDimension} style={{ width: 100 }}>
            {DIMENSIONS.map((dim) => (<Option key={dim.value} value={dim.value}>{dim.label}</Option>))}
          </Select>
          <span>维度值：</span>
          <Select value={dimensionValue} onChange={setDimensionValue} style={{ width: 150 }} allowClear placeholder="全部">
            {getDimensionValueOptions().map((opt) => (<Option key={opt.value} value={opt.value}>{opt.label}</Option>))}
          </Select>
        </Space>
      </Card>

      {/* 汇总数据 */}
      <Card loading={loading} style={{ marginBottom: 16 }}>
        <Table dataSource={summaryData} columns={summaryColumns} pagination={false} size="small" scroll={{ x: 1000 }} />
      </Card>

      {/* 图表区域 */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={14}>
          <Card title={`${DIMENSIONS.find(d => d.value === dimension)?.label}维度余额与利息分布`}>
            <ReactECharts option={getBarChartOption()} style={{ height: 300 }} />
          </Card>
        </Col>
        <Col span={10}>
          <Card title="利息收入占比">
            <ReactECharts option={getPieChartOption()} style={{ height: 300 }} />
          </Card>
        </Col>
      </Row>

      {/* 维度汇总表格 */}
      <Card title={`${DIMENSIONS.find(d => d.value === dimension)?.label}维度汇总`} style={{ marginBottom: 16 }}>
        <Table
          dataSource={dimensionData}
          columns={dimensionColumns}
          rowKey="dim_name"
          pagination={false}
          size="small"
          scroll={{ x: 600 }}
          onRow={(record) => ({
            onClick: () => setDimensionValue(record.dim_name),
            style: { cursor: 'pointer' },
          })}
        />
      </Card>

      {/* 明细数据 */}
      <Card title="贷款明细数据">
        <Table
          dataSource={detailList}
          columns={detailColumns}
          rowKey="id"
          pagination={{
            ...pagination,
            onChange: handlePageChange,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
          }}
          size="small"
          scroll={{ x: 1200 }}
        />
      </Card>
    </div>
  );
};

export default AssetInterestIncome;
