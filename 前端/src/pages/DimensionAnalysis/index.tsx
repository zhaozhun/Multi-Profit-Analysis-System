import React, { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { Row, Col, Card, Select, Table, Spin, Tag, Button, Space, DatePicker, Modal, Tabs, Dropdown, Breadcrumb, message } from 'antd';
import {
  ReloadOutlined, ApartmentOutlined, BarChartOutlined, ShoppingOutlined,
  TeamOutlined, UserOutlined, CustomerServiceOutlined, DownOutlined, DownloadOutlined,
} from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import dayjs from 'dayjs';
import api from '../../services/api';

const { RangePicker } = DatePicker;

const dimLabels: Record<string, string> = {
  ORG: '机构', BIZ_LINE: '条线', DEPT: '部门', PRODUCT: '产品',
  CHANNEL: '渠道', MANAGER: '客户经理', CUSTOMER: '客户',
};

const dimIcons: Record<string, React.ReactNode> = {
  ORG: <ApartmentOutlined />, BIZ_LINE: <BarChartOutlined />, DEPT: <ApartmentOutlined />,
  PRODUCT: <ShoppingOutlined />, CHANNEL: <TeamOutlined />, MANAGER: <UserOutlined />,
  CUSTOMER: <CustomerServiceOutlined />,
};

const ALL_DIMS = ['ORG', 'BIZ_LINE', 'DEPT', 'PRODUCT', 'CHANNEL', 'MANAGER', 'CUSTOMER'];

const DimensionAnalysis: React.FC = () => {
  const { dimType } = useParams<{ dimType: string }>();
  const [loading, setLoading] = useState(true);
  const [caliberType] = useState('BOOK');
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs]>([dayjs('2025-06-01'), dayjs('2025-06-30')]);
  const [quickSelect, setQuickSelect] = useState('thisMonth');
  const [treeData, setTreeData] = useState<any[]>([]);
  const [expandedRowKeys, setExpandedRowKeys] = useState<string[]>([]);
  const [activeTab, setActiveTab] = useState('data');
  const [crossDimType, setCrossDimType] = useState<string | undefined>(undefined);
  const [crossDimName, setCrossDimName] = useState<string | undefined>(undefined);
  const [crossTreeData, setCrossTreeData] = useState<any[]>([]);
  const [crossExpandedKeys, setCrossExpandedKeys] = useState<string[]>([]);
  const [crossLoading, setCrossLoading] = useState(false);
  const [breakdownVisible, setBreakdownVisible] = useState(false);
  const [breakdownData, setBreakdownData] = useState<any>(null);
  const [drillPath, setDrillPath] = useState<any[]>([]);

  const fetchTreeData = useCallback(async (parentId?: number) => {
    if (!dimType) return;
    setLoading(true);
    const startDate = dateRange[0].format('YYYY-MM-DD');
    const endDate = dateRange[1].format('YYYY-MM-DD');
    try {
      const result: any = await api.get(`/dimension/${dimType}/tree`, {
        params: { startDate, endDate, caliberType, parentId: parentId || 0 }
      });
      const newData = result || [];

      if (parentId === 0 || parentId === undefined) {
        setTreeData(newData);
        const keys = getAllExpandKeys(newData);
        setExpandedRowKeys(keys);
      } else {
        setTreeData(prev => updateTreeNode(prev, parentId, newData));
        const newKeys = newData.filter((n: any) => n.childCount > 0).map((n: any) => n.key);
        setExpandedRowKeys(prev => [...prev, ...newKeys]);
      }
    } catch (error) {
      console.error('Failed to fetch tree data:', error);
    } finally {
      setLoading(false);
    }
  }, [dimType, dateRange, caliberType]);

  useEffect(() => {
    setCrossDimType(undefined);
    setCrossTreeData([]);
    setDrillPath([]);
    fetchTreeData(0);
  }, [dimType, dateRange, caliberType]);

  const getAllExpandKeys = (nodes: any[]): string[] => {
    const keys: string[] = [];
    const collect = (list: any[]) => {
      for (const node of list) {
        if (node.childCount > 0) {
          keys.push(node.key);
        }
        if (node.children) {
          collect(node.children);
        }
      }
    };
    collect(nodes);
    return keys;
  };

  const updateTreeNode = (nodes: any[], parentId: number, children: any[]): any[] => {
    return nodes.map(node => {
      if (node.id === parentId) {
        return { ...node, children };
      }
      if (node.children && node.children.length > 0) {
        return { ...node, children: updateTreeNode(node.children, parentId, children) };
      }
      return node;
    });
  };

  const flattenTree = (nodes: any[]): any[] => {
    const result: any[] = [];
    const flatten = (list: any[]) => {
      for (const node of list) {
        result.push(node);
        if (node.children) flatten(node.children);
      }
    };
    flatten(nodes);
    return result;
  };

  const handleQuickSelect = (value: string) => {
    setQuickSelect(value);
    switch (value) {
      case 'today': setDateRange([dayjs(), dayjs()]); break;
      case 'thisMonth': setDateRange([dayjs().startOf('month'), dayjs()]); break;
      case 'thisYear': setDateRange([dayjs().startOf('year'), dayjs()]); break;
      case 'custom': break;
    }
  };

  const handleExport = async () => {
    if (!dimType) return;
    try {
      const startDate = dateRange[0].format('YYYY-MM-DD');
      const endDate = dateRange[1].format('YYYY-MM-DD');
      const response = await fetch(`/api/export/dimension/${dimType}?startDate=${startDate}&endDate=${endDate}&caliberType=${caliberType}`);
      if (!response.ok) throw new Error('导出失败');

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `dimension_${dimType}_${startDate}.xlsx`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      message.success('导出成功');
    } catch (error) {
      message.error('导出失败');
    }
  };

  const handleExpand = async (expanded: boolean, record: any) => {
    const key = record.key;
    if (expanded) {
      setExpandedRowKeys(prev => [...prev, key]);
      if (!record.children || record.children.length === 0) {
        const startDate = dateRange[0].format('YYYY-MM-DD');
        const endDate = dateRange[1].format('YYYY-MM-DD');
        try {
          const result: any = await api.get(`/dimension/${dimType}/tree`, {
            params: { startDate, endDate, caliberType, parentId: record.id }
          });
          setTreeData(prev => updateTreeNode(prev, record.id, result || []));
        } catch (error) {
          console.error('Failed to load children:', error);
        }
      }
    } else {
      setExpandedRowKeys(prev => prev.filter(k => k !== key));
    }
  };

  // 交叉钻取（树状结构）
  const handleCrossDrillTree = async (record: any, targetDim: string) => {
    setCrossLoading(true);
    setCrossDimType(targetDim);
    setCrossDimName(record.name);
    const startDate = dateRange[0].format('YYYY-MM-DD');
    const endDate = dateRange[1].format('YYYY-MM-DD');
    try {
      const result: any = await api.get('/dimension/cross-drill-tree', {
        params: {
          fromDimType: dimType,
          fromDimId: record.id,
          toDimType: targetDim,
          startDate,
          endDate,
          caliberType
        }
      });
      const treeData = result || [];
      setCrossTreeData(treeData);
      // 自动展开所有有子节点的行
      const keys = getAllCrossExpandKeys(treeData);
      setCrossExpandedKeys(keys);

      // 获取钻取路径
      const pathResult: any = await api.get(`/dimension/${dimType}/drill-path/${record.id}`);
      setDrillPath(pathResult || []);
    } catch (error) {
      console.error('Cross drill failed:', error);
    } finally {
      setCrossLoading(false);
    }
  };

  const getAllCrossExpandKeys = (nodes: any[]): string[] => {
    const keys: string[] = [];
    const collect = (list: any[]) => {
      for (const node of list) {
        if (node.children && node.children.length > 0) {
          keys.push(`cross_${node.id}`);
          collect(node.children);
        }
      }
    };
    collect(nodes);
    return keys;
  };

  const handleBreakdown = async (record: any) => {
    setBreakdownVisible(true);
    const startDate = dateRange[0].format('YYYY-MM-DD');
    const endDate = dateRange[1].format('YYYY-MM-DD');
    try {
      const result: any = await api.get(`/dimension/${dimType}/tree`, {
        params: { startDate, endDate, caliberType, parentId: record.id }
      });
      setBreakdownData({
        dimName: record.name,
        totalProfit: record.netProfit,
        loanProfit: record.loanProfit,
        depositProfit: record.depositProfit,
        bySubDimension: (result || []).map((r: any) => ({
          name: r.name, loanProfit: r.loanProfit, depositProfit: r.depositProfit, total: r.netProfit,
        })),
      });
    } catch (error) {
      console.error('Breakdown failed:', error);
    }
  };

  const getCrossMenuItems = (record: any) => {
    return ALL_DIMS.filter(d => d !== dimType).map(dim => ({
      key: dim,
      label: `按${dimLabels[dim]}分析`,
      icon: dimIcons[dim],
      onClick: () => handleCrossDrillTree(record, dim),
    }));
  };

  // 图表
  const getRankingOption = () => {
    const flatData = flattenTree(treeData).sort((a: any, b: any) => b.netProfit - a.netProfit).slice(0, 10).reverse();
    return {
      tooltip: { trigger: 'axis' },
      legend: { data: ['贷款利润', '存款利润'], top: 0, right: 0, textStyle: { fontSize: 11 } },
      grid: { left: '3%', right: '15%', bottom: '3%', top: '10%', containLabel: true },
      xAxis: { type: 'value', name: '万元' },
      yAxis: { type: 'category', data: flatData.map((d: any) => d.name) },
      series: [
        { name: '贷款利润', type: 'bar', stack: 'profit', data: flatData.map((d: any) => d.loanProfit), itemStyle: { color: '#1890ff' } },
        { name: '存款利润', type: 'bar', stack: 'profit', data: flatData.map((d: any) => d.depositProfit), itemStyle: { color: '#722ed1' } },
      ],
    };
  };

  const getCostPieOption = () => {
    const flatData = flattenTree(treeData);
    const totalFtp = flatData.reduce((s: number, d: any) => s + d.ftpCost, 0);
    const totalRisk = flatData.reduce((s: number, d: any) => s + d.riskCost, 0);
    const totalOp = flatData.reduce((s: number, d: any) => s + d.opCost, 0);
    return {
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      series: [{
        type: 'pie', radius: ['35%', '65%'],
        data: [
          { name: 'FTP成本', value: totalFtp, itemStyle: { color: '#fa8c16' } },
          { name: '风险成本', value: totalRisk, itemStyle: { color: '#f5222d' } },
          { name: '运营成本', value: totalOp, itemStyle: { color: '#722ed1' } },
        ],
        label: { formatter: '{b}\n{d}%', fontSize: 10 },
      }],
    };
  };

  // 表格列
  const treeColumns = [
    {
      title: `${dimLabels[dimType || 'ORG']}名称`,
      dataIndex: 'name', fixed: 'left' as const, width: 180,
      render: (text: string, record: any) => (
        <Space size="small">
          {dimIcons[dimType || 'ORG']}
          <span style={{ fontWeight: record.level === 1 ? 600 : 400, fontSize: 12 }}>{text}</span>
          {record.childCount > 0 && <Tag color="blue" style={{ fontSize: 10 }}>{record.childCount}</Tag>}
        </Space>
      ),
    },
    {
      title: '贷款利润', dataIndex: 'loanProfit', width: 100,
      render: (v: number, record: any) => (
        <span style={{ color: '#1890ff', cursor: 'pointer', fontWeight: 'bold', fontSize: 12 }} onClick={() => handleBreakdown(record)}>
          {v?.toLocaleString()}
        </span>
      ),
    },
    {
      title: '存款利润', dataIndex: 'depositProfit', width: 100,
      render: (v: number, record: any) => (
        <span style={{ color: '#722ed1', cursor: 'pointer', fontWeight: 'bold', fontSize: 12 }} onClick={() => handleBreakdown(record)}>
          {v?.toLocaleString()}
        </span>
      ),
    },
    {
      title: '总利润', dataIndex: 'netProfit', width: 110,
      render: (v: number, record: any) => (
        <span style={{ color: '#52c41a', cursor: 'pointer', fontWeight: 'bold', fontSize: 12 }} onClick={() => handleBreakdown(record)}>
          {v?.toLocaleString()}
        </span>
      ),
    },
    { title: '贷款收入', dataIndex: 'loanRevenue', width: 100, render: (v: number) => <span style={{ fontSize: 12 }}>{v?.toLocaleString()}</span> },
    { title: '存款收入', dataIndex: 'depositRevenue', width: 100, render: (v: number) => <span style={{ fontSize: 12 }}>{v?.toLocaleString()}</span> },
    { title: 'FTP成本', dataIndex: 'ftpCost', width: 90, render: (v: number) => <span style={{ fontSize: 12 }}>{v?.toLocaleString()}</span> },
    { title: '风险成本', dataIndex: 'riskCost', width: 90, render: (v: number) => <span style={{ fontSize: 12 }}>{v?.toLocaleString()}</span> },
    { title: '运营成本', dataIndex: 'opCost', width: 90, render: (v: number) => <span style={{ fontSize: 12 }}>{v?.toLocaleString()}</span> },
    {
      title: '状态', dataIndex: 'profitStatus', width: 60,
      render: (v: string) => <Tag color={v === 'PROFIT' ? 'green' : 'red'} style={{ fontSize: 10 }}>{v === 'PROFIT' ? '盈利' : '亏损'}</Tag>,
    },
    {
      title: '操作', width: 100, fixed: 'right' as const,
      render: (_: any, record: any) => (
        <Dropdown menu={{ items: getCrossMenuItems(record) }} trigger={['click']}>
          <Button size="small" type="link" style={{ fontSize: 11 }}>交叉分析 <DownOutlined /></Button>
        </Dropdown>
      ),
    },
  ];

  // 交叉钻取树状表格列
  const crossTreeColumns = [
    {
      title: dimLabels[crossDimType || 'PRODUCT'],
      dataIndex: 'name',
      width: 180,
      render: (text: string, record: any) => (
        <Space size="small">
          {dimIcons[crossDimType || 'PRODUCT']}
          <span style={{ fontWeight: record.level === 1 ? 600 : 400, fontSize: 12 }}>{text}</span>
          {record.childCount > 0 && <Tag color="blue" style={{ fontSize: 10 }}>{record.childCount}</Tag>}
        </Space>
      ),
    },
    {
      title: '贷款利润', dataIndex: 'loanProfit', width: 100,
      render: (v: number) => <span style={{ color: '#1890ff', fontSize: 12 }}>{v?.toLocaleString()}</span>,
    },
    {
      title: '存款利润', dataIndex: 'depositProfit', width: 100,
      render: (v: number) => <span style={{ color: '#722ed1', fontSize: 12 }}>{v?.toLocaleString()}</span>,
    },
    {
      title: '总利润', dataIndex: 'netProfit', width: 110,
      render: (v: number) => <span style={{ color: '#52c41a', fontWeight: 'bold', fontSize: 12 }}>{v?.toLocaleString()}</span>,
    },
    {
      title: '状态', dataIndex: 'isLeaf', width: 60,
      render: (_: any, record: any) => {
        const isProfit = (record.netProfit || 0) >= 0;
        return <Tag color={isProfit ? 'green' : 'red'} style={{ fontSize: 10 }}>{isProfit ? '盈利' : '亏损'}</Tag>;
      },
    },
  ];

  // 分析报告
  const report = (() => {
    const flatData = flattenTree(treeData);
    return {
      totalProfit: flatData.reduce((s: number, d: any) => s + d.netProfit, 0),
      totalLoan: flatData.reduce((s: number, d: any) => s + d.loanProfit, 0),
      totalDeposit: flatData.reduce((s: number, d: any) => s + d.depositProfit, 0),
      top3: [...flatData].sort((a: any, b: any) => b.netProfit - a.netProfit).slice(0, 3),
      bottom3: [...flatData].sort((a: any, b: any) => a.netProfit - b.netProfit).slice(0, 3),
    };
  })();

  return (
    <Spin spinning={loading}>
      <div style={{ marginBottom: 12 }}>
        <h3 style={{ margin: 0 }}>{dimIcons[dimType || 'ORG']} {dimLabels[dimType || 'ORG']}维度分析</h3>
        <p style={{ color: '#999', margin: '2px 0 0', fontSize: 12 }}>点击利润数字查看构成，点击「交叉分析」按其他维度展开</p>
      </div>

      {/* 筛选栏 */}
      <div style={{ marginBottom: 12, display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'center' }}>
        <Select value={quickSelect} onChange={handleQuickSelect} style={{ width: 120 }} size="small"
          options={[
            { value: 'today', label: '今日' },
            { value: 'thisMonth', label: '本月' },
            { value: 'thisYear', label: '本年' },
            { value: 'custom', label: '自定义' },
          ]}
        />
        <RangePicker value={dateRange} onChange={(d: any) => { if (d) { setDateRange(d); setQuickSelect('custom'); } }} format="YYYY-MM-DD" style={{ width: 220 }} size="small" />
        <Button icon={<ReloadOutlined />} onClick={() => fetchTreeData(0)} size="small">刷新</Button>
        <Button icon={<DownloadOutlined />} onClick={handleExport} size="small">导出</Button>
        <Tag color="blue">考核口径</Tag>
      </div>

      <Tabs activeKey={activeTab} onChange={setActiveTab} size="small" items={[
        {
          key: 'data',
          label: '明细数据',
          children: (
            <>
              {/* 图表区 */}
              <Row gutter={12} style={{ marginBottom: 12 }}>
                <Col xs={24} lg={12}>
                  <Card title="盈利排名 TOP10" size="small" bodyStyle={{ padding: '8px 12px' }}>
                    <ReactECharts option={getRankingOption()} style={{ height: 220 }} />
                  </Card>
                </Col>
                <Col xs={24} lg={12}>
                  <Card title="成本结构占比" size="small" bodyStyle={{ padding: '8px 12px' }}>
                    <ReactECharts option={getCostPieOption()} style={{ height: 220 }} />
                  </Card>
                </Col>
              </Row>

              {/* 树形表格 */}
              <Card title={<Space><span>{dimLabels[dimType || 'ORG']}明细</span><Tag style={{ fontSize: 10 }}>{flattenTree(treeData).length}条</Tag></Space>} size="small">
                <Table
                  dataSource={treeData} columns={treeColumns} rowKey="key" size="small"
                  scroll={{ x: 1200 }}
                  expandable={{
                    expandedRowKeys,
                    onExpand: handleExpand,
                    childrenColumnName: 'children',
                  }}
                  pagination={false}
                />
              </Card>

              {/* 交叉分析结果（树状结构） */}
              {crossDimType && crossDimName && (
                <Card
                  title={
                    <Space>
                      <span>{crossDimName} · 按{dimLabels[crossDimType]}分析</span>
                      <Tag color="blue" style={{ fontSize: 10 }}>交叉钻取</Tag>
                    </Space>
                  }
                  extra={
                    <Button size="small" onClick={() => { setCrossTreeData([]); setCrossDimName(undefined); setDrillPath([]); }}>
                      关闭
                    </Button>
                  }
                  style={{ marginTop: 12 }} size="small"
                >
                  {/* 面包屑导航 */}
                  {drillPath.length > 0 && (
                    <Breadcrumb style={{ marginBottom: 12, fontSize: 12 }}>
                      {drillPath.map((item: any, index: number) => (
                        <Breadcrumb.Item key={index}>
                          <span style={{ color: index === drillPath.length - 1 ? '#1890ff' : '#666' }}>
                            {item.name}
                          </span>
                        </Breadcrumb.Item>
                      ))}
                      <Breadcrumb.Item>
                        <span style={{ color: '#1890ff' }}>按{dimLabels[crossDimType]}</span>
                      </Breadcrumb.Item>
                    </Breadcrumb>
                  )}

                  <Spin spinning={crossLoading}>
                    {crossTreeData.length > 0 ? (
                      <Table
                        dataSource={crossTreeData}
                        columns={crossTreeColumns}
                        rowKey={(record) => `cross_${record.id}`}
                        size="small"
                        scroll={{ x: 800 }}
                        expandable={{
                          expandedRowKeys: crossExpandedKeys,
                          onExpand: (expanded, record) => {
                            const key = `cross_${record.id}`;
                            if (expanded) {
                              setCrossExpandedKeys(prev => [...prev, key]);
                            } else {
                              setCrossExpandedKeys(prev => prev.filter(k => k !== key));
                            }
                          },
                          childrenColumnName: 'children',
                        }}
                        pagination={false}
                      />
                    ) : (
                      <div style={{ textAlign: 'center', color: '#999', padding: 30 }}>暂无数据</div>
                    )}
                  </Spin>
                </Card>
              )}
            </>
          ),
        },
        {
          key: 'report',
          label: '分析报告',
          children: (
            <>
              <Card title="📋 经营摘要" size="small" style={{ marginBottom: 12 }}>
                <div style={{ whiteSpace: 'pre-wrap', lineHeight: 1.6, fontSize: 13 }}>
                  {`📊 ${dimLabels[dimType || 'ORG']}维度分析\n\n💰 总利润：${report.totalProfit.toLocaleString()}万元\n🏦 贷款利润：${report.totalLoan.toLocaleString()}万元\n💳 存款利润：${report.totalDeposit.toLocaleString()}万元`}
                </div>
              </Card>
              <Row gutter={12}>
                <Col xs={24} md={12}>
                  <Card title="📈 TOP3 盈利" size="small">
                    <Table dataSource={report.top3} rowKey="name" size="small" pagination={false}
                      columns={[
                        { title: '名称', dataIndex: 'name', width: 100 },
                        { title: '贷款利润', dataIndex: 'loanProfit', render: (v: number) => <span style={{ color: '#1890ff' }}>{v?.toLocaleString()}</span> },
                        { title: '存款利润', dataIndex: 'depositProfit', render: (v: number) => <span style={{ color: '#722ed1' }}>{v?.toLocaleString()}</span> },
                        { title: '总利润', dataIndex: 'netProfit', render: (v: number) => <span style={{ color: '#52c41a', fontWeight: 'bold' }}>{v?.toLocaleString()}</span> },
                      ]}
                    />
                  </Card>
                </Col>
                <Col xs={24} md={12}>
                  <Card title="📉 BOTTOM3" size="small">
                    <Table dataSource={report.bottom3} rowKey="name" size="small" pagination={false}
                      columns={[
                        { title: '名称', dataIndex: 'name', width: 100 },
                        { title: '总利润', dataIndex: 'netProfit', render: (v: number) => <span style={{ color: '#f5222d', fontWeight: 'bold' }}>{v?.toLocaleString()}</span> },
                      ]}
                    />
                  </Card>
                </Col>
              </Row>
            </>
          ),
        },
      ]} />

      {/* 下钻弹窗 */}
      <Modal title={`${breakdownData?.dimName || ''} · 利润构成`} open={breakdownVisible}
        onCancel={() => setBreakdownVisible(false)} footer={null} width={600}>
        {breakdownData && (
          <div>
            <Row gutter={12} style={{ marginBottom: 12 }}>
              <Col span={8}>
                <Card size="small"><div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: 11, color: '#999' }}>总利润</div>
                  <div style={{ fontSize: 20, fontWeight: 'bold', color: '#52c41a' }}>{breakdownData.totalProfit?.toLocaleString()}万</div>
                </div></Card>
              </Col>
              <Col span={8}>
                <Card size="small"><div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: 11, color: '#999' }}>贷款利润</div>
                  <div style={{ fontSize: 20, fontWeight: 'bold', color: '#1890ff' }}>{breakdownData.loanProfit?.toLocaleString()}万</div>
                </div></Card>
              </Col>
              <Col span={8}>
                <Card size="small"><div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: 11, color: '#999' }}>存款利润</div>
                  <div style={{ fontSize: 20, fontWeight: 'bold', color: '#722ed1' }}>{breakdownData.depositProfit?.toLocaleString()}万</div>
                </div></Card>
              </Col>
            </Row>
            <div style={{ fontWeight: 500, marginBottom: 8, fontSize: 13 }}>按下级维度构成：</div>
            <Table dataSource={breakdownData.bySubDimension} rowKey="name" size="small" pagination={false}
              columns={[
                { title: '名称', dataIndex: 'name', width: 100 },
                { title: '贷款利润', dataIndex: 'loanProfit', render: (v: number) => <span style={{ color: '#1890ff' }}>{v?.toLocaleString()}</span> },
                { title: '存款利润', dataIndex: 'depositProfit', render: (v: number) => <span style={{ color: '#722ed1' }}>{v?.toLocaleString()}</span> },
                { title: '总利润', dataIndex: 'total', render: (v: number) => <span style={{ color: '#52c41a', fontWeight: 'bold' }}>{v?.toLocaleString()}</span> },
              ]}
            />
          </div>
        )}
      </Modal>
    </Spin>
  );
};

export default DimensionAnalysis;
