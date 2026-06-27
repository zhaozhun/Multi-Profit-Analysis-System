import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Select, Spin, Empty, message, Table, Collapse, Descriptions } from 'antd';
import { getIndicatorSummary, getCostTypes, getCostAllocationResult, getCostOriginalData } from '../../../services/indicatorApi';

const { Option } = Select;
const { Panel } = Collapse;

interface IndicatorData {
  code: string;
  name: string;
  value: number | null;
  unit: string;
  period: string;
}

interface CostType {
  code: string;
  name: string;
  businessLine: string;
}

interface CostAllocationData {
  costType: string;
  totalAmount: number;
  total: number;
  details: any[];
}

interface CostOriginalData {
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
      setIndicators(data);
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
        setCostTypes(types);
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
      setAllocationData(allocation);
      setOriginalData(original);
    } catch (error) {
      console.error('加载费用数据失败:', error);
      message.error('加载费用数据失败');
    } finally {
      setLoading(false);
    }
  };

  const allocationColumns = [
    { title: '账户ID', dataIndex: 'targetAccountId', key: 'targetAccountId' },
    { title: '账户名称', dataIndex: 'accountName', key: 'accountName' },
    { title: '分摊金额', dataIndex: 'allocatedAmount', key: 'allocatedAmount' },
    { title: '分摊规则', dataIndex: 'allocationRule', key: 'allocationRule' },
  ];

  const originalColumns = [
    { title: '维度编码', dataIndex: 'dimCode', key: 'dimCode' },
    { title: '维度名称', dataIndex: 'dimName', key: 'dimName' },
    { title: '金额', dataIndex: 'amount', key: 'amount' },
  ];

  return (
    <div style={{ display: 'flex', minHeight: 'calc(100vh - 64px)' }}>
      {/* 左侧边栏 */}
      <div style={{ width: 200, background: '#f0f2f5', padding: '16px', borderRight: '1px solid #d9d9d9' }}>
        <div style={{ marginBottom: 16 }}>
          <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>业务线</label>
          <Select
            value={businessLine}
            onChange={setBusinessLine}
            style={{ width: '100%' }}
          >
            <Option value="ASSET">资产</Option>
            <Option value="LIABILITY">负债</Option>
          </Select>
        </div>

        <div>
          <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>指标列表</label>
          <div style={{ maxHeight: 'calc(100vh - 200px)', overflowY: 'auto' }}>
            {indicators.map((indicator) => (
              <div
                key={indicator.code}
                onClick={() => handleIndicatorClick(indicator.code)}
                style={{
                  padding: '8px 12px',
                  marginBottom: 4,
                  cursor: 'pointer',
                  background: selectedIndicator === indicator.code ? '#e6f7ff' : '#fff',
                  border: selectedIndicator === indicator.code ? '1px solid #1890ff' : '1px solid #d9d9d9',
                  borderRadius: 4,
                }}
              >
                <div style={{ fontWeight: 'bold' }}>{indicator.name}</div>
                <div style={{ color: '#666', fontSize: 12 }}>
                  {indicator.value !== null ? indicator.value.toFixed(2) : '-'} {indicator.unit}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* 右侧内容区域 */}
      <div style={{ flex: 1, padding: '16px' }}>
        <Spin spinning={loading}>
          {/* 指标详情区域 */}
          {selectedIndicator && !selectedIndicator.includes('OP') && (
            <Card title="指标详情">
              <p>指标详情区域（待实现）</p>
            </Card>
          )}

          {/* 运营成本 - 费用类型列表 */}
          {selectedIndicator && selectedIndicator.includes('OP') && (
            <Card title="费用类型">
              <Row gutter={[16, 16]}>
                {costTypes.map((type) => (
                  <Col span={6} key={type.code}>
                    <Card
                      hoverable
                      onClick={() => handleCostTypeClick(type.code)}
                      style={{
                        borderColor: selectedCostType === type.code ? '#1890ff' : undefined,
                        backgroundColor: selectedCostType === type.code ? '#e6f7ff' : undefined,
                      }}
                    >
                      <Card.Meta
                        title={type.name}
                        description={
                          <div>
                            <div>业务条线: {type.businessLine === 'ASSET' ? '资产' : '负债'}</div>
                          </div>
                        }
                      />
                    </Card>
                  </Col>
                ))}
              </Row>

              {/* 费用分摊结果 */}
              {selectedCostType && allocationData && (
                <div style={{ marginTop: 24 }}>
                  <Card title="费用分摊结果" style={{ marginBottom: 16 }}>
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
                            pagination={false}
                          />
                        </Panel>
                      </Collapse>
                    </Card>
                  )}
                </div>
              )}
            </Card>
          )}

          {/* 未选择指标时的提示 */}
          {!selectedIndicator && (
            <Card>
              <Empty description="请选择指标查看详情" />
            </Card>
          )}
        </Spin>
      </div>
    </div>
  );
};

export default IndicatorDataPage;
