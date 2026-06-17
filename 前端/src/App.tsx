import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import MainLayout from './components/MainLayout';
import Dashboard from './pages/Dashboard';
import DimensionAnalysis from './pages/DimensionAnalysis';
import AiAssistant from './pages/AiAssistant';
import MasterData from './pages/MasterData';
import Indicator from './pages/Indicator';
import Ledger from './pages/Report/Ledger';
import ProfitReport from './pages/Report/ProfitReport';
import CustomReport from './pages/Report/CustomReport';
import AiReport from './pages/Report/AiReport';
import DataGovernance from './pages/DataGovernance';

const App: React.FC = () => {
  return (
    <ConfigProvider locale={zhCN}>
      <Routes>
        <Route path="/" element={<MainLayout />}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="analysis/:dimType" element={<DimensionAnalysis />} />
          <Route path="ai" element={<AiAssistant />} />
          <Route path="master-data" element={<MasterData />} />
          <Route path="indicator" element={<Indicator />} />
          <Route path="report/ledger" element={<Ledger />} />
          <Route path="report/profit" element={<ProfitReport />} />
          <Route path="report/custom" element={<CustomReport />} />
          <Route path="report/ai" element={<AiReport />} />
          <Route path="data-governance" element={<DataGovernance />} />
        </Route>
      </Routes>
    </ConfigProvider>
  );
};

export default App;
