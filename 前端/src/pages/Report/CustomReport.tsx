import React, { useState, useEffect } from 'react';
import { Card, Button, Space, Select, Tag, Table, message, Spin } from 'antd';
import { SaveOutlined, DownloadOutlined, FolderOpenOutlined, ReloadOutlined } from '@ant-design/icons';
import api from '../../services/api';

const { Option } = Select;

const allDims = [
  { value: 'ORG', label: '机构' },
  { value: 'BIZ_LINE', label: '条线' },
  { value: 'PRODUCT', label: '产品' },
  { value: 'CHANNEL', label: '渠道' },
  { value: 'MANAGER', label: '客户经理' },
  { value: 'CUSTOMER', label: '客户' },
];

const allMetrics = [
  { value: 'REV_TOTAL', label: '业务总收入' },
  { value: 'REV_INTEREST', label: '利息收入' },
  { value: 'REV_FEE', label: '手续费收入' },
  { value: 'COST_FTP', label: 'FTP成本' },
  { value: 'COST_RISK', label: '风险成本' },
  { value: 'COST_OP', label: '运营成本' },
  { value: 'PROFIT_NET', label: '净利润' },
  { value: 'COST_INCOME_RATIO', label: '成本收入比' },
  { value: 'PROFIT_MARGIN', label: '利润率' },
  { value: 'SCALE_BIZ_AMT', label: '业务规模' },
  { value: 'SCALE_CUSTOMER_CNT', label: '客户数' },
];

const CustomReport: React.FC = () => {
  const [rowDims, setRowDims] = useState<string[]>(['ORG']);
  const [colMetrics, setColMetrics] = useState<string[]>(['REV_TOTAL', 'PROFIT_NET']);
  const [data, setData] = useState<any[]>([]);
  const [templates, setTemplates] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchTemplates();
  }, []);

  const fetchTemplates = async () => {
    try {
      const result: any = await api.get('/report/custom/templates');
      setTemplates(result || []);
    } catch (error) {
      console.error('Failed to fetch templates:', error);
    }
  };

  const handleGenerate = async () => {
    setLoading(true);
    try {
      const result: any = await api.post('/report/custom/query', {
        rowDims,
        colMetrics,
        period: '2026-05',
        caliberType: 'BOOK',
      });
      setData(result?.list || []);
      message.success('报表生成成功');
    } catch (error: any) {
      message.error(error.message || '生成失败');
    } finally {
      setLoading(false);
    }
  };

  const handleLoadTemplate = (template: any) => {
    try {
      const rowDimsParsed = JSON.parse(template.row_dims);
      const colMetricsParsed = JSON.parse(template.col_metrics);
      setRowDims(rowDimsParsed);
      setColMetrics(colMetricsParsed);
    } catch (error) {
      console.error('Failed to parse template:', error);
    }
  };

  const getDimLabel = (dim: string) => allDims.find(d => d.value === dim)?.label || dim;
  const getMetricLabel = (metric: string) => allMetrics.find(m => m.value === metric)?.label || metric;

  const tableColumns = [
    ...rowDims.map(dim => ({
      title: getDimLabel(dim),
      dataIndex: dim.toLowerCase(),
      fixed: 'left' as const,
      width: 120,
    })),
    ...colMetrics.map(metric => ({
      title: getMetricLabel(metric),
      dataIndex: metric.toLowerCase(),
      render: (v: number) => v != null ? (
        metric.includes('RATIO') || metric.includes('MARGIN') ? v + '%' : v?.toLocaleString()
      ) : '-',
    })),
  ];

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>📝 自定义报表</h2>
        <p style={{ color: '#999', margin: '4px 0 0' }}>自由选择维度和指标，生成个性化报表</p>
      </div>

      <Card style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap' }}>
          <div>
            <div style={{ marginBottom: 8, fontWeight: 500 }}>行维度（可多选）</div>
            <Select mode="multiple" value={rowDims} onChange={setRowDims} style={{ width: 300 }}
              placeholder="选择行维度">
              {allDims.map(d => <Option key={d.value} value={d.value}>{d.label}</Option>)}
            </Select>
          </div>
          <div>
            <div style={{ marginBottom: 8, fontWeight: 500 }}>列指标（可多选）</div>
            <Select mode="multiple" value={colMetrics} onChange={setColMetrics} style={{ width: 400 }}
              placeholder="选择列指标">
              {allMetrics.map(m => <Option key={m.value} value={m.value}>{m.label}</Option>)}
            </Select>
          </div>
          <div style={{ display: 'flex', alignItems: 'flex-end', gap: 8 }}>
            <Button type="primary" onClick={handleGenerate} loading={loading}>生成报表</Button>
            <Button icon={<SaveOutlined />}>保存模板</Button>
            <Button icon={<DownloadOutlined />}>导出</Button>
          </div>
        </div>
      </Card>

      {templates.length > 0 && (
        <Card title="预设模板" size="small" style={{ marginBottom: 16 }}>
          <Space wrap>
            {templates.map((t: any) => (
              <Tag key={t.id} style={{ cursor: 'pointer', padding: '4px 12px' }}
                onClick={() => handleLoadTemplate(t)}>
                <FolderOpenOutlined /> {t.name}
                {t.is_system === 1 && <Tag color="blue" style={{ marginLeft: 4 }}>系统</Tag>}
              </Tag>
            ))}
          </Space>
        </Card>
      )}

      <Card title="报表预览">
        <Spin spinning={loading}>
          {data.length > 0 ? (
            <Table
              dataSource={data}
              columns={tableColumns}
              rowKey={(_, index) => String(index)}
              size="small"
              scroll={{ x: 800 }}
              pagination={{ pageSize: 20 }}
            />
          ) : (
            <div style={{ textAlign: 'center', color: '#999', padding: 60 }}>
              选择维度和指标后点击「生成报表」
            </div>
          )}
        </Spin>
      </Card>
    </div>
  );
};

export default CustomReport;
