// @ts-nocheck
import React from 'react';
import IndicatorPage from './components/IndicatorPage';

const CostIndicator: React.FC = () => {
  return <IndicatorPage category="LIABILITY" title="负债类指标" icon="📉" color="#52c41a" />;
};

export default CostIndicator;
