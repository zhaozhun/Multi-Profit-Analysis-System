import React, { useState, useEffect } from 'react';
import { Card, Table, Select, Space, Tag, Collapse, Descriptions, message, Empty, Spin } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { getIndicatorSummary, getCostTypes, getCostAllocationResult, getCostOriginalData } from '../../../services/indicatorApi';

const { Option } = Select;
const { Panel } = Collapse;

interface IndicatorData {
  [key: string]: any;
  code: string;
  name: string;
  value: number | null;
  unit: string;
  period: string;
  today?: number;
  monthTotal?: number;
  yearTotal?: number;
  monthDailyAvg?: number;
  yearDailyAvg?: number;
  balanceToday?: number;
  balanceMonthAvg?: number;
  balanceYearAvg?: number;
  avgRate?: number;
}

interface CostType {
  [key: string]: any;
  code: string;
  name: string;
  businessLine: string;
}

interface CostAllocationData {
  [key: string]: any;
  costType: string;
  totalAmount: number;
  total: number;
  details: any[];
}

interface CostOriginalData {
  [key: string]: any;
  costType: string;
  dimType: string;
  details: any[];
}

const IndicatorDataPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [businessLine, setBusinessLine] = useState('ASSET');
  const [indicators, setIndicators] = useState<IndicatorData[]>([]);
  const [costTypes, setCostTypes] = useState<CostType[]>([]);
  const [selectedIndicator, setSelectedIndicator] = useState<string | null>(null);
  const [selectedCostType, setSelectedCostType] = useState<string | null>(null);
  const [allocationData, setAllocationData] = useState<CostAllocationData | null>(null);
  const [originalData, setOriginalData] = useState<CostOriginalData | null>(null);

  useEffect(() => {
    loadIndicators();
  }, [businessLine]);

  const loadIndicators = async () => {
    setLoading(true);
    try {
      const data = await getIndicatorSummary(businessLine, '2026-01');
      setIndicators(data as unknown as IndicatorData[]);
      setSelectedIndicator(null);
      setSelectedCostType(null);
      setAllocationData(null);
      setOriginalData(null);
    } catch (error) {
      console.error('加载指标数据失败:', error);
      message.error('加载指标数据失败');
    } finally {
      setLoading(false);
    }
  };

  const handleIndicatorClick = async (code: string) => {
    setSelectedIndicator(code);
    setSelectedCostType(null);
    setAllocationData(null);
    setOriginalData(null);

    if (code.includes('OP')) {
      // 如果是运营成本，加载费用类型
      try {
        const types = await getCostTypes(businessLine);
        setCostTypes(types as unknown as CostType[]);
      } catch (error) {
        console.error('加载费用类型失败:', error);
        message.error('加载费用类型失败');
      }
    } else {
      setCostTypes([]);
    }
  };

  const handleCostTypeClick = async (costType: string) => {
    setSelectedCostType(costType);
    setLoading(true);

    try {
      const [allocation, original] = await Promise.all([
        getCostAllocationResult(costType, '2026-01'),
        getCostOriginalData(costType, '2026-01', 'DEPT')
      ]);
      setAllocationData(allocation as unknown as CostAllocationData);
      setOriginalData(original as unknown as CostOriginalData);
    } catch (error) {
      console.error('加载费用数据失败:', error);
      message.error('加载费用数据失败');
    } finally {
      setLoading(false);
    }
  };

  // 格式化金额
  const formatAmount = (value: number | undefined | null) => {
    if (value === undefined || value === null) return '-';
    return value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  };

  // 指标列表表格列
  const indicatorColumns = [
    {
      title: '指标名称',
      dataIndex: 'name',
      key: 'name',
      width: 120,
      fixed: 'left' as const,
      render: (text: string, record: IndicatorData) => (
        <span
          style={{
            cursor: 'pointer',
            color: selectedIndicator === record.code ? '#1890ff' : undefined,
            fontWeight: selectedIndicator === record.code ? 'bold' : undefined,
          }}
          onClick={() => handleIndicatorClick(record.code)}
        >
          {text}
        </span>
      ),
    },
    {
      title: '当天',
      dataIndex: 'today',
      key: 'today',
      width: 120,
      render: (value: number | undefined) => formatAmount(value),
    },
    {
      title: '当月累计',
      dataIndex: 'monthTotal',
      key: 'monthTotal',
      width: 120,
      render: (value: number | undefined) => formatAmount(value),
    },
    {
      title: '当年累计',
      dataIndex: 'yearTotal',
      key: 'yearTotal',
      width: 120,
      render: (value: number | undefined) => formatAmount(value),
    },
    {
      title: '月日均',
      dataIndex: 'monthDailyAvg',
      key: 'monthDailyAvg',
      width: 120,
      render: (value: number | undefined) => formatAmount(value),
    },
    {
      title: '年日均',
      dataIndex: 'yearDailyAvg',
      key: 'yearDailyAvg',
      width: 120,
      render: (value: number | undefined) => formatAmount(value),
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      fixed: 'right' as const,
      render: (_: any, record: IndicatorData) => (
        <a onClick={() => handleIndicatorClick(record.code)}>查看详情</a>
      ),
    },
  ];

  // 费用类型表格列
  const costTypeColumns = [
    {
      title: '费用类型',
      dataIndex: 'name',
      key: 'name',
      render: (text: string, record: CostType) => (
        <span
          style={{
            cursor: 'pointer',
            color: selectedCostType === record.code ? '#1890ff' : undefined,
            fontWeight: selectedCostType === record.code ? 'bold' : undefined,
          }}
          onClick={() => handleCostTypeClick(record.code)}
        >
          {text}
        </span>
      ),
    },
    {
      title: '业务条线',
      dataIndex: 'businessLine',
      key: 'businessLine',
      render: (v: string) => <Tag color={v === 'ASSET' ? 'blue' : 'green'}>{v === 'ASSET' ? '资产' : '负债'}</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: CostType) => (
        <a onClick={() => handleCostTypeClick(record.code)}>查看分摊结果</a>
      ),
    },
  ];

  // 账户级明细表格列
  const allocationColumns = [
    { title: '账户ID', dataIndex: 'targetAccountId', key: 'targetAccountId' },
    { title: '账户名称', dataIndex: 'accountName', key: 'accountName' },
    { title: '分摊金额', dataIndex: 'allocatedAmount', key: 'allocatedAmount' },
    { title: '分摊规则', dataIndex: 'allocationRule', key: 'allocationRule' },
  ];

  // 原始数据表格列
  const originalColumns = [
    { title: '维度编码', dataIndex: 'dimCode', key: 'dimCode' },
    { title: '维度名称', dataIndex: 'dimName', key: 'dimName' },
    { title: '金额', dataIndex: 'amount', key: 'amount' },
  ];

  // 获取当前选中的指标信息
  const selectedIndicatorData = indicators.find(item => item.code === selectedIndicator);

  return (
    <div>
      {/* 页面标题 */}
      <div style={{ marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>📊 指标数据</h2>
        <p style={{ color: '#999', margin: '4px 0 0' }}>
          查看指标汇总数据，点击指标可查看详情，运营成本可查看费用分摊结果
        </p>
      </div>

      {/* 筛选条件 */}
      <Card style={{ marginBottom: 16 }}>
        <Space>
          <span>业务线：</span>
          <Select
            value={businessLine}
            onChange={setBusinessLine}
            style={{ width: 120 }}
          >
            <Option value="ASSET">资产</Option>
            <Option value="LIABILITY">负债</Option>
          </Select>
          <span style={{ marginLeft: 16 }}>账期：</span>
          <Select
            value="2026-01"
            style={{ width: 120 }}
            disabled
          >
            <Option value="2026-01">2026年1月</Option>
          </Select>
        </Space>
      </Card>

      {/* 指标列表 */}
      <Card
        title="指标列表"
        style={{ marginBottom: 16 }}
        extra={
          <Space>
            <span style={{ color: '#999' }}>
              共 {indicators.length} 个指标
            </span>
          </Space>
        }
      >
        <Table
          dataSource={indicators}
          columns={indicatorColumns}
          rowKey="code"
          loading={loading}
          size="small"
          pagination={false}
        />
      </Card>

      {/* 指标详情区域 */}
      {selectedIndicator && (
        <Card
          title={`${selectedIndicatorData?.name || '指标'} - 详情`}
          style={{ marginBottom: 16 }}
        >
          {/* 如果是运营成本，显示费用类型列表 */}
          {selectedIndicator.includes('OP') && (
            <div>
              <h4>费用类型列表</h4>
              <Table
                dataSource={costTypes}
                columns={costTypeColumns}
                rowKey="code"
                size="small"
                pagination={false}
                style={{ marginBottom: 16 }}
              />

              {/* 费用分摊结果 */}
              {selectedCostType && allocationData && (
                <div>
                  <h4>费用分摊结果</h4>
                  <Card style={{ marginBottom: 16 }}>
                    <Descriptions>
                      <Descriptions.Item label="费用类型">{allocationData.costType}</Descriptions.Item>
                      <Descriptions.Item label="总金额">{allocationData.totalAmount} 万元</Descriptions.Item>
                      <Descriptions.Item label="记录数">{allocationData.total}</Descriptions.Item>
                    </Descriptions>
                  </Card>

                  <Card title="账户级明细" style={{ marginBottom: 16 }}>
                    <Table
                      columns={allocationColumns}
                      dataSource={allocationData.details}
                      rowKey="id"
                      size="small"
                      pagination={{ pageSize: 10 }}
                    />
                  </Card>

                  {originalData && (
                    <Card title="原始数据">
                      <Collapse>
                        <Panel header="按部门汇总" key="dept">
                          <Table
                            columns={originalColumns}
                            dataSource={originalData.details}
                            rowKey="dimCode"
                            size="small"
                            pagination={false}
                          />
                        </Panel>
                      </Collapse>
                    </Card>
                  )}
                </div>
              )}
            </div>
          )}

          {/* 如果不是运营成本，显示指标详情 */}
          {!selectedIndicator.includes('OP') && (
            <div>
              <Descriptions column={3}>
                <Descriptions.Item label="指标编码">{selectedIndicator}</Descriptions.Item>
                <Descriptions.Item label="指标名称">{selectedIndicatorData?.name}</Descriptions.Item>
                <Descriptions.Item label="单位">{selectedIndicatorData?.unit}</Descriptions.Item>
              </Descriptions>

              {/* 利息收入显示贷款余额信息 */}
              {selectedIndicator === 'INTEREST_INCOME' && selectedIndicatorData?.balanceToday !== undefined && (
                <Card title="贷款余额信息" style={{ marginTop: 16 }} size="small">
                  <Descriptions column={4}>
                    <Descriptions.Item label="当天余额">{formatAmount(selectedIndicatorData.balanceToday)} 元</Descriptions.Item>
                    <Descriptions.Item label="月日均余额">{formatAmount(selectedIndicatorData.balanceMonthAvg)} 元</Descriptions.Item>
                    <Descriptions.Item label="年日均余额">{formatAmount(selectedIndicatorData.balanceYearAvg)} 元</Descriptions.Item>
                    <Descriptions.Item label="平均利率">{selectedIndicatorData.avgRate?.toFixed(2)}%</Descriptions.Item>
                  </Descriptions>
                </Card>
              )}

              {/* 对客利息支出显示存款余额信息 */}
              {selectedIndicator === 'INTEREST_EXPENSE' && selectedIndicatorData?.balanceToday !== undefined && (
                <Card title="存款余额信息" style={{ marginTop: 16 }} size="small">
                  <Descriptions column={3}>
                    <Descriptions.Item label="当天余额">{formatAmount(selectedIndicatorData.balanceToday)} 元</Descriptions.Item>
                    <Descriptions.Item label="月日均余额">{formatAmount(selectedIndicatorData.balanceMonthAvg)} 元</Descriptions.Item>
                    <Descriptions.Item label="年日均余额">{formatAmount(selectedIndicatorData.balanceYearAvg)} 元</Descriptions.Item>
                  </Descriptions>
                </Card>
              )}
            </div>
          )}
        </Card>
      )}

      {/* 未选择指标时的提示 */}
      {!selectedIndicator && (
        <Card>
          <Empty description="请在上方表格中点击指标查看详情" />
        </Card>
      )}
    </div>
  );
};

export default IndicatorDataPage;
