import React from 'react';
import { Card, Statistic, Row, Col } from 'antd';

interface IndicatorCardProps {
  code: string;
  name: string;
  monthlyDailyAvg: number | null;
  yearlyDailyAvg: number | null;
  unit: string;
  precision: number;
  isSelected: boolean;
  onClick: (code: string) => void;
}

const IndicatorCard: React.FC<IndicatorCardProps> = ({
  code,
  name,
  monthlyDailyAvg,
  yearlyDailyAvg,
  unit,
  precision,
  isSelected,
  onClick,
}) => {
  const formatValue = (value: number | null) => {
    if (value === null || value === undefined) return '--';
    return value.toFixed(precision);
  };

  return (
    <Card
      hoverable
      style={{
        borderColor: isSelected ? '#1890ff' : '#d9d9d9',
        borderWidth: isSelected ? 2 : 1,
      }}
      onClick={() => onClick(code)}
    >
      <div style={{ textAlign: 'center', marginBottom: 16 }}>
        <div style={{ fontSize: 16, fontWeight: 'bold', marginBottom: 8 }}>{name}</div>
      </div>
      <Row gutter={[16, 16]}>
        <Col span={12}>
          <Statistic
            title="月日均"
            value={formatValue(monthlyDailyAvg)}
            suffix={unit}
            precision={precision}
          />
        </Col>
        <Col span={12}>
          <Statistic
            title="年日均"
            value={formatValue(yearlyDailyAvg)}
            suffix={unit}
            precision={precision}
          />
        </Col>
      </Row>
    </Card>
  );
};

export default IndicatorCard;
