import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import MainLayout from './components/MainLayout';
import Dashboard from './pages/Dashboard';
import DimensionAnalysis from './pages/DimensionAnalysis';
import AiAssistant from './pages/AiAssistant';
import DataGovernance from './pages/DataGovernance';

// 基础数据
import BaseDataIndex from './pages/BaseData';
import OrgMaster from './pages/BaseData/MasterData/OrgMaster';
import BizLineMaster from './pages/BaseData/MasterData/BizLineMaster';
import DeptMaster from './pages/BaseData/MasterData/DeptMaster';
import ProductMaster from './pages/BaseData/MasterData/ProductMaster';
import ChannelMaster from './pages/BaseData/MasterData/ChannelMaster';
import ManagerMaster from './pages/BaseData/MasterData/ManagerMaster';
import CustomerMaster from './pages/BaseData/MasterData/CustomerMaster';
import ScaleIndicator from './pages/BaseData/Indicator/ScaleIndicator';
import RevenueIndicator from './pages/BaseData/Indicator/RevenueIndicator';
import CostIndicator from './pages/BaseData/Indicator/CostIndicator';
import ProfitIndicator from './pages/BaseData/Indicator/ProfitIndicator';
import EfficiencyIndicator from './pages/BaseData/Indicator/EfficiencyIndicator';
import IndicatorLibrary from './pages/BaseData/IndicatorLibrary';

// 报表
import Ledger from './pages/Report/Ledger';
import ProfitReport from './pages/Report/ProfitReport';
import CustomReport from './pages/Report/CustomReport';
import AiReport from './pages/Report/AiReport';

// 指标数据
import IndicatorDataPage from './pages/BaseData/IndicatorData';
import AssetInterestIncome from './pages/BaseData/IndicatorData/AssetInterestIncome';
import AssetFtpCost from './pages/BaseData/IndicatorData/AssetFtpCost';
import AssetRiskCost from './pages/BaseData/IndicatorData/AssetRiskCost';
import AssetOperationCost from './pages/BaseData/IndicatorData/AssetOperationCost';
import LiabilityInterestExpense from './pages/BaseData/IndicatorData/LiabilityInterestExpense';
import LiabilityFtpCost from './pages/BaseData/IndicatorData/LiabilityFtpCost';
import LiabilityRiskCost from './pages/BaseData/IndicatorData/LiabilityRiskCost';
import LiabilityOperationCost from './pages/BaseData/IndicatorData/LiabilityOperationCost';

const App: React.FC = () => {
  return (
    <ConfigProvider locale={zhCN}>
      <Routes>
        <Route path="/" element={<MainLayout />}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="analysis/:dimType" element={<DimensionAnalysis />} />
          <Route path="ai" element={<AiAssistant />} />

          {/* 基础数据 */}
          <Route path="base-data" element={<BaseDataIndex />} />
          <Route path="base-data/indicator-library" element={<IndicatorLibrary />} />
          <Route path="base-data/master/org" element={<OrgMaster />} />
          <Route path="base-data/master/biz-line" element={<BizLineMaster />} />
          <Route path="base-data/master/dept" element={<DeptMaster />} />
          <Route path="base-data/master/product" element={<ProductMaster />} />
          <Route path="base-data/master/channel" element={<ChannelMaster />} />
          <Route path="base-data/master/manager" element={<ManagerMaster />} />
          <Route path="base-data/master/customer" element={<CustomerMaster />} />
          <Route path="base-data/indicator/scale" element={<ScaleIndicator />} />
          <Route path="base-data/indicator/revenue" element={<RevenueIndicator />} />
          <Route path="base-data/indicator/cost" element={<CostIndicator />} />
          <Route path="base-data/indicator/profit" element={<ProfitIndicator />} />
          <Route path="base-data/indicator/efficiency" element={<EfficiencyIndicator />} />

          {/* 报表 */}
          <Route path="report/ledger" element={<Ledger />} />
          <Route path="report/profit" element={<ProfitReport />} />
          <Route path="report/custom" element={<CustomReport />} />
          <Route path="report/ai" element={<AiReport />} />
          <Route path="data-governance" element={<DataGovernance />} />

          {/* 指标数据 */}
          <Route path="indicator-data" element={<IndicatorDataPage />} />
          <Route path="indicator-data/asset/interest" element={<AssetInterestIncome />} />
          <Route path="indicator-data/asset/ftp" element={<AssetFtpCost />} />
          <Route path="indicator-data/asset/risk" element={<AssetRiskCost />} />
          <Route path="indicator-data/asset/operation" element={<AssetOperationCost />} />
          <Route path="indicator-data/liability/interest" element={<LiabilityInterestExpense />} />
          <Route path="indicator-data/liability/ftp" element={<LiabilityFtpCost />} />
          <Route path="indicator-data/liability/risk" element={<LiabilityRiskCost />} />
          <Route path="indicator-data/liability/operation" element={<LiabilityOperationCost />} />
        </Route>
      </Routes>
    </ConfigProvider>
  );
};

export default App;
