import React, { useState, useEffect } from 'react';
import { Card, Table, Select, Space, Button, Modal, Form, Input, message, Tabs } from 'antd';
import { PlayCircleOutlined, SettingOutlined, DatabaseOutlined, ToolOutlined } from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import {
  getExpenseSummary,
  getBizExpenseComposition,
  executeAllocation,
  getExpenseOriginalData,
  getAllocationFactors,
  getAllocationRules,
  saveAllocationRule,
} from '../../../services/expenseApi';

const { Option } = Select;
const { TabPane } = Tabs;

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

const EXPENSE_TYPES = [
  { value: 'RENT', label: '房租物业' },
  { value: 'SALARY', label: '人力成本' },
  { value: 'IT', label: 'IT系统' },
  { value: 'MARKETING', label: '营销费用' },
];

const AssetOperationCost: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [period, setPeriod] = useState('2026-06');
  const [dimension, setDimension] = useState('ORG');
  const [activeTab, setActiveTab] = useState('summary');
  const [summary, setSummary] = useState<Record<string, any>>({});
  const [dimensionData, setDimensionData] = useState<any[]>([]);
  const [detailList, setDetailList] = useState<any[]>([]);
  const [executing, setExecuting] = useState(false);
  const [compositionVisible, setCompositionVisible] = useState(false);
  const [compositionData, setCompositionData] = useState<any[]>([]);
  const [selectedBizId, setSelectedBizId] = useState<string>('');
  const [expenseType, setExpenseType] = useState('RENT');
  const [originalData, setOriginalData] = useState<any[]>([]);
  const [factors, setFactors] = useState<any[]>([]);
  const [rules, setRules] = useState<any[]>([]);
  const [configVisible, setConfigVisible] = useState(false);
  const [selectedRecord, setSelectedRecord] = useState<any>(null);
  const [form] = Form.useForm();

  useEffect(() => {
    loadData();
  }, [period, dimension]);

  useEffect(() => {
    if (activeTab === 'original') {
      loadOriginalData();
    } else if (activeTab === 'factor') {
      loadFactors();
    } else if (activeTab === 'rule') {
      loadRules();
    }
  }, [activeTab, expenseType]);

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

  const loadOriginalData = async () => {
    setLoading(true);
    try {
      const data = await getExpenseOriginalData(period, expenseType);
      setOriginalData(data || []);
    } catch (error) {
      message.error('加载费用原始数据失败');
    } finally {
      setLoading(false);
    }
  };

  const loadFactors = async () => {
    setLoading(true);
    try {
      const data = await getAllocationFactors();
      setFactors(data || []);
    } catch (error) {
      message.error('加载分摊因子失败');
    } finally {
      setLoading(false);
    }
  };

  const loadRules = async () => {
    setLoading(true);
    try {
      const data = await getAllocationRules(expenseType);
      setRules(data || []);
    } catch (error) {
      message.error('加载分摊规则失败');
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

  const handleConfigRule = (record: any) => {
    setSelectedRecord(record);
    const existingRule = rules.find(r => r.expenseType === expenseType);
    form.setFieldsValue({
      ruleCode: existingRule?.ruleCode || `${expenseType}_RULE`,
      ruleName: existingRule?.ruleName || `${EXPENSE_TYPES.find(e => e.value === expenseType)?.label}分摊规则`,
      factorCode: existingRule?.factorCode || 'BIZ_AMOUNT',
      description: existingRule?.description || '',
    });
    setConfigVisible(true);
  };

  const handleSaveRule = async () => {
    try {
      const values = await form.validateFields();
      const result = await saveAllocationRule({
        ...values,
        expenseTable: `expense_${expenseType.toLowerCase()}`,
        expenseType: expenseType,
        sourceDimType: getDimType(),
      });

      if (result.success) {
        message.success('保存成功');
        setConfigVisible(false);
        loadRules();
      } else {
        message.error(result.message || '保存失败');
      }
    } catch (error) {
      message.error('保存失败');
    }
  };

  const getDimType = () => {
    switch (expenseType) {
      case 'RENT': return 'DEPT';
      case 'SALARY': return 'DEPT';
      case 'IT': return 'PRODUCT';
      case 'MARKETING': return 'ORG';
      default: return 'DEPT';
    }
  };

  // 饼图配置
  const getPieChartOption = () => {
    const data = dimensionData.map(item => ({
      name: item.dim_name,
      value: item.total_op_cost,
    }));

    return {
      tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c} ({d}%)' },
      legend: { orient: 'vertical', left: 'left' },
      series: [{
        name: '运营成本',
        type: 'pie',
        radius: '50%',
        data: data,
        emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' } },
      }],
    };
  };

  // 汇总数据
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

  const getOriginalColumns = () => {
    const baseColumns = [{ title: '费用类型', dataIndex: 'expense_type', key: 'expense_type', width: 100, render: () => EXPENSE_TYPES.find(e => e.value === expenseType)?.label }];

    switch (expenseType) {
      case 'RENT':
        return [...baseColumns, { title: '部门名称', dataIndex: 'dept_name', key: 'dept_name', width: 150 }, { title: '金额', dataIndex: 'amount', key: 'amount', width: 120, render: (val: number) => (val / 10000).toFixed(2) + '万' }];
      case 'SALARY':
        return [...baseColumns, { title: '部门名称', dataIndex: 'dept_name', key: 'dept_name', width: 120 }, { title: '客户经理', dataIndex: 'manager_name', key: 'manager_name', width: 120 }, { title: '金额', dataIndex: 'amount', key: 'amount', width: 120, render: (val: number) => (val / 10000).toFixed(2) + '万' }];
      case 'IT':
        return [...baseColumns, { title: '产品名称', dataIndex: 'product_name', key: 'product_name', width: 150 }, { title: '金额', dataIndex: 'amount', key: 'amount', width: 120, render: (val: number) => (val / 10000).toFixed(2) + '万' }];
      case 'MARKETING':
        return [...baseColumns, { title: '机构名称', dataIndex: 'org_name', key: 'org_name', width: 150 }, { title: '金额', dataIndex: 'amount', key: 'amount', width: 120, render: (val: number) => (val / 10000).toFixed(2) + '万' }];
      default:
        return baseColumns;
    }
  };

  const factorColumns = [
    { title: '因子编码', dataIndex: 'factorCode', key: 'factorCode', width: 150 },
    { title: '因子名称', dataIndex: 'factorName', key: 'factorName', width: 150 },
    { title: '因子类型', dataIndex: 'factorType', key: 'factorType', width: 120 },
    { title: '数据来源表', dataIndex: 'sourceTable', key: 'sourceTable', width: 150 },
    { title: '数据来源字段', dataIndex: 'sourceField', key: 'sourceField', width: 120 },
    { title: '描述', dataIndex: 'description', key: 'description', width: 200 },
  ];

  const ruleColumns = [
    { title: '规则编码', dataIndex: 'ruleCode', key: 'ruleCode', width: 150 },
    { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName', width: 150 },
    { title: '费用类型', dataIndex: 'expenseType', key: 'expenseType', width: 100 },
    { title: '源维度', dataIndex: 'sourceDimType', key: 'sourceDimType', width: 100 },
    { title: '分摊因子', dataIndex: 'factorCode', key: 'factorCode', width: 120 },
    { title: '描述', dataIndex: 'description', key: 'description', width: 200 },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h2 style={{ margin: 0 }}>运营成本</h2>
          <p style={{ color: '#999', margin: '4px 0 0' }}>资产条线运营成本汇总及分摊</p>
        </div>
        <Space>
          <Button type="primary" icon={<PlayCircleOutlined />} onClick={handleExecuteAllocation} loading={executing}>
            执行分摊
          </Button>
        </Space>
      </div>

      {/* 筛选条件 */}
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <span>账期：</span>
          <Select value={period} onChange={setPeriod} style={{ width: 150 }}>
            {MONTH_OPTIONS.map((opt) => (<Option key={opt.value} value={opt.value}>{opt.label}</Option>))}
          </Select>
          <span>维度：</span>
          <Select value={dimension} onChange={setDimension} style={{ width: 100 }}>
            {DIMENSIONS.map((dim) => (<Option key={dim.value} value={dim.value}>{dim.label}</Option>))}
          </Select>
          <Button type="primary" onClick={loadData}>查询</Button>
        </Space>
      </Card>

      {/* 主要内容区域 */}
      <Card>
        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          <TabPane tab="运营成本汇总" key="summary">
            {/* 汇总卡片 */}
            <Table dataSource={summaryData} columns={summaryColumns} pagination={false} size="small" scroll={{ x: 600 }} style={{ marginBottom: 16 }} />

            {/* 图表和维度汇总 */}
            <div style={{ display: 'flex', gap: 16, marginBottom: 16 }}>
              <Card title="运营成本占比" style={{ flex: 1 }}>
                <ReactECharts option={getPieChartOption()} style={{ height: 300 }} />
              </Card>
              <Card title={`${DIMENSIONS.find(d => d.value === dimension)?.label}维度汇总`} style={{ flex: 2 }}>
                <Table dataSource={dimensionData} columns={dimensionColumns} rowKey="dim_name" pagination={false} size="small" scroll={{ x: 600 }} />
              </Card>
            </div>

            {/* 业务明细 */}
            <Card title="业务明细">
              <Table dataSource={detailList} columns={detailColumns} rowKey="biz_id"
                pagination={{ pageSize: 20, showSizeChanger: true, showTotal: (total) => `共 ${total} 条` }}
                size="small" scroll={{ x: 1000 }} />
            </Card>
          </TabPane>

          <TabPane tab={<span><DatabaseOutlined /> 费用原始数据</span>} key="original">
            <div style={{ marginBottom: 16 }}>
              <Space>
                <span>费用类型：</span>
                <Select value={expenseType} onChange={setExpenseType} style={{ width: 120 }}>
                  {EXPENSE_TYPES.map((type) => (<Option key={type.value} value={type.value}>{type.label}</Option>))}
                </Select>
                <Button type="primary" onClick={loadOriginalData}>查询</Button>
              </Space>
            </div>
            <Table dataSource={originalData} columns={[
              ...getOriginalColumns(),
              { title: '操作', key: 'action', width: 100, render: (_: any, record: any) => (
                <Button type="link" icon={<SettingOutlined />} onClick={() => handleConfigRule(record)}>配置规则</Button>
              )},
            ]} rowKey="id" loading={loading} pagination={{ pageSize: 20 }} size="small" scroll={{ x: 800 }} />
          </TabPane>

          <TabPane tab={<span><ToolOutlined /> 分摊因子</span>} key="factor">
            <Table dataSource={factors} columns={factorColumns} rowKey="factorCode" loading={loading} pagination={false} size="small" scroll={{ x: 900 }} />
          </TabPane>

          <TabPane tab={<span><SettingOutlined /> 分摊规则</span>} key="rule">
            <div style={{ marginBottom: 16 }}>
              <Space>
                <span>费用类型：</span>
                <Select value={expenseType} onChange={setExpenseType} style={{ width: 120 }}>
                  {EXPENSE_TYPES.map((type) => (<Option key={type.value} value={type.value}>{type.label}</Option>))}
                </Select>
                <Button type="primary" onClick={loadRules}>查询</Button>
              </Space>
            </div>
            <Table dataSource={rules} columns={ruleColumns} rowKey="ruleCode" loading={loading} pagination={false} size="small" scroll={{ x: 900 }} />
          </TabPane>
        </Tabs>
      </Card>

      {/* 费用组成弹窗 */}
      <Modal title={`业务 ${selectedBizId} - 运营成本组成`} open={compositionVisible} onCancel={() => setCompositionVisible(false)} footer={null} width={800}>
        <Table dataSource={compositionData} columns={compositionColumns} rowKey="expense_type" pagination={false} size="small" />
      </Modal>

      {/* 规则配置弹窗 */}
      <Modal title="分摊规则配置" open={configVisible} onOk={handleSaveRule} onCancel={() => setConfigVisible(false)} width={600}>
        <Form form={form} layout="vertical">
          <Form.Item name="ruleCode" label="规则编码" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="ruleName" label="规则名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="factorCode" label="分摊因子" rules={[{ required: true }]}>
            <Select>
              <Option value="MANAGER_COUNT">客户经理人数</Option>
              <Option value="BIZ_AMOUNT">业务金额</Option>
              <Option value="LOAN_BALANCE">贷款余额</Option>
              <Option value="DEPOSIT_BALANCE">存款余额</Option>
              <Option value="BIZ_COUNT">业务笔数</Option>
              <Option value="REVENUE">收入金额</Option>
            </Select>
          </Form.Item>
          <Form.Item name="description" label="描述"><Input.TextArea rows={3} /></Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default AssetOperationCost;
