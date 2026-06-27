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

// 报表
import Ledger from './pages/Report/Ledger';
import ProfitReport from './pages/Report/ProfitReport';
import CustomReport from './pages/Report/CustomReport';
import AiReport from './pages/Report/AiReport';

// 费用分摊
import CostTypePage from './pages/Allocation/CostType';
import FactorManagePage from './pages/Allocation/FactorManage';
import RuleConfigPage from './pages/Allocation/RuleConfig';
import CostRecordPage from './pages/Allocation/CostRecord';
import EmployeeAllocationPage from './pages/Allocation/EmployeeAllocation';
import ProductCommissionPage from './pages/Allocation/ProductCommission';
import OperationCostPage from './pages/Allocation/OperationCost';
import ExecutionPage from './pages/Allocation/Execution';
import ResultPage from './pages/Allocation/Result';
import StatisticsPage from './pages/Allocation/Statistics';

// 指标数据
import IndicatorDataPage from './pages/BaseData/IndicatorData';

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

          {/* 费用分摊 - 基础配置 */}
          <Route path="allocation/cost-type" element={<CostTypePage />} />
          <Route path="allocation/factor" element={<FactorManagePage />} />
          <Route path="allocation/rule" element={<RuleConfigPage />} />

          {/* 费用分摊 - 费用管理 */}
          <Route path="allocation/cost-record" element={<CostRecordPage />} />
          <Route path="allocation/employee" element={<EmployeeAllocationPage />} />
          <Route path="allocation/product-commission" element={<ProductCommissionPage />} />
          <Route path="allocation/operation-cost" element={<OperationCostPage />} />

          {/* 费用分摊 - 分摊结果 */}
          <Route path="allocation/execution" element={<ExecutionPage />} />
          <Route path="allocation/result" element={<ResultPage />} />
          <Route path="allocation/statistics" element={<StatisticsPage />} />

          {/* 指标数据 */}
          <Route path="indicator-data" element={<IndicatorDataPage />} />
        </Route>
      </Routes>
    </ConfigProvider>
  );
};

export default App;
