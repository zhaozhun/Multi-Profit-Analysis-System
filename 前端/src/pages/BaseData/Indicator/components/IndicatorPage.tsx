// @ts-nocheck
import React, { useState, useEffect, useCallback } from 'react';
import { Card, Table, Button, Space, Tag, Tabs, message } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { getAtomicIndicators, getDerivedIndicators } from '../../../../services/indicatorApi';
import type { AtomicIndicator, DerivedIndicator } from '../../../../services/indicatorApi';

interface IndicatorPageProps {
  category: string;
  title: string;
  icon: string;
  color: string;
}

const IndicatorPage: React.FC<IndicatorPageProps> = ({ category, title, icon, color }) => {
  const [atomicData, setAtomicData] = useState<AtomicIndicator[]>([]);
  const [derivedData, setDerivedData] = useState<DerivedIndicator[]>([]);
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('atomic');

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [atomic, derived] = await Promise.all([
        getAtomicIndicators(),
        getDerivedIndicators(),
      ]);
      // 按业务条线筛选
      const businessLine = category === 'ASSET' ? 'ASSET' : category === 'LIABILITY' ? 'LIABILITY' : null;
      if (businessLine) {
        setAtomicData(atomic.filter(item => item.businessLine === businessLine));
        setDerivedData(derived.filter(item => item.businessLine === businessLine));
      } else {
        setAtomicData(atomic);
        setDerivedData(derived);
      }
    } catch (error) {
      console.error('Failed to fetch indicators:', error);
    } finally {
      setLoading(false);
    }
  }, [category]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const atomicColumns = [
    { title: '编码', dataIndex: 'code', width: 150 },
    { title: '名称', dataIndex: 'name', width: 150 },
    { title: '业务条线', dataIndex: 'businessLine', width: 100, render: (v: string) => (
      <Tag color={v === 'ASSET' ? 'blue' : 'green'}>{v === 'ASSET' ? '资产' : '负债'}</Tag>
    )},
    { title: '来源表', dataIndex: 'sourceTable', width: 120 },
    { title: '来源字段', dataIndex: 'sourceField', width: 120 },
    { title: '筛选条件', dataIndex: 'filterCondition', width: 150, ellipsis: true },
    { title: '单位', dataIndex: 'unit', width: 80 },
    { title: '精度', dataIndex: 'precisionVal', width: 80 },
    { title: '描述', dataIndex: 'description', width: 200, ellipsis: true },
  ];

  const derivedColumns = [
    { title: '编码', dataIndex: 'code', width: 180 },
    { title: '名称', dataIndex: 'name', width: 150 },
    { title: '业务条线', dataIndex: 'businessLine', width: 100, render: (v: string) => (
      <Tag color={v === 'ASSET' ? 'blue' : 'green'}>{v === 'ASSET' ? '资产' : '负债'}</Tag>
    )},
    { title: '计算公式', dataIndex: 'calcFormula', width: 300, ellipsis: true },
    { title: '单位', dataIndex: 'unit', width: 80 },
    { title: '精度', dataIndex: 'precisionVal', width: 80 },
    { title: '描述', dataIndex: 'description', width: 200, ellipsis: true },
  ];

  const tabItems = [
    {
      key: 'atomic',
      label: '原子指标',
      children: (
        <Table
          dataSource={atomicData}
          columns={atomicColumns}
          rowKey="code"
          loading={loading}
          size="small"
          pagination={{ pageSize: 15 }}
          scroll={{ x: 1200 }}
        />
      ),
    },
    {
      key: 'derived',
      label: '派生指标',
      children: (
        <Table
          dataSource={derivedData}
          columns={derivedColumns}
          rowKey="code"
          loading={loading}
          size="small"
          pagination={{ pageSize: 15 }}
          scroll={{ x: 1200 }}
        />
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>{icon} {title}</h2>
        <p style={{ color: '#999', margin: '4px 0 0' }}>管理{title}，查看原子指标和派生指标</p>
      </div>

      <Card>
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={fetchData}>刷新</Button>
          </Space>
          <Space>
            <Tag color="blue">原子指标: {atomicData.length}</Tag>
            <Tag color="green">派生指标: {derivedData.length}</Tag>
          </Space>
        </div>

        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={tabItems}
        />
      </Card>
    </div>
  );
};

export default IndicatorPage;
