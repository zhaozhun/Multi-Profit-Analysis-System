import React, { useState } from 'react';
import { Layout, Menu } from 'antd';
import { useNavigate, useLocation, Outlet } from 'react-router-dom';
import {
  DashboardOutlined,
  BarChartOutlined,
  RobotOutlined,
  DatabaseOutlined,
  FundOutlined,
  FileTextOutlined,
  SafetyOutlined,
  AccountBookOutlined,
  LineChartOutlined,
} from '@ant-design/icons';

const { Header, Sider, Content } = Layout;

const MainLayout: React.FC = () => {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  const menuItems = [
    {
      key: '/dashboard',
      icon: <DashboardOutlined />,
      label: '驾驶舱',
    },
    {
      key: 'analysis',
      icon: <BarChartOutlined />,
      label: '维度分析',
      children: [
        { key: '/analysis/ORG', label: '机构分析' },
        { key: '/analysis/BIZ_LINE', label: '条线分析' },
        { key: '/analysis/PRODUCT', label: '产品分析' },
        { key: '/analysis/CHANNEL', label: '渠道分析' },
        { key: '/analysis/DEPT', label: '部门分析' },
        { key: '/analysis/MANAGER', label: '客户经理分析' },
      ],
    },
    {
      key: 'indicator-data',
      icon: <LineChartOutlined />,
      label: '指标数据',
      children: [
        {
          key: 'indicator-asset',
          label: '资产',
          children: [
            { key: '/indicator-data/asset/interest', label: '利息收入' },
            { key: '/indicator-data/asset/ftp', label: 'FTP成本' },
            { key: '/indicator-data/asset/risk', label: '风险成本' },
            { key: '/indicator-data/asset/operation', label: '运营成本' },
          ],
        },
        {
          key: 'indicator-liability',
          label: '负债',
          children: [
            { key: '/indicator-data/liability/interest', label: '对客利息支出' },
            { key: '/indicator-data/liability/ftp', label: 'FTP成本' },
            { key: '/indicator-data/liability/risk', label: '风险成本' },
            { key: '/indicator-data/liability/operation', label: '运营成本' },
          ],
        },
      ],
    },
    {
      key: 'base-data',
      icon: <DatabaseOutlined />,
      label: '主数据管理',
      children: [
        {
          key: 'master-data',
          label: '📁 维度主数据',
          children: [
            { key: '/base-data/master/org', label: '机构' },
            { key: '/base-data/master/biz-line', label: '条线' },
            { key: '/base-data/master/dept', label: '部门' },
            { key: '/base-data/master/product', label: '产品' },
            { key: '/base-data/master/channel', label: '渠道' },
            { key: '/base-data/master/manager', label: '客户经理' },
            { key: '/base-data/master/customer', label: '客户' },
          ],
        },
        {
          key: 'indicator',
          label: '📊 指标库',
          children: [
            { key: '/base-data/indicator/scale', label: '规模指标' },
            { key: '/base-data/indicator/revenue', label: '收入指标' },
            { key: '/base-data/indicator/cost', label: '成本指标' },
            { key: '/base-data/indicator/profit', label: '利润指标' },
            { key: '/base-data/indicator/efficiency', label: '效率指标' },
          ],
        },
      ],
    },
    {
      key: 'report',
      icon: <FileTextOutlined />,
      label: '报表中心',
      children: [
        { key: '/report/ledger', label: '台账报表' },
        { key: '/report/profit', label: '盈利报表' },
        { key: '/report/custom', label: '自定义报表' },
        { key: '/report/ai', label: 'AI报表' },
      ],
    },
    {
      key: '/data-governance',
      icon: <SafetyOutlined />,
      label: '数据治理',
    },
    {
      key: '/ai',
      icon: <RobotOutlined />,
      label: 'AI助手',
    },
  ];

  const getSelectedKeys = () => {
    return [location.pathname];
  };

  const getOpenKeys = () => {
    const path = location.pathname;
    const keys: string[] = [];
    if (path.startsWith('/analysis')) keys.push('analysis');
    if (path.startsWith('/base-data')) {
      keys.push('base-data');
      if (path.includes('/master/')) keys.push('master-data');
      if (path.includes('/indicator/')) keys.push('indicator');
    }
    if (path.startsWith('/indicator-data')) {
      keys.push('indicator-data');
      if (path.includes('/asset/')) keys.push('indicator-asset');
      if (path.includes('/liability/')) keys.push('indicator-liability');
    }
    if (path.startsWith('/report')) keys.push('report');
    return keys;
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider collapsible collapsed={collapsed} onCollapse={setCollapsed}>
        <div style={{ height: 32, margin: 16, textAlign: 'center', color: '#fff', fontWeight: 'bold', fontSize: collapsed ? 14 : 16 }}>
          {collapsed ? '盈利' : '多维盈利分析'}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={getSelectedKeys()}
          defaultOpenKeys={getOpenKeys()}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header style={{ padding: '0 24px', background: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <h2 style={{ margin: 0, fontSize: 18 }}>多维盈利分析系统</h2>
        </Header>
        <Content style={{ margin: '16px' }}>
          <div style={{ padding: 24, background: '#fff', minHeight: 360 }}>
            <Outlet />
          </div>
        </Content>
      </Layout>
    </Layout>
  );
};

export default MainLayout;
