import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Tabs, Spin, Empty, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { getIndicatorSummary, getCostTypes } from '../../../services/indicatorApi';

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

const IndicatorDataPage: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('ASSET');
  const [indicators, setIndicators] = useState<IndicatorData[]>([]);
  const [costTypes, setCostTypes] = useState<CostType[]>([]);
  const [selectedIndicator, setSelectedIndicator] = useState<string | null>(null);

  useEffect(() => {
    loadIndicators();
  }, [activeTab]);

  const loadIndicators = async () => {
    setLoading(true);
    try {
      const data = await getIndicatorSummary(activeTab, '2026-01');
      setIndicators(data);
    } catch (error) {
      console.error('加载指标数据失败:', error);
      message.error('加载指标数据失败');
    } finally {
      setLoading(false);
    }
  };

  const handleIndicatorClick = async (code: string) => {
    setSelectedIndicator(code);
    if (code.includes('OP')) {
      // 如果是运营成本，加载费用类型
      try {
        const types = await getCostTypes(activeTab);
        setCostTypes(types);
      } catch (error) {
        console.error('加载费用类型失败:', error);
        message.error('加载费用类型失败');
      }
    } else {
      setCostTypes([]);
    }
  };

  const handleCostTypeClick = (costType: string) => {
    navigate(`/indicator-data/expense/${costType}`);
  };

  const handleTabChange = (key: string) => {
    setActiveTab(key);
    setSelectedIndicator(null);
    setCostTypes([]);
  };

  return (
    <div style={{ padding: 24 }}>
      <Card title="指标数据">
        <Tabs activeKey={activeTab} onChange={handleTabChange}>
          <Tabs.TabPane tab="资产" key="ASSET" />
          <Tabs.TabPane tab="负债" key="LIABILITY" />
        </Tabs>

        <Spin spinning={loading}>
          {/* 一级指标卡片区域 */}
          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            {indicators.map((indicator) => (
              <Col span={6} key={indicator.code}>
                <Card
                  hoverable
                  onClick={() => handleIndicatorClick(indicator.code)}
                  style={{
                    borderColor: selectedIndicator === indicator.code ? '#1890ff' : undefined,
                    backgroundColor: selectedIndicator === indicator.code ? '#e6f7ff' : undefined,
                  }}
                >
                  <Card.Meta
                    title={indicator.name}
                    description={
                      <div>
                        <div style={{ fontSize: 24, fontWeight: 'bold', color: '#1890ff' }}>
                          {indicator.value !== null ? indicator.value.toFixed(2) : '-'}
                        </div>
                        <div style={{ color: '#666' }}>{indicator.unit}</div>
                      </div>
                    }
                  />
                </Card>
              </Col>
            ))}
          </Row>

          {/* 二级费用类型卡片区域 */}
          {selectedIndicator && selectedIndicator.includes('OP') && costTypes.length > 0 && (
            <div style={{ marginTop: 24 }}>
              <h3 style={{ marginBottom: 16 }}>费用类型</h3>
              <Row gutter={[16, 16]}>
                {costTypes.map((type) => (
                  <Col span={6} key={type.code}>
                    <Card
                      hoverable
                      onClick={() => handleCostTypeClick(type.code)}
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
            </div>
          )}

          {/* 指标详情区域 */}
          {selectedIndicator && !selectedIndicator.includes('OP') && (
            <div style={{ marginTop: 24 }}>
              <Card title="指标详情">
                <p>指标详情区域（待实现）</p>
              </Card>
            </div>
          )}

          {/* 未选择指标时的提示 */}
          {!selectedIndicator && (
            <div style={{ marginTop: 24 }}>
              <Empty description="请选择指标查看详情" />
            </div>
          )}
        </Spin>
      </Card>
    </div>
  );
};

export default IndicatorDataPage;
