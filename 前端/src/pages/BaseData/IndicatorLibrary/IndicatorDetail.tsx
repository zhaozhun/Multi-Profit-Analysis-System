import React from 'react';
import { Descriptions, Tag, Divider } from 'antd';
import IndicatorRelationGraph from './IndicatorRelationGraph';

interface IndicatorDetailProps {
  indicator: any;
}

const IndicatorDetail: React.FC<IndicatorDetailProps> = ({ indicator }) => {
  const getBusinessLineName = (code: string) => {
    const map: Record<string, string> = {
      ASSET: '资产类',
      LIABILITY: '负债类',
      ALL: '全部',
    };
    return map[code] || code;
  };

  const getTypeName = (code: string) => {
    const map: Record<string, string> = {
      SCALE: '规模类',
      REVENUE: '收入类',
      COST: '成本类',
      PROFIT: '利润类',
      EFFICIENCY: '效率类',
      DAILY_AVG: '日均类',
    };
    return map[code] || code;
  };

  const getBusinessLineColor = (code: string) => {
    const map: Record<string, string> = {
      ASSET: 'blue',
      LIABILITY: 'green',
      ALL: 'default',
    };
    return map[code] || 'default';
  };

  return (
    <div>
      <Descriptions column={2} bordered>
        <Descriptions.Item label="指标编码">{indicator.indicator_code}</Descriptions.Item>
        <Descriptions.Item label="指标名称">{indicator.indicator_name}</Descriptions.Item>
        <Descriptions.Item label="业务条线">
          <Tag color={getBusinessLineColor(indicator.business_line)}>
            {getBusinessLineName(indicator.business_line)}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="指标类型">
          <Tag>{getTypeName(indicator.indicator_type)}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="计算公式" span={2}>
          {indicator.calc_formula}
        </Descriptions.Item>
        <Descriptions.Item label="数据来源">{indicator.data_source}</Descriptions.Item>
        <Descriptions.Item label="单位">{indicator.unit}</Descriptions.Item>
        <Descriptions.Item label="精度">{indicator.precision_val}</Descriptions.Item>
        <Descriptions.Item label="状态">
          <Tag color={indicator.status === 'ACTIVE' ? 'green' : 'red'}>
            {indicator.status === 'ACTIVE' ? '启用' : '禁用'}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="支持维度" span={2}>
          {indicator.supported_dims}
        </Descriptions.Item>
        <Descriptions.Item label="描述" span={2}>
          {indicator.description}
        </Descriptions.Item>
      </Descriptions>

      <Divider>关联关系</Divider>

      <IndicatorRelationGraph indicatorCode={indicator.indicator_code} />
    </div>
  );
};

export default IndicatorDetail;
