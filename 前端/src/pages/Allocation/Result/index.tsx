// @ts-nocheck
import React, { useState, useEffect } from 'react';
import {
  Card,
  Table,
  Button,
  Space,
  Select,
  DatePicker,
  message,
  Tag,
  Row,
  Col,
  Statistic,
  Tabs,
  Empty
} from 'antd';
import {
  ReloadOutlined,
  DownloadOutlined,
  BarChartOutlined,
  PieChartOutlined
} from '@ant-design/icons';
import { allocationApi } from '@/services/api';
import ReactECharts from 'echarts-for-react';
import dayjs from 'dayjs';

const { Option } = Select;
const { TabPane } = Tabs;

interface Batch {
  id: number;
  batchNo: string;
  period: string;
  costType: string;
  totalAmount: number;
  allocatedAmount: number;
  recordCount: number;
  status: string;
}

interface SummaryItem {
  targetDimCode: string;
  allocatedAmount: number;
}

interface DetailItem {
  id: number;
  ruleCode: string;
  ruleName: string;
  costType: string;
  sourceDimType: string;
  sourceDimCode: string;
  targetDimType: string;
  targetDimCode: string;
  originalAmount: number;
  allocatedAmount: number;
  allocationRatio: number;
  algorithmCode: string;
}

const ResultPage: React.FC = () => {
  const [period, setPeriod] = useState<dayjs.Dayjs>(dayjs('2025-06-01'));
  const [costType, setCostType] = useState<string>('');
  const [batches, setBatches] = useState<Batch[]>([]);
  const [selectedBatch, setSelectedBatch] = useState<Batch | null>(null);
  const [summary, setSummary] = useState<SummaryItem[]>([]);
  const [details, setDetails] = useState<DetailItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [summaryLoading, setSummaryLoading] = useState(false);

  // 成本类型选项
  const costTypes = [
    { code: '', name: '全部' },
    { code: 'RENT', name: '房租物业' },
    { code: 'SALARY', name: '人力成本' },
    { code: 'IT', name: '信息技术' },
    { code: 'MARKETING', name: '营销费用' },
    { code: 'ADMIN', name: '行政费用' },
    { code: 'RISK', name: '风险准备' }
  ];

  // 加载批次列表
  const loadBatches = async () => {
    setLoading(true);
    try {
      const response = await allocationApi.getResults(period.format('YYYY-MM'), costType || undefined);
      if (response) {
        setBatches(response.data);
        if (response.data.length > 0) {
          setSelectedBatch(response.data[0]);
          loadSummary(response.data[0].id);
          loadDetails(response.data[0].id);
        }
      }
    } catch (error) {
      message.error('加载数据失败');
    } finally {
      setLoading(false);
    }
  };

  // 加载汇总数据
  const loadSummary = async (batchId: number) => {
    setSummaryLoading(true);
    try {
      const response = await allocationApi.getAllocationSummary(batchId);
      if (response) {
        setSummary(response.data);
      }
    } catch (error) {
      message.error('加载汇总数据失败');
    } finally {
      setSummaryLoading(false);
    }
  };

  // 加载明细数据
  const loadDetails = async (batchId: number) => {
    try {
      const response = await allocationApi.getResultDetail(batchId);
      if (response) {
        setDetails(response.data.details || []);
      }
    } catch (error) {
      message.error('加载明细数据失败');
    }
  };

  useEffect(() => {
    loadBatches();
  }, [period, costType]);

  // 选择批次
  const handleSelectBatch = (batch: Batch) => {
    setSelectedBatch(batch);
    loadSummary(batch.id);
    loadDetails(batch.id);
  };

  // 汇总图表配置
  const getSummaryChartOption = () => {
    if (!summary || summary.length === 0) return {};

    return {
      title: {
        text: '分摊金额分布',
        left: 'center'
      },
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 'left',
        top: 'middle'
      },
      series: [
        {
          name: '分摊金额',
          type: 'pie',
          radius: '50%',
          data: summary.map(item => ({
            name: item.targetDimCode,
            value: item.allocatedAmount
          })),
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }
      ]
    };
  };

  // 柱状图配置
  const getBarChartOption = () => {
    if (!summary || summary.length === 0) return {};

    return {
      title: {
        text: '分摊金额排名',
        left: 'center'
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      xAxis: {
        type: 'category',
        data: summary.map(item => item.targetDimCode),
        axisLabel: {
          rotate: 45
        }
      },
      yAxis: {
        type: 'value',
        name: '金额'
      },
      series: [
        {
          name: '分摊金额',
          type: 'bar',
          data: summary.map(item => item.allocatedAmount),
          itemStyle: {
            color: '#1890ff'
          }
        }
      ]
    };
  };

  // 汇总表格列定义
  const summaryColumns = [
    {
      title: '目标维度',
      dataIndex: 'targetDimCode',
      key: 'targetDimCode',
      width: 150
    },
    {
      title: '分摊金额',
      dataIndex: 'allocatedAmount',
      key: 'allocatedAmount',
      width: 150,
      render: (val: number) => val?.toLocaleString(),
      sorter: (a: SummaryItem, b: SummaryItem) => a.allocatedAmount - b.allocatedAmount
    },
    {
      title: '占比',
      key: 'ratio',
      width: 100,
      render: (_: any, record: any) => {
        const total = summary.reduce((sum, item) => sum + item.allocatedAmount, 0);
        const ratio = total > 0 ? (record.allocatedAmount / total * 100).toFixed(2) : '0';
        return ratio + '%';
      }
    }
  ];

  // 明细表格列定义
  const detailColumns = [
    {
      title: '规则',
      dataIndex: 'ruleName',
      key: 'ruleName',
      width: 150
    },
    {
      title: '成本类型',
      dataIndex: 'costType',
      key: 'costType',
      width: 100,
      render: (code: string) => {
        const type = costTypes.find(t => t.code === code);
        return type ? type.name : code;
      }
    },
    {
      title: '来源',
      dataIndex: 'sourceDimCode',
      key: 'sourceDimCode',
      width: 100
    },
    {
      title: '目标',
      dataIndex: 'targetDimCode',
      key: 'targetDimCode',
      width: 100
    },
    {
      title: '原始金额',
      dataIndex: 'originalAmount',
      key: 'originalAmount',
      width: 120,
      render: (val: number) => val?.toLocaleString()
    },
    {
      title: '分摊金额',
      dataIndex: 'allocatedAmount',
      key: 'allocatedAmount',
      width: 120,
      render: (val: number) => val?.toLocaleString(),
      sorter: (a: DetailItem, b: DetailItem) => a.allocatedAmount - b.allocatedAmount
    },
    {
      title: '比例',
      dataIndex: 'allocationRatio',
      key: 'allocationRatio',
      width: 100,
      render: (val: number) => val ? (val * 100).toFixed(2) + '%' : '-'
    },
    {
      title: '算法',
      dataIndex: 'algorithmCode',
      key: 'algorithmCode',
      width: 100
    }
  ];

  // 计算统计信息
  const totalAmount = summary.reduce((sum, item) => sum + item.allocatedAmount, 0);
  const maxItem = summary.length > 0 ? summary.reduce((max, item) =>
    item.allocatedAmount > max.allocatedAmount ? item : max
  ) : null;
  const minItem = summary.length > 0 ? summary.reduce((min, item) =>
    item.allocatedAmount < min.allocatedAmount ? item : min
  ) : null;

  return (
    <div>
      <Card title="分摊结果分析">
        <Space style={{ marginBottom: 16 }}>
          <DatePicker
            picker="month"
            format="YYYY-MM"
            value={period}
            onChange={(date) => date && setPeriod(date)}
          />
          <Select
            value={costType}
            onChange={setCostType}
            style={{ width: 150 }}
          >
            {costTypes.map(type => (
              <Option key={type.code} value={type.code}>{type.name}</Option>
            ))}
          </Select>
          <Button
            icon={<ReloadOutlined />}
            onClick={loadBatches}
          >
            刷新
          </Button>
        </Space>

        {/* 批次列表 */}
        <Card title="分摊批次" size="small" style={{ marginBottom: 16 }}>
          <Table
            columns={[
              { title: '批次号', dataIndex: 'batchNo', width: 180 },
              { title: '期间', dataIndex: 'period', width: 100 },
              {
                title: '成本类型',
                dataIndex: 'costType',
                width: 100,
                render: (code: string) => {
                  if (!code) return '全部';
                  const type = costTypes.find(t => t.code === code);
                  return type ? type.name : code;
                }
              },
              {
                title: '总金额',
                dataIndex: 'totalAmount',
                width: 120,
                render: (val: number) => val?.toLocaleString()
              },
              {
                title: '已分摊',
                dataIndex: 'allocatedAmount',
                width: 120,
                render: (val: number) => val?.toLocaleString()
              },
              { title: '记录数', dataIndex: 'recordCount', width: 80 },
              {
                title: '状态',
                dataIndex: 'status',
                width: 80,
                render: (status: string) => (
                  <Tag color={status === 'COMPLETED' ? 'green' : 'red'}>
                    {status === 'COMPLETED' ? '完成' : status}
                  </Tag>
                )
              },
              {
                title: '操作',
                width: 100,
                render: (_: any, record: any) => (
                  <Button
                    type="link"
                    onClick={() => handleSelectBatch(record)}
                  >
                    查看
                  </Button>
                )
              }
            ]}
            dataSource={batches}
            rowKey="id"
            loading={loading}
            pagination={false}
            size="small"
          />
        </Card>

        {selectedBatch && (
          <>
            {/* 统计卡片 */}
            <Row gutter={16} style={{ marginBottom: 16 }}>
              <Col span={6}>
                <Card>
                  <Statistic
                    title="总分摊金额"
                    value={totalAmount}
                    precision={2}
                    suffix="元"
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card>
                  <Statistic
                    title="分摊记录数"
                    value={summary.length}
                    suffix="条"
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card>
                  <Statistic
                    title="最大分摊"
                    value={maxItem?.allocatedAmount || 0}
                    precision={2}
                    suffix={maxItem?.targetDimCode}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card>
                  <Statistic
                    title="最小分摊"
                    value={minItem?.allocatedAmount || 0}
                    precision={2}
                    suffix={minItem?.targetDimCode}
                  />
                </Card>
              </Col>
            </Row>

            {/* 图表和表格 */}
            <Tabs defaultActiveKey="chart">
              <TabPane tab={<span><PieChartOutlined /> 图表分析</span>} key="chart">
                <Row gutter={16}>
                  <Col span={12}>
                    <Card title="饼图分布">
                      {summary.length > 0 ? (
                        <ReactECharts option={getSummaryChartOption()} style={{ height: 400 }} />
                      ) : (
                        <Empty description="暂无数据" />
                      )}
                    </Card>
                  </Col>
                  <Col span={12}>
                    <Card title="柱状图排名">
                      {summary.length > 0 ? (
                        <ReactECharts option={getBarChartOption()} style={{ height: 400 }} />
                      ) : (
                        <Empty description="暂无数据" />
                      )}
                    </Card>
                  </Col>
                </Row>
              </TabPane>

              <TabPane tab={<span><BarChartOutlined /> 汇总数据</span>} key="summary">
                <Table
                  columns={summaryColumns}
                  dataSource={summary}
                  rowKey="targetDimCode"
                  loading={summaryLoading}
                  pagination={{ pageSize: 20 }}
                />
              </TabPane>

              <TabPane tab="分摊明细" key="detail">
                <Table
                  columns={detailColumns}
                  dataSource={details}
                  rowKey="id"
                  pagination={{ pageSize: 20 }}
                  scroll={{ x: 1200 }}
                />
              </TabPane>
            </Tabs>
          </>
        )}
      </Card>
    </div>
  );
};

export default ResultPage;
