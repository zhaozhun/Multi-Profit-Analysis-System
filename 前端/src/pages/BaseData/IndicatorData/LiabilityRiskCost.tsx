import React, { useState } from 'react';
import { Card, Select, Space, Empty } from 'antd';

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

const LiabilityRiskCost: React.FC = () => {
  const [period, setPeriod] = useState('2026-06');

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>风险成本</h2>
        <p style={{ color: '#999', margin: '4px 0 0' }}>负债条线无风险成本</p>
      </div>

      <Card style={{ marginBottom: 16 }}>
        <Space>
          <span>账期：</span>
          <Select value={period} onChange={setPeriod} style={{ width: 150 }}>
            {MONTH_OPTIONS.map(opt => <Option key={opt.value} value={opt.value}>{opt.label}</Option>)}
          </Select>
        </Space>
      </Card>

      <Card>
        <Empty description="负债条线无风险成本" />
      </Card>
    </div>
  );
};

export default LiabilityRiskCost;
