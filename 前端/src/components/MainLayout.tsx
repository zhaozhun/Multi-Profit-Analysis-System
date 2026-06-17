import React, { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu, theme, Typography } from 'antd';
import {
  DashboardOutlined,
  BarChartOutlined,
  DatabaseOutlined,
  FileTextOutlined,
  RobotOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  BankOutlined,
  TeamOutlined,
  ShoppingOutlined,
  BranchesOutlined,
  ApartmentOutlined,
  UserOutlined,
  CustomerServiceOutlined,
  SearchOutlined,
  SafetyOutlined,
  FileSearchOutlined,
  LineChartOutlined,
  TableOutlined,
  FormOutlined,
  ExperimentOutlined,
} from '@ant-design/icons';

const { Header, Sider, Content } = Layout;
const { Title } = Typography;

const MainLayout: React.FC = () => {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { token: { colorBgContainer, borderRadiusLG } } = theme.useToken();

  const menuItems = [
    {
      key: 'dashboard',
      icon: <DashboardOutlined />,
      label: '经营驾驶舱',
    },
    {
      key: 'analysis',
      icon: <BarChartOutlined />,
      label: '盈利分析中心',
      children: [
        { key: 'analysis/ORG', icon: <BankOutlined />, label: '机构维度分析' },
        { key: 'analysis/BIZ_LINE', icon: <BranchesOutlined />, label: '条线维度分析' },
        { key: 'analysis/DEPT', icon: <ApartmentOutlined />, label: '部门维度分析' },
        { key: 'analysis/PRODUCT', icon: <ShoppingOutlined />, label: '产品维度分析' },
        { key: 'analysis/CHANNEL', icon: <TeamOutlined />, label: '渠道维度分析' },
        { key: 'analysis/MANAGER', icon: <UserOutlined />, label: '客户经理维度分析' },
        { key: 'analysis/CUSTOMER', icon: <CustomerServiceOutlined />, label: '客户维度分析' },
      ],
    },
    {
      key: 'data-mgmt',
      icon: <DatabaseOutlined />,
      label: '数据管理平台',
      children: [
        { key: 'master-data', icon: <FormOutlined />, label: '主数据管理' },
        { key: 'indicator', icon: <LineChartOutlined />, label: '指标库管理' },
      ],
    },
    {
      key: 'report',
      icon: <FileTextOutlined />,
      label: '报表服务中心',
      children: [
        { key: 'report/ledger', icon: <TableOutlined />, label: '明细台账查询' },
        { key: 'report/profit', icon: <BarChartOutlined />, label: '盈利报表查询' },
        { key: 'report/custom', icon: <FormOutlined />, label: '自定义报表' },
        { key: 'report/ai', icon: <ExperimentOutlined />, label: 'AI 智能报表' },
      ],
    },
    {
      key: 'data-governance',
      icon: <SafetyOutlined />,
      label: '数据治理',
    },
    {
      key: 'ai',
      icon: <RobotOutlined />,
      label: 'AI 智能助手',
    },
  ];

  const handleMenuClick = ({ key }: { key: string }) => {
    if (key.includes('/')) {
      navigate(`/${key}`);
    } else {
      navigate(`/${key}`);
    }
  };

  const getSelectedKeys = () => {
    const path = location.pathname;
    if (path === '/dashboard') return ['dashboard'];
    if (path.startsWith('/analysis')) return [path.substring(1)];
    if (path === '/ai') return ['ai'];
    if (path === '/ai-explore') return ['ai-explore'];
    if (path === '/master-data') return ['master-data'];
    if (path === '/indicator') return ['indicator'];
    if (path.startsWith('/report')) return [path.substring(1)];
    if (path === '/data-governance') return ['data-governance'];
    return ['dashboard'];
  };

  const getOpenKeys = () => {
    const path = location.pathname;
    if (path.startsWith('/analysis')) return ['analysis'];
    if (path.startsWith('/report')) return ['report'];
    if (path === '/master-data' || path === '/indicator') return ['data-mgmt'];
    return [];
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        width={240}
        style={{
          overflow: 'auto',
          height: '100vh',
          position: 'fixed',
          left: 0,
          top: 0,
          bottom: 0,
          background: '#001529',
        }}
      >
        <div style={{
          height: 64,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          borderBottom: '1px solid rgba(255,255,255,0.1)',
        }}>
          <Title level={4} style={{ color: '#fff', margin: 0, whiteSpace: 'nowrap' }}>
            {collapsed ? '盈利' : '多维盈利分析系统'}
          </Title>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={getSelectedKeys()}
          defaultOpenKeys={getOpenKeys()}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>

      <Layout style={{ marginLeft: collapsed ? 80 : 240, transition: 'all 0.2s' }}>
        <Header style={{
          padding: '0 24px',
          background: colorBgContainer,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
          position: 'sticky',
          top: 0,
          zIndex: 10,
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            {React.createElement(collapsed ? MenuUnfoldOutlined : MenuFoldOutlined, {
              className: 'trigger',
              onClick: () => setCollapsed(!collapsed),
              style: { fontSize: 18, cursor: 'pointer' },
            })}
            <span style={{ color: '#666', fontSize: 14 }}>
              数据更新至 2026年5月
            </span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <RobotOutlined style={{ fontSize: 18, cursor: 'pointer', color: '#1890ff' }}
              onClick={() => navigate('/ai')} />
          </div>
        </Header>

        <Content style={{
          margin: 16,
          padding: 24,
          background: colorBgContainer,
          borderRadius: borderRadiusLG,
          minHeight: 'calc(100vh - 112px)',
        }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
};

export default MainLayout;
