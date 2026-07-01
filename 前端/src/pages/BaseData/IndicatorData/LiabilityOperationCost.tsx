import React, { useState, useEffect } from 'react';
import { Card, Table, Select, Space, Button, Modal, message } from 'antd';
import { PlayCircleOutlined } from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import {
  getExpenseSummary,
  getBizExpenseComposition,
  executeAllocation,
} from '../../../services/expenseApi';

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
];

const LiabilityOperationCost: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [period, setPeriod] = useState('2026-06');
  const [dimension, setDimension] = useState('ORG');
  const [summary, setSummary] = useState<Record<string, any>>({});
  const [dimensionData, setDimensionData] = useState<any[]>([]);
  const [detailList, setDetailList] = useState<any[]>([]);
  const [executing, setExecuting] = useState(false);
  const [compositionVisible, setCompositionVisible] = useState(false);
  const [compositionData, setCompositionData] = useState<any[]>([]);
  const [selectedBizId, setSelectedBizId] = useState<string>('');

  useEffect(() => {
    loadData();
  }, [period, dimension]);

  const loadData = async () => {
    setLoading(true);
    try {
      const data = await getExpenseSummary(period, 'ASSESS', dimension);
      setSummary(data?.summary || {});
      setDimensionData(data?.dimension || []);
      setDetailList(data?.detail || []);
    } catch (error) {
      message.error('加载数据失败');
    } finally {
      setLoading(false);
    }
  };

  const handleExecuteAllocation = async () => {
    setExecuting(true);
    try {
      const result = await executeAllocation(period);
      if (result.success) {
        message.success(`分摊执行成功，共${result.totalRecords}条记录`);
        loadData();
      } else {
        message.error(result.message || '分摊执行失败');
      }
    } catch (error) {
      message.error('分摊执行失败');
    } finally {
      setExecuting(false);
    }
  };

  const handleShowComposition = async (bizId: string) => {
    setSelectedBizId(bizId);
    try {
      const data = await getBizExpenseComposition(period, bizId);
      setCompositionData(data || []);
      setCompositionVisible(true);
    } catch (error) {
      message.error('获取费用组成失败');
    }
  };

  // 饼图配置
  const getPieChartOption = () => {
    const data = dimensionData.map(item => ({ name: item.dim_name, value: item.total_op_cost }));
    return {
      tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c} ({d}%)' },
      legend: { orient: 'vertical', left: 'left' },
      series: [{ name: '运营成本', type: 'pie', radius: '50%', data: data }],
    };
  };

  const summaryData = [{
    key: 'summary',
    opCost: summary.total_op_cost || 0,
    costRatio: summary.total_balance ? (summary.total_op_cost / summary.total_balance * 100) : 0,
    bizCount: summary.total_count || 0,
    avgCost: summary.total_count ? (summary.total_op_cost / summary.total_count) : 0,
  }];

  const summaryColumns = [
    { title: '运营成本合计', dataIndex: 'opCost', key: 'opCost', width: 150, render: (val: number) => (val / 10000).toFixed(2) + ' 万' },
    { title: '平均成本率', dataIndex: 'costRatio', key: 'costRatio', width: 120, render: (val: number) => val.toFixed(2) + '%' },
    { title: '业务笔数', dataIndex: 'bizCount', key: 'bizCount', width: 100 },
    { title: '平均单笔成本', dataIndex: 'avgCost', key: 'avgCost', width: 130, render: (val: number) => (val / 10000).toFixed(4) + ' 万' },
  ];

  const dimensionColumns = [
    { title: '维度名称', dataIndex: 'dim_name', key: 'dim_name', width: 120 },
    { title: '业务笔数', dataIndex: 'count', key: 'count', width: 100 },
    { title: '余额合计', dataIndex: 'total_balance', key: 'total_balance', width: 120, render: (val: number) => val ? (val / 10000).toFixed(2) + '万' : '-' },
    { title: '运营成本合计', dataIndex: 'total_op_cost', key: 'total_op_cost', width: 130, render: (val: number) => val ? (val / 10000).toFixed(2) + '万' : '-' },
    { title: '平均成本率', dataIndex: 'cost_ratio', key: 'cost_ratio', width: 120, render: (val: number) => val ? val.toFixed(2) + '%' : '-' },
  ];

  const detailColumns = [
    { title: '业务编号', dataIndex: 'biz_id', key: 'biz_id', width: 120 },
    { title: '客户名称', dataIndex: 'customer_name', key: 'customer_name', width: 120 },
    { title: '机构', dataIndex: 'org_name', key: 'org_name', width: 100 },
    { title: '产品', dataIndex: 'product_name', key: 'product_name', width: 100 },
    { title: '客户经理', dataIndex: 'manager_name', key: 'manager_name', width: 100 },
    { title: '余额', dataIndex: 'balance', key: 'balance', width: 120, render: (val: number) => val ? (val / 10000).toFixed(2) + '万' : '-' },
    {
      title: '运营成本',
      dataIndex: 'op_cost',
      key: 'op_cost',
      width: 120,
      render: (val: number, record: any) => (
        <a onClick={() => handleShowComposition(record.biz_id)}>
          {val ? (val / 10000).toFixed(4) + '万' : '-'}
        </a>
      ),
    },
    { title: '利润', dataIndex: 'profit', key: 'profit', width: 120, render: (val: number) => val ? (val / 10000).toFixed(4) + '万' : '-' },
  ];

  const compositionColumns = [
    { title: '费用类型', dataIndex: 'expense_type', key: 'expense_type', width: 120 },
    { title: '费用名称', dataIndex: 'expense_name', key: 'expense_name', width: 150 },
    { title: '分摊金额', dataIndex: 'allocated_amount', key: 'allocated_amount', width: 120, render: (val: number) => val ? (val / 10000).toFixed(4) + '万' : '-' },
    { title: '分摊比例', dataIndex: 'factor_ratio', key: 'factor_ratio', width: 100, render: (val: number) => val ? (val * 100).toFixed(4) + '%' : '-' },
    { title: '分摊规则', dataIndex: 'rule_code', key: 'rule_code', width: 150 },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h2 style={{ margin: 0 }}>运营成本</h2>
          <p style={{ color: '#999', margin: '4px 0 0' }}>负债条线运营成本汇总及分摊</p>
        </div>
        <Button type="primary" icon={<PlayCircleOutlined />} onClick={handleExecuteAllocation} loading={executing}>
          执行分摊
        </Button>
      </div>

      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <span>账期：</span>
          <Select value={period} onChange={setPeriod} style={{ width: 150 }}>
            {MONTH_OPTIONS.map(opt => <Option key={opt.value} value={opt.value}>{opt.label}</Option>)}
          </Select>
          <span>维度：</span>
          <Select value={dimension} onChange={setDimension} style={{ width: 100 }}>
            {DIMENSIONS.map(dim => <Option key={dim.value} value={dim.value}>{dim.label}</Option>)}
          </Select>
          <Button type="primary" onClick={loadData}>查询</Button>
        </Space>
      </Card>

      <Card loading={loading} style={{ marginBottom: 16 }}>
        <Table dataSource={summaryData} columns={summaryColumns} pagination={false} size="small" scroll={{ x: 600 }} />
      </Card>

      <div style={{ display: 'flex', gap: 16, marginBottom: 16 }}>
        <Card title="运营成本占比" style={{ flex: 1 }}>
          <ReactECharts option={getPieChartOption()} style={{ height: 300 }} />
        </Card>
        <Card title={`${DIMENSIONS.find(d => d.value === dimension)?.label}维度汇总`} style={{ flex: 2 }}>
          <Table dataSource={dimensionData} columns={dimensionColumns} rowKey="dim_name" pagination={false} size="small" scroll={{ x: 600 }} />
        </Card>
      </div>

      <Card title="业务明细">
        <Table dataSource={detailList} columns={detailColumns} rowKey="biz_id"
          pagination={{ pageSize: 20, showSizeChanger: true, showTotal: total => `共 ${total} 条` }}
          size="small" scroll={{ x: 1000 }} />
      </Card>

      <Modal title={`业务 ${selectedBizId} - 运营成本组成`} open={compositionVisible} onCancel={() => setCompositionVisible(false)} footer={null} width={800}>
        <Table dataSource={compositionData} columns={compositionColumns} rowKey="expense_type" pagination={false} size="small" />
      </Modal>
    </div>
  );
};

export default LiabilityOperationCost;
