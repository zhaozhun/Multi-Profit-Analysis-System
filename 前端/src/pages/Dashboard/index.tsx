import React, { useState, useEffect, useCallback } from 'react';
import { Row, Col, Card, Select, Spin, Tag, Table, Tabs, Space, Button, DatePicker, List, Descriptions, Statistic, Divider, message } from 'antd';
import {
  ReloadOutlined, DownloadOutlined, WarningOutlined, FileTextOutlined,
  CheckCircleOutlined, ExclamationCircleOutlined, CloseCircleOutlined,
} from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import dayjs from 'dayjs';
import api from '../../services/api';

const { RangePicker } = DatePicker;

// 统一卡片高度
const CARD_HEIGHT = 120;
const CHART_HEIGHT = 250;

const Dashboard: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [caliberType] = useState('BOOK');
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs]>([dayjs('2025-06-01'), dayjs('2025-06-30')]);
  const [quickSelect, setQuickSelect] = useState('thisMonth');
  const [activeTab, setActiveTab] = useState('overview');
  const [kpiCards, setKpiCards] = useState<any[]>([]);
  const [waterfall, setWaterfall] = useState<any>(null);
  const [trend, setTrend] = useState<any>(null);
  const [dimOverviews, setDimOverviews] = useState<any[]>([]);
  const [activeDimTab, setActiveDimTab] = useState('ORG');
  const [alerts, setAlerts] = useState<any[]>([]);
  const [analysisReport, setAnalysisReport] = useState<any>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    const startDate = dateRange[0].format('YYYY-MM-DD');
    const endDate = dateRange[1].format('YYYY-MM-DD');
    const period = dateRange[0].format('YYYY-MM');

    try {
      const [dashboardRes, alertsRes, reportRes]: any[] = await Promise.all([
        api.get('/dashboard/overview', { params: { startDate, endDate, caliberType } }),
        api.post(`/governance/monitor?period=${period}`).catch(() => []),
        api.get('/report/analysis', { params: { period, caliberType } }).catch(() => null),
      ]);

      setKpiCards(dashboardRes.kpiCards || []);
      setWaterfall(dashboardRes.waterfall);
      setTrend(dashboardRes.trend);
      setDimOverviews(dashboardRes.dimOverviews || []);
      setAlerts(alertsRes || []);
      setAnalysisReport(reportRes);
    } catch (error) {
      console.error('Failed to fetch dashboard data:', error);
    } finally {
      setLoading(false);
    }
  }, [dateRange, caliberType]);

  useEffect(() => { fetchData(); }, [fetchData]);

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
    try {
      const period = dateRange[0].format('YYYY-MM');
      const response = await fetch(`/api/export/dashboard?period=${period}&caliberType=${caliberType}`);
      if (!response.ok) throw new Error('导出失败');

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `dashboard_${period}.xlsx`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      message.success('导出成功');
    } catch (error) {
      message.error('导出失败');
    }
  };

  const handleExportReport = async () => {
    try {
      const period = dateRange[0].format('YYYY-MM');
      const response = await fetch(`/api/export/report?period=${period}&caliberType=${caliberType}`);
      if (!response.ok) throw new Error('导出失败');
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `operation_report_${period}.xlsx`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      message.success('导出成功');
    } catch (error) {
      message.error('导出失败');
    }
  };

  const getCard = (name: string) => kpiCards.find((c: any) => c.metricName === name);
  const totalProfit = getCard('总利润');
  const loanProfit = getCard('贷款利润');
  const depositProfit = getCard('存款利润');
  const loanRevenue = getCard('贷款收入');
  const depositRevenue = getCard('存款收入');
  const ftpCost = getCard('FTP成本');
  const riskCost = getCard('风险成本');
  const opCost = getCard('运营成本');

  // 计算关键指标
  const totalRevenue = (loanRevenue?.value || 0) + (depositRevenue?.value || 0);
  const totalCost = (ftpCost?.value || 0) + (riskCost?.value || 0) + (opCost?.value || 0);
  const costIncomeRatio = totalRevenue > 0 ? Math.round(totalCost / totalRevenue * 100) : 0;

  // 分析报告数据
  const reportOverview = analysisReport?.overview || {};
  const totalRevenueReport = Number(reportOverview.total_revenue || 0);
  const totalProfitReport = Number(reportOverview.total_profit || 0);
  const totalCostReport = Number(reportOverview.total_ftp_cost || 0) + Number(reportOverview.total_risk_cost || 0) + Number(reportOverview.total_op_cost || 0);
  const costIncomeRatioReport = totalRevenueReport > 0 ? (totalCostReport / totalRevenueReport * 100).toFixed(2) : '0';
  const profitMargin = totalRevenueReport > 0 ? (totalProfitReport / totalRevenueReport * 100).toFixed(2) : '0';
  const riskCostRate = Number(reportOverview.loan_revenue || 0) > 0 ? (Number(reportOverview.total_risk_cost || 0) / Number(reportOverview.loan_revenue || 0) * 100).toFixed(2) : '0';

  // 贷款瀑布图
  const getLoanWaterfallOption = () => ({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['利息收入', 'FTP成本', '风险成本', '运营成本', '贷款利润'] },
    yAxis: { type: 'value', name: '万元' },
    series: [{
      type: 'bar',
      data: [
        { value: loanRevenue?.value || 0, itemStyle: { color: '#1890ff' } },
        { value: -(ftpCost?.value || 0), itemStyle: { color: '#fa8c16' } },
        { value: -(riskCost?.value || 0), itemStyle: { color: '#f5222d' } },
        { value: -(opCost?.value || 0), itemStyle: { color: '#722ed1' } },
        { value: loanProfit?.value || 0, itemStyle: { color: '#36cfc9' } },
      ],
      barWidth: '50%',
      label: { show: true, position: 'top', formatter: (p: any) => (p.value / 10000).toFixed(1) + '亿', fontSize: 10 },
    }],
  });

  // 存款瀑布图
  const getDepositWaterfallOption = () => ({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['FTP收入', '利息支出', '运营成本', '存款利润'] },
    yAxis: { type: 'value', name: '万元' },
    series: [{
      type: 'bar',
      data: [
        { value: depositRevenue?.value || 0, itemStyle: { color: '#b37feb' } },
        { value: -(depositRevenue?.value ? Math.round(depositRevenue.value * 0.6) : 0), itemStyle: { color: '#ff85c0' } },
        { value: -(depositRevenue?.value ? Math.round(depositRevenue.value * 0.2) : 0), itemStyle: { color: '#ffc53d' } },
        { value: depositProfit?.value || 0, itemStyle: { color: '#95de64' } },
      ],
      barWidth: '50%',
      label: { show: true, position: 'top', formatter: (p: any) => (p.value / 10000).toFixed(1) + '亿', fontSize: 10 },
    }],
  });

  // 趋势图
  const getTrendOption = () => {
    if (!trend) return {};
    return {
      tooltip: { trigger: 'axis' },
      legend: { data: ['业务收入', '净利润'], top: 0, right: 0, textStyle: { fontSize: 11 } },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '12%', containLabel: true },
      xAxis: { type: 'category', data: trend.months, axisLabel: { fontSize: 10 } },
      yAxis: [
        { type: 'value', name: '收入', position: 'left', axisLabel: { fontSize: 10 } },
        { type: 'value', name: '利润', position: 'right', axisLabel: { fontSize: 10 } },
      ],
      series: [
        { name: '业务收入', type: 'bar', data: trend.revenueTrend, itemStyle: { color: '#1890ff' }, barWidth: '40%' },
        { name: '净利润', type: 'line', yAxisIndex: 1, data: trend.profitTrend, itemStyle: { color: '#52c41a' }, smooth: true, symbol: 'none' },
      ],
    };
  };

  // 维度饼图
  const getDimPieOption = (dim: any) => ({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      data: dim.pieItems?.map((item: any) => ({ name: item.name, value: Math.abs(item.value) })) || [],
      label: { formatter: '{b}\n{d}%', fontSize: 11 },
    }],
  });

  // 获取告警图标
  const getAlertIcon = (level: string) => {
    if (level === 'CRITICAL') return <CloseCircleOutlined style={{ color: '#f5222d' }} />;
    if (level === 'WARNING') return <ExclamationCircleOutlined style={{ color: '#fa8c16' }} />;
    return <CheckCircleOutlined style={{ color: '#52c41a' }} />;
  };

  // 机构排名列定义
  const orgColumns = [
    { title: '排名', render: (_: any, __: any, i: number) => i + 1, width: 60 },
    { title: '机构名称', dataIndex: 'name', width: 120 },
    { title: '净利润（万元）', dataIndex: 'profit', render: (v: number) => (
      <span style={{ color: v >= 0 ? '#52c41a' : '#f5222d', fontWeight: 'bold' }}>{v?.toLocaleString()}</span>
    )},
    { title: '利润率', dataIndex: 'margin', render: (v: number) => v ? `${v}%` : '-' },
  ];

  // 产品排名列定义
  const prodColumns = [
    { title: '排名', render: (_: any, __: any, i: number) => i + 1, width: 60 },
    { title: '产品名称', dataIndex: 'name', width: 150 },
    { title: '业务规模（万元）', dataIndex: 'amount', render: (v: number) => v?.toLocaleString() },
    { title: '净利润（万元）', dataIndex: 'profit', render: (v: number) => (
      <span style={{ color: v >= 0 ? '#52c41a' : '#f5222d', fontWeight: 'bold' }}>{v?.toLocaleString()}</span>
    )},
  ];

  // 渠道分析列定义
  const channelColumns = [
    { title: '渠道', dataIndex: 'name', width: 120 },
    { title: '收入（万元）', dataIndex: 'revenue', render: (v: number) => v?.toLocaleString() },
    { title: '净利润（万元）', dataIndex: 'profit', render: (v: number) => (
      <span style={{ color: v >= 0 ? '#52c41a' : '#f5222d', fontWeight: 'bold' }}>{v?.toLocaleString()}</span>
    )},
  ];

  // 客户经理列定义
  const managerColumns = [
    { title: '排名', render: (_: any, __: any, i: number) => i + 1, width: 60 },
    { title: '客户经理', dataIndex: 'name', width: 100 },
    { title: '所属机构', dataIndex: 'org_name', width: 120 },
    { title: '净利润（万元）', dataIndex: 'profit', render: (v: number) => (
      <span style={{ color: v >= 0 ? '#52c41a' : '#f5222d', fontWeight: 'bold' }}>{v?.toLocaleString()}</span>
    )},
  ];

  // 渲染经营总览Tab
  const renderOverview = () => (
    <>
      {/* 第一行：KPI卡片 */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col xs={12} md={6}>
          <Card size="small" style={{ height: CARD_HEIGHT, background: 'linear-gradient(135deg, #f6ffed 0%, #e6f7ff 100%)' }}>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 13, color: '#666', marginBottom: 8 }}>总利润</div>
              <div style={{ fontSize: 36, fontWeight: 'bold', color: '#52c41a', lineHeight: 1 }}>
                {totalProfit?.value ? (totalProfit.value / 10000).toFixed(2) : '-'}
              </div>
              <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>亿元</div>
            </div>
          </Card>
        </Col>
        <Col xs={12} md={6}>
          <Card size="small" style={{ height: CARD_HEIGHT }}>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 13, color: '#666', marginBottom: 8 }}>贷款利润</div>
              <div style={{ fontSize: 36, fontWeight: 'bold', color: '#1890ff', lineHeight: 1 }}>
                {loanProfit?.value ? (loanProfit.value / 10000).toFixed(2) : '-'}
              </div>
              <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>亿元</div>
            </div>
          </Card>
        </Col>
        <Col xs={12} md={6}>
          <Card size="small" style={{ height: CARD_HEIGHT }}>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 13, color: '#666', marginBottom: 8 }}>存款利润</div>
              <div style={{ fontSize: 36, fontWeight: 'bold', color: '#722ed1', lineHeight: 1 }}>
                {depositProfit?.value ? (depositProfit.value / 10000).toFixed(2) : '-'}
              </div>
              <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>亿元</div>
            </div>
          </Card>
        </Col>
        <Col xs={12} md={6}>
          <Card size="small" style={{ height: CARD_HEIGHT }}>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 13, color: '#666', marginBottom: 8 }}>成本收入比</div>
              <div style={{ fontSize: 36, fontWeight: 'bold', color: costIncomeRatio > 70 ? '#f5222d' : '#52c41a', lineHeight: 1 }}>
                {costIncomeRatio}%
              </div>
              <div style={{ fontSize: 12, color: costIncomeRatio > 70 ? '#f5222d' : '#52c41a', marginTop: 4 }}>
                {costIncomeRatio > 70 ? '偏高' : '正常'}
              </div>
            </div>
          </Card>
        </Col>
      </Row>

      {/* 第二行：瀑布图 */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col xs={24} lg={12}>
          <Card title="贷款利润瀑布图" size="small" bodyStyle={{ padding: '12px' }}>
            <ReactECharts option={getLoanWaterfallOption()} style={{ height: CHART_HEIGHT }} />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="存款利润瀑布图" size="small" bodyStyle={{ padding: '12px' }}>
            <ReactECharts option={getDepositWaterfallOption()} style={{ height: CHART_HEIGHT }} />
          </Card>
        </Col>
      </Row>

      {/* 第三行：趋势图 + 异常告警 */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col xs={24} lg={14}>
          <Card title="盈利趋势" size="small" bodyStyle={{ padding: '12px' }}>
            <ReactECharts option={getTrendOption()} style={{ height: CHART_HEIGHT }} />
          </Card>
        </Col>
        <Col xs={24} lg={10}>
          <Card
            title={<Space><WarningOutlined style={{ color: '#fa8c16' }} /><span>异常告警</span><Tag color="red">{alerts.length}</Tag></Space>}
            size="small"
            bodyStyle={{ padding: '12px', height: CHART_HEIGHT, overflow: 'auto' }}
          >
            {alerts.length === 0 ? (
              <div style={{ textAlign: 'center', color: '#999', padding: 40 }}>
                <CheckCircleOutlined style={{ fontSize: 32, color: '#52c41a', marginBottom: 12 }} />
                <div>暂无异常告警</div>
              </div>
            ) : (
              <List
                size="small"
                dataSource={alerts.slice(0, 5)}
                renderItem={(alert: any) => (
                  <List.Item style={{ padding: '8px 0' }}>
                    <List.Item.Meta
                      avatar={getAlertIcon(alert.level)}
                      title={<span style={{ fontSize: 13 }}>{alert.message}</span>}
                      description={<span style={{ fontSize: 12, color: '#999' }}>{alert.aiAnalysis}</span>}
                    />
                  </List.Item>
                )}
              />
            )}
          </Card>
        </Col>
      </Row>

      {/* 第四行：维度概览 */}
      <Card title="维度盈利概览" size="small" bodyStyle={{ padding: '12px' }}>
        <Tabs activeKey={activeDimTab} onChange={setActiveDimTab} size="small"
          items={dimOverviews.map((dim: any) => ({
            key: dim.dimType, label: dim.dimName,
            children: (
              <Row gutter={16}>
                <Col xs={24} md={12}>
                  <Table dataSource={dim.topItems} rowKey="name" size="small" pagination={false}
                    columns={[
                      { title: '#', render: (_: any, __: any, i: number) => i + 1, width: 40 },
                      { title: '名称', dataIndex: 'name' },
                      { title: '净利润', dataIndex: 'netProfit', render: (v: number) => v?.toLocaleString() },
                    ]}
                  />
                </Col>
                <Col xs={24} md={12}>
                  <ReactECharts option={getDimPieOption(dim)} style={{ height: 300 }} />
                </Col>
              </Row>
            ),
          }))}
        />
      </Card>
    </>
  );

  // 渲染分析报告Tab
  const renderReport = () => (
    <>
      {/* 一、整体经营概况 */}
      <Card title="一、整体经营概况" size="small" style={{ marginBottom: 16 }}>
        <Descriptions bordered column={4} size="small">
          <Descriptions.Item label="总收入">
            <Statistic value={totalRevenueReport} precision={0} suffix="万元" valueStyle={{ fontSize: 14 }} />
          </Descriptions.Item>
          <Descriptions.Item label="总成本">
            <Statistic value={totalCostReport} precision={0} suffix="万元" valueStyle={{ fontSize: 14, color: '#f5222d' }} />
          </Descriptions.Item>
          <Descriptions.Item label="净利润">
            <Statistic value={totalProfitReport} precision={0} suffix="万元" valueStyle={{ fontSize: 14, color: '#52c41a' }} />
          </Descriptions.Item>
          <Descriptions.Item label="成本收入比">
            <Statistic value={costIncomeRatioReport} suffix="%" valueStyle={{ fontSize: 14, color: Number(costIncomeRatioReport) > 70 ? '#f5222d' : '#52c41a' }} />
          </Descriptions.Item>
        </Descriptions>

        <Divider style={{ margin: '12px 0' }} />

        <Descriptions bordered column={4} size="small">
          <Descriptions.Item label="贷款收入">
            <Statistic value={Number(reportOverview.loan_revenue || 0)} precision={0} suffix="万元" valueStyle={{ fontSize: 14 }} />
          </Descriptions.Item>
          <Descriptions.Item label="贷款利润">
            <Statistic value={Number(reportOverview.loan_profit || 0)} precision={0} suffix="万元" valueStyle={{ fontSize: 14, color: '#1890ff' }} />
          </Descriptions.Item>
          <Descriptions.Item label="存款收入">
            <Statistic value={Number(reportOverview.deposit_revenue || 0)} precision={0} suffix="万元" valueStyle={{ fontSize: 14 }} />
          </Descriptions.Item>
          <Descriptions.Item label="存款利润">
            <Statistic value={Number(reportOverview.deposit_profit || 0)} precision={0} suffix="万元" valueStyle={{ fontSize: 14, color: '#722ed1' }} />
          </Descriptions.Item>
        </Descriptions>

        <Divider style={{ margin: '12px 0' }} />

        <Row gutter={16}>
          <Col span={8}>
            <Card size="small" title="利润率分析">
              <div style={{ lineHeight: 2, fontSize: 13 }}>
                <div>• 整体利润率: <strong>{profitMargin}%</strong></div>
                <div>• 贷款利润率: <strong>{Number(reportOverview.loan_revenue || 0) > 0 ? (Number(reportOverview.loan_profit || 0) / Number(reportOverview.loan_revenue || 0) * 100).toFixed(2) : '0'}%</strong></div>
                <div>• 存款利润率: <strong>{Number(reportOverview.deposit_revenue || 0) > 0 ? (Number(reportOverview.deposit_profit || 0) / Number(reportOverview.deposit_revenue || 0) * 100).toFixed(2) : '0'}%</strong></div>
              </div>
            </Card>
          </Col>
          <Col span={8}>
            <Card size="small" title="成本结构分析">
              <div style={{ lineHeight: 2, fontSize: 13 }}>
                <div>• FTP成本占比: <strong>{totalRevenueReport > 0 ? (Number(reportOverview.total_ftp_cost || 0) / totalRevenueReport * 100).toFixed(2) : '0'}%</strong></div>
                <div>• 风险成本占比: <strong>{totalRevenueReport > 0 ? (Number(reportOverview.total_risk_cost || 0) / totalRevenueReport * 100).toFixed(2) : '0'}%</strong></div>
                <div>• 运营成本占比: <strong>{totalRevenueReport > 0 ? (Number(reportOverview.total_op_cost || 0) / totalRevenueReport * 100).toFixed(2) : '0'}%</strong></div>
              </div>
            </Card>
          </Col>
          <Col span={8}>
            <Card size="small" title="风险指标">
              <div style={{ lineHeight: 2, fontSize: 13 }}>
                <div>• 风险成本率: <strong style={{ color: Number(riskCostRate) > 10 ? '#f5222d' : '#52c41a' }}>{riskCostRate}%</strong></div>
                <div>• 成本收入比: <strong style={{ color: Number(costIncomeRatioReport) > 70 ? '#f5222d' : '#52c41a' }}>{costIncomeRatioReport}%</strong></div>
                <div>• 风险状态: <Tag color={Number(riskCostRate) > 10 ? 'red' : 'green'}>{Number(riskCostRate) > 10 ? '需关注' : '正常'}</Tag></div>
              </div>
            </Card>
          </Col>
        </Row>
      </Card>

      {/* 二、问题与风险提示 */}
      {analysisReport?.issues && analysisReport.issues.length > 0 && (
        <Card title="二、问题与风险提示" size="small" style={{ marginBottom: 16 }}>
          <Table
            dataSource={analysisReport.issues}
            rowKey={(record, index) => index?.toString() || '0'}
            size="small"
            pagination={false}
            columns={[
              { title: '类型', dataIndex: 'type', width: 100, render: (v: string) => (
                <Tag color={v === 'LOSS_ORG' ? 'red' : v === 'LOSS_PRODUCT' ? 'orange' : 'blue'}>
                  {v === 'LOSS_ORG' ? '亏损机构' : v === 'LOSS_PRODUCT' ? '亏损产品' : '低利润'}
                </Tag>
              )},
              { title: '级别', dataIndex: 'level', width: 80, render: (v: string) => (
                <Tag color={v === 'HIGH' ? 'red' : 'orange'}>{v === 'HIGH' ? '高' : '中'}</Tag>
              )},
              { title: '名称', dataIndex: 'name', width: 120 },
              { title: '问题描述', dataIndex: 'description' },
            ]}
          />
        </Card>
      )}

      {/* 三、机构盈利排名 */}
      <Card title="三、机构盈利排名" size="small" style={{ marginBottom: 16 }}>
        <Table
          dataSource={analysisReport?.orgRanking || []}
          columns={orgColumns}
          rowKey="name"
          size="small"
          pagination={false}
        />
      </Card>

      {/* 四、产品损益排名 */}
      <Card title="四、产品损益排名" size="small" style={{ marginBottom: 16 }}>
        <Table
          dataSource={analysisReport?.prodRanking || []}
          columns={prodColumns}
          rowKey="name"
          size="small"
          pagination={false}
        />
      </Card>

      {/* 五、渠道贡献分析 */}
      <Card title="五、渠道贡献分析" size="small" style={{ marginBottom: 16 }}>
        <Table
          dataSource={analysisReport?.channelAnalysis || []}
          columns={channelColumns}
          rowKey="name"
          size="small"
          pagination={false}
        />
      </Card>

      {/* 六、客户经理绩效 */}
      <Card title="六、客户经理绩效 TOP10" size="small" style={{ marginBottom: 16 }}>
        <Table
          dataSource={analysisReport?.managerPerformance || []}
          columns={managerColumns}
          rowKey="name"
          size="small"
          pagination={false}
        />
      </Card>

      {/* 七、建议与措施 */}
      <Card title="七、建议与措施" size="small" style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          <Col span={12}>
            <Card size="small" title="📈 经营建议">
              <div style={{ lineHeight: 2.5, fontSize: 13 }}>
                {analysisReport?.suggestions?.map((s: string, i: number) => (
                  <div key={i}>{i + 1}. {s}</div>
                ))}
              </div>
            </Card>
          </Col>
          <Col span={12}>
            <Card size="small" title="⚠️ 风险提示">
              <div style={{ lineHeight: 2.5, fontSize: 13 }}>
                {analysisReport?.issues?.filter((i: any) => i.level === 'HIGH').map((issue: any, i: number) => (
                  <div key={i} style={{ color: '#f5222d' }}>{i + 1}. {issue.description}</div>
                ))}
                {(!analysisReport?.issues || analysisReport.issues.filter((i: any) => i.level === 'HIGH').length === 0) && (
                  <div style={{ color: '#52c41a' }}>暂无高风险问题</div>
                )}
              </div>
            </Card>
          </Col>
        </Row>
      </Card>

      {/* 报告说明 */}
      <Card size="small" style={{ background: '#f6ffed' }}>
        <div style={{ fontSize: 12, color: '#666' }}>
          <strong>报告说明：</strong>
          <div>• 报告期间：{dateRange[0].format('YYYY-MM')} | 口径：考核口径</div>
          <div>• 数据来源：多维盈利分析系统 | 生成时间：{new Date().toLocaleString()}</div>
          <div>• 本报告基于系统数据自动生成，如有疑问请联系数据分析团队</div>
        </div>
      </Card>
    </>
  );

  return (
    <Spin spinning={loading}>
      {/* 筛选栏 */}
      <div style={{ marginBottom: 16, display: 'flex', gap: 12, alignItems: 'center' }}>
        <Select value={quickSelect} onChange={handleQuickSelect} style={{ width: 120 }} size="small"
          options={[
            { value: 'today', label: '今日' },
            { value: 'thisMonth', label: '本月' },
            { value: 'thisYear', label: '本年' },
            { value: 'custom', label: '自定义' },
          ]}
        />
        <RangePicker value={dateRange} onChange={(d: any) => { if (d) { setDateRange(d); setQuickSelect('custom'); } }} format="YYYY-MM-DD" style={{ width: 220 }} size="small" />
        <Button icon={<ReloadOutlined />} onClick={fetchData} size="small">刷新</Button>
        <Button icon={<DownloadOutlined />} onClick={activeTab === 'overview' ? handleExport : handleExportReport} size="small">
          {activeTab === 'overview' ? '导出数据' : '导出报告'}
        </Button>
        <Tag color="blue">考核口径</Tag>
      </div>

      {/* Tab页切换 */}
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        size="large"
        style={{ marginBottom: 0 }}
        items={[
          {
            key: 'overview',
            label: (
              <span>
                <DashboardOutlined style={{ marginRight: 4 }} />
                经营总览
              </span>
            ),
            children: renderOverview(),
          },
          {
            key: 'report',
            label: (
              <span>
                <FileTextOutlined style={{ marginRight: 4 }} />
                经营分析报告
              </span>
            ),
            children: renderReport(),
          },
        ]}
      />
    </Spin>
  );
};

// 需要导入 DashboardOutlined
import { DashboardOutlined } from '@ant-design/icons';

export default Dashboard;
