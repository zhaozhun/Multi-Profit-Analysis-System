import React, { useState, useEffect, useCallback } from 'react';
import { Card, Button, Space, Select, Spin, Tag, Table, Row, Col, Statistic, Alert, List, message } from 'antd';
import {
  ExperimentOutlined, FileTextOutlined, BarChartOutlined, AlertOutlined,
  AimOutlined, DownloadOutlined, WarningOutlined, CheckCircleOutlined,
} from '@ant-design/icons';
import api from '../../services/api';

const AiReport: React.FC = () => {
  const [period, setPeriod] = useState('2026-05');
  const [caliber, setCaliber] = useState('ASSESS');
  const [loading, setLoading] = useState(false);
  const [reportData, setReportData] = useState<any>(null);

  const fetchReport = useCallback(async () => {
    setLoading(true);
    try {
      const result: any = await api.get('/report/analysis', { params: { period, caliberType: caliber } });
      setReportData(result);
      message.success('报告生成成功');
    } catch (error) {
      message.error('报告生成失败');
    } finally {
      setLoading(false);
    }
  }, [period, caliber]);

  useEffect(() => {
    fetchReport();
  }, [fetchReport]);

  const handleExport = async () => {
    try {
      const response = await fetch(`/api/export/report?period=${period}&caliberType=${caliber}`);
      if (!response.ok) throw new Error('导出失败');
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `analysis_report_${period}.xlsx`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      message.success('导出成功');
    } catch (error) {
      message.error('导出失败');
    }
  };

  if (!reportData) {
    return <Spin spinning={loading}><div style={{ height: 400 }} /></Spin>;
  }

  const { overview, orgRanking, prodRanking, channelAnalysis, managerPerformance, issues, suggestions } = reportData;

  const totalRevenue = Number(overview?.total_revenue || 0);
  const totalProfit = Number(overview?.total_profit || 0);
  const totalCost = Number(overview?.total_ftp_cost || 0) + Number(overview?.total_risk_cost || 0) + Number(overview?.total_op_cost || 0);
  const costIncomeRatio = totalRevenue > 0 ? (totalCost / totalRevenue * 100).toFixed(2) : '0';
  const profitMargin = totalRevenue > 0 ? (totalProfit / totalRevenue * 100).toFixed(2) : '0';

  return (
    <Spin spinning={loading}>
      <div style={{ marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}><ExperimentOutlined style={{ marginRight: 8 }} />经营分析报告</h2>
        <p style={{ color: '#999', margin: '4px 0 0' }}>基于多维度数据的综合经营分析</p>
      </div>

      {/* 筛选栏 */}
      <div style={{ marginBottom: 16, display: 'flex', gap: 12, alignItems: 'center' }}>
        <Select value={period} onChange={setPeriod} style={{ width: 130 }}>
          <Select.Option value="2026-05">2026年5月</Select.Option>
          <Select.Option value="2026-04">2026年4月</Select.Option>
          <Select.Option value="2026-03">2026年3月</Select.Option>
        </Select>
        <Select value={caliber} onChange={setCaliber} style={{ width: 120 }}>
          <Select.Option value="BOOK">账面口径</Select.Option>
          <Select.Option value="ASSESS">考核口径</Select.Option>
        </Select>
        <Button icon={<DownloadOutlined />} onClick={handleExport}>导出报告</Button>
      </div>

      {/* 一、整体经营概况 */}
      <Card title="一、整体经营概况" size="small" style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          <Col span={6}>
            <Statistic title="总收入" value={totalRevenue} precision={0} suffix="万元"
              valueStyle={{ color: '#1890ff' }} />
          </Col>
          <Col span={6}>
            <Statistic title="总成本" value={totalCost} precision={0} suffix="万元"
              valueStyle={{ color: '#f5222d' }} />
          </Col>
          <Col span={6}>
            <Statistic title="净利润" value={totalProfit} precision={0} suffix="万元"
              valueStyle={{ color: '#52c41a' }} />
          </Col>
          <Col span={6}>
            <Statistic title="成本收入比" value={costIncomeRatio} suffix="%"
              valueStyle={{ color: '#fa8c16' }} />
          </Col>
        </Row>
        <Row gutter={16} style={{ marginTop: 16 }}>
          <Col span={6}>
            <Statistic title="贷款收入" value={Number(overview?.loan_revenue || 0)} precision={0} suffix="万元" />
          </Col>
          <Col span={6}>
            <Statistic title="贷款利润" value={Number(overview?.loan_profit || 0)} precision={0} suffix="万元"
              valueStyle={{ color: '#1890ff' }} />
          </Col>
          <Col span={6}>
            <Statistic title="存款收入" value={Number(overview?.deposit_revenue || 0)} precision={0} suffix="万元" />
          </Col>
          <Col span={6}>
            <Statistic title="存款利润" value={Number(overview?.deposit_profit || 0)} precision={0} suffix="万元"
              valueStyle={{ color: '#722ed1' }} />
          </Col>
        </Row>
      </Card>

      {/* 二、问题与风险 */}
      {issues && issues.length > 0 && (
        <Card title="二、问题与风险提示" size="small" style={{ marginBottom: 16 }}>
          <List
            dataSource={issues}
            renderItem={(item: any) => (
              <List.Item>
                <Alert
                  type={item.level === 'HIGH' ? 'error' : 'warning'}
                  showIcon
                  icon={item.level === 'HIGH' ? <WarningOutlined /> : <AlertOutlined />}
                  message={<span style={{ fontWeight: 500 }}>{item.type === 'LOSS_ORG' ? '亏损机构' : item.type === 'LOSS_PRODUCT' ? '亏损产品' : '低利润'}</span>}
                  description={item.description}
                  style={{ width: '100%' }}
                />
              </List.Item>
            )}
          />
        </Card>
      )}

      {/* 三、机构分析 */}
      <Card title="三、机构盈利排名" size="small" style={{ marginBottom: 16 }}>
        <Table
          dataSource={orgRanking || []}
          rowKey="name"
          size="small"
          pagination={false}
          columns={[
            { title: '排名', render: (_: any, __: any, i: number) => i + 1, width: 60 },
            { title: '机构', dataIndex: 'name', width: 120 },
            { title: '净利润（万元）', dataIndex: 'profit', render: (v: number) => (
              <span style={{ color: v >= 0 ? '#52c41a' : '#f5222d', fontWeight: 'bold' }}>{v?.toLocaleString()}</span>
            )},
            { title: '利润率', dataIndex: 'margin', render: (v: number) => v ? `${v}%` : '-' },
          ]}
        />
      </Card>

      {/* 四、产品分析 */}
      <Card title="四、产品损益排名" size="small" style={{ marginBottom: 16 }}>
        <Table
          dataSource={prodRanking || []}
          rowKey="name"
          size="small"
          pagination={false}
          columns={[
            { title: '排名', render: (_: any, __: any, i: number) => i + 1, width: 60 },
            { title: '产品', dataIndex: 'name', width: 150 },
            { title: '业务规模（万元）', dataIndex: 'amount', render: (v: number) => v?.toLocaleString() },
            { title: '净利润（万元）', dataIndex: 'profit', render: (v: number) => (
              <span style={{ color: v >= 0 ? '#52c41a' : '#f5222d', fontWeight: 'bold' }}>{v?.toLocaleString()}</span>
            )},
          ]}
        />
      </Card>

      {/* 五、渠道分析 */}
      <Card title="五、渠道贡献分析" size="small" style={{ marginBottom: 16 }}>
        <Table
          dataSource={channelAnalysis || []}
          rowKey="name"
          size="small"
          pagination={false}
          columns={[
            { title: '渠道', dataIndex: 'name', width: 120 },
            { title: '收入（万元）', dataIndex: 'revenue', render: (v: number) => v?.toLocaleString() },
            { title: '净利润（万元）', dataIndex: 'profit', render: (v: number) => (
              <span style={{ color: v >= 0 ? '#52c41a' : '#f5222d', fontWeight: 'bold' }}>{v?.toLocaleString()}</span>
            )},
          ]}
        />
      </Card>

      {/* 六、客户经理绩效 */}
      <Card title="六、客户经理绩效 TOP10" size="small" style={{ marginBottom: 16 }}>
        <Table
          dataSource={managerPerformance || []}
          rowKey="name"
          size="small"
          pagination={false}
          columns={[
            { title: '排名', render: (_: any, __: any, i: number) => i + 1, width: 60 },
            { title: '客户经理', dataIndex: 'name', width: 100 },
            { title: '所属机构', dataIndex: 'org_name', width: 120 },
            { title: '净利润（万元）', dataIndex: 'profit', render: (v: number) => (
              <span style={{ color: v >= 0 ? '#52c41a' : '#f5222d', fontWeight: 'bold' }}>{v?.toLocaleString()}</span>
            )},
          ]}
        />
      </Card>

      {/* 七、建议与注意事项 */}
      <Card title="七、建议与注意事项" size="small">
        <List
          dataSource={suggestions || []}
          renderItem={(item: string, index: number) => (
            <List.Item>
              <List.Item.Meta
                avatar={<CheckCircleOutlined style={{ color: '#52c41a', fontSize: 16 }} />}
                title={<span>{index + 1}. {item}</span>}
              />
            </List.Item>
          )}
        />
      </Card>
    </Spin>
  );
};

export default AiReport;
