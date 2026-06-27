import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Select, Button, Space, message } from 'antd';
import IndicatorCard from './components/IndicatorCard';
import DetailTable from './components/DetailTable';
import { getAtomicIndicators, getIndicatorValue } from '../../../services/indicatorApi';
import type { AtomicIndicator, IndicatorValue } from '../../../services/indicatorApi';

const { Option } = Select;

interface IndicatorData {
  code: string;
  name: string;
  monthlyDailyAvg: number | null;
  yearlyDailyAvg: number | null;
  unit: string;
  precisionVal: number;
}

const LoanIndicator: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [indicators, setIndicators] = useState<AtomicIndicator[]>([]);
  const [indicatorData, setIndicatorData] = useState<Record<string, IndicatorData>>({});
  const [selectedIndicator, setSelectedIndicator] = useState<string>('');
  const [period, setPeriod] = useState<string>('MONTH');
  const [periodValue, setPeriodValue] = useState<string>('2024-01');

  useEffect(() => {
    fetchIndicators();
  }, []);

  useEffect(() => {
    if (indicators.length > 0) {
      fetchIndicatorValues();
    }
  }, [indicators, period, periodValue]);

  const fetchIndicators = async () => {
    setLoading(true);
    try {
      const data = await getAtomicIndicators();
      const loanIndicators = data.filter((item) => item.businessLine === 'LOAN');
      setIndicators(loanIndicators);
      if (loanIndicators.length > 0) {
        setSelectedIndicator(loanIndicators[0].code);
      }
    } catch (error) {
      message.error('获取指标列表失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchIndicatorValues = async () => {
    setLoading(true);
    try {
      const data: Record<string, IndicatorData> = {};
      for (const indicator of indicators) {
        const monthlyValue = await getIndicatorValue(indicator.code, 'MONTH', periodValue);
        const yearlyValue = await getIndicatorValue(indicator.code, 'YEAR', periodValue.substring(0, 4));
        data[indicator.code] = {
          code: indicator.code,
          name: indicator.name,
          monthlyDailyAvg: monthlyValue.value,
          yearlyDailyAvg: yearlyValue.value,
          unit: indicator.unit,
          precisionVal: indicator.precisionVal,
        };
      }
      setIndicatorData(data);
    } catch (error) {
      message.error('获取指标值失败');
    } finally {
      setLoading(false);
    }
  };

  const handleIndicatorClick = (code: string) => {
    setSelectedIndicator(code);
  };

  const handleQuery = () => {
    fetchIndicatorValues();
  };

  const handleReset = () => {
    setPeriod('MONTH');
    setPeriodValue('2024-01');
  };

  const selectedIndicatorConfig = indicators.find((item) => item.code === selectedIndicator);

  return (
    <div style={{ padding: 24 }}>
      <Card title="贷款指标" loading={loading}>
        {/* 指标卡片区域 */}
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          {indicators.map((indicator) => {
            const data = indicatorData[indicator.code];
            return (
              <Col key={indicator.code} xs={24} sm={12} md={8} lg={4}>
                <IndicatorCard
                  code={indicator.code}
                  name={indicator.name}
                  monthlyDailyAvg={data?.monthlyDailyAvg || null}
                  yearlyDailyAvg={data?.yearlyDailyAvg || null}
                  unit={indicator.unit}
                  precision={indicator.precisionVal}
                  isSelected={selectedIndicator === indicator.code}
                  onClick={handleIndicatorClick}
                />
              </Col>
            );
          })}
        </Row>

        {/* 筛选条件 */}
        <Card style={{ marginBottom: 24 }}>
          <Space>
            <Select
              value={period}
              onChange={setPeriod}
              style={{ width: 120 }}
            >
              <Option value="MONTH">月日均</Option>
              <Option value="YEAR">年日均</Option>
            </Select>
            <Select
              value={periodValue}
              onChange={setPeriodValue}
              style={{ width: 150 }}
            >
              <Option value="2024-01">2024年1月</Option>
              <Option value="2024-02">2024年2月</Option>
              <Option value="2024-03">2024年3月</Option>
              <Option value="2024">2024年</Option>
            </Select>
            <Button type="primary" onClick={handleQuery}>查询</Button>
            <Button onClick={handleReset}>重置</Button>
          </Space>
        </Card>

        {/* 明细数据区域 */}
        {selectedIndicatorConfig && (
          <Card title={`${selectedIndicatorConfig.name} - 明细数据`}>
            <DetailTable
              indicatorCode={selectedIndicator}
              period={period}
              periodValue={periodValue}
              displayFields={JSON.parse(selectedIndicatorConfig.detailDisplayFields || '[]')}
              groupByField={selectedIndicatorConfig.detailGroupBy || 'stat_date'}
            />
          </Card>
        )}
      </Card>
    </div>
  );
};

export default LoanIndicator;
