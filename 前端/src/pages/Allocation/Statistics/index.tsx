// @ts-nocheck
import React, { useState, useEffect } from 'react';
import {
  Card,
  Row,
  Col,
  DatePicker,
  Select,
  Button,
  message,
  Statistic,
  Table,
  Tag,
  Space,
  Tabs,
  Empty
} from 'antd';
import {
  ReloadOutlined,
  PieChartOutlined,
  BarChartOutlined,
  LineChartOutlined
} from '@ant-design/icons';
import { costTypeApi } from '@/services/api';
import ReactECharts from 'echarts-for-react';
import dayjs from 'dayjs';

const { Option } = Select;
const { TabPane } = Tabs;

interface CostSummary {
  costCode: string;
  costName: string;
  totalAmount: number;
  level: number;
  costCategory: string;
  costNature: string;
  allocationRequired: boolean;
}

const StatisticsPage: React.FC = () => {
  const [period, setPeriod] = useState<dayjs.Dayjs>(dayjs('2025-06-01'));
  const [summary, setSummary] = useState<CostSummary[]>([]);
  const [natureSummary, setNatureSummary] = useState<Record<string, number>>({});
  const [categorySummary, setCategorySummary] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(false);

  // 费用性质映射
  const natureNames: Record<string, string> = {
    'OPERATION': '运营费用',
    'MANAGEMENT': '管理费用',
    'SALES': '销售费用',
    'FINANCE': '财务费用',
    'HR': '人力成本',
    'IT': '信息技术'
  };

  // 费用类别映射
  const categoryNames: Record<string, string> = {
    'FIXED': '固定费用',
    'VARIABLE': '变动费用',
    'DIRECT': '直接费用'
  };

  // 加载统计数据
  const loadStatistics = async () => {
    setLoading(true);
    try {
      const [summaryRes, natureRes, categoryRes] = await Promise.all([
        costTypeApi.getCostSummary(period.format('YYYY-MM')),
        costTypeApi.getSummaryByNature(period.format('YYYY-MM')),
        costTypeApi.getSummaryByCategory(period.format('YYYY-MM'))
      ]);

      if (Array.isArray(summaryRes)) {
        setSummary(summaryRes.data);
      }
      if (Array.isArray(natureRes)) {
        setNatureSummary(natureRes.data);
      }
      if (Array.isArray(categoryRes)) {
        setCategorySummary(categoryRes.data);
      }
    } catch (error) {
      message.error('加载统计数据失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadStatistics();
  }, [period]);

  // 计算统计
  const totalAmount = summary.reduce((sum, item) => sum + item.totalAmount, 0);
  const fixedAmount = categorySummary['FIXED'] || 0;
  const variableAmount = categorySummary['VARIABLE'] || 0;
  const directAmount = categorySummary['DIRECT'] || 0;

  // 按性质汇总饼图配置
  const getNaturePieOption = () => {
    const data = Object.entries(natureSummary).map(([key, value]) => ({
      name: natureNames[key] || key,
      value
    }));

    return {
      title: {
        text: '费用性质分布',
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
      series: [{
        name: '费用金额',
        type: 'pie',
        radius: '50%',
        data,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }]
    };
  };

  // 按类别汇总饼图配置
  const getCategoryPieOption = () => {
    const data = Object.entries(categorySummary).map(([key, value]) => ({
      name: categoryNames[key] || key,
      value
    }));

    return {
      title: {
        text: '费用类别分布',
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
      series: [{
        name: '费用金额',
        type: 'pie',
        radius: '50%',
        data,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }]
    };
  };

  // 费用排名柱状图配置
  const getRankBarOption = () => {
    const top10 = summary
      .filter(item => item.level === 2)
      .sort((a, b) => b.totalAmount - a.totalAmount)
      .slice(0, 10);

    return {
      title: {
        text: '费用类型排名（Top 10）',
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
        data: top10.map(item => item.costName),
        axisLabel: {
          rotate: 45,
          interval: 0
        }
      },
      yAxis: {
        type: 'value',
        name: '金额',
        axisLabel: {
          formatter: (value: number) => {
            if (value >= 10000) {
              return (value / 10000).toFixed(1) + '万';
            }
            return value;
          }
        }
      },
      series: [{
        name: '费用金额',
        type: 'bar',
        data: top10.map(item => item.totalAmount),
        itemStyle: {
          color: '#1890ff'
        },
        label: {
          show: true,
          position: 'top',
          formatter: (params: any) => {
            if (params.value >= 10000) {
              return (params.value / 10000).toFixed(2) + '万';
            }
            return params.value;
          }
        }
      }]
    };
  };

  // 表格列定义
  const columns = [
    {
      title: '费用编码',
      dataIndex: 'costCode',
      key: 'costCode',
      width: 120
    },
    {
      title: '费用名称',
      dataIndex: 'costName',
      key: 'costName',
      width: 150
    },
    {
      title: '层级',
      dataIndex: 'level',
      key: 'level',
      width: 80,
      render: (level: number) => (
        <Tag>{level === 1 ? '大类' : level === 2 ? '中类' : '小类'}</Tag>
      )
    },
    {
      title: '费用性质',
      dataIndex: 'costNature',
      key: 'costNature',
      width: 100,
      render: (nature: string) => <Tag>{natureNames[nature] || nature}</Tag>
    },
    {
      title: '费用类别',
      dataIndex: 'costCategory',
      key: 'costCategory',
      width: 100,
      render: (category: string) => <Tag>{categoryNames[category] || category}</Tag>
    },
    {
      title: '金额',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      width: 120,
      render: (val: number) => val?.toLocaleString(),
      sorter: (a: CostSummary, b: CostSummary) => a.totalAmount - b.totalAmount
    },
    {
      title: '占比',
      key: 'ratio',
      width: 100,
      render: (_: any, record: any) => {
        const ratio = totalAmount > 0 ? (record.totalAmount / totalAmount * 100).toFixed(2) : '0';
        return ratio + '%';
      }
    },
    {
      title: '需要分摊',
      dataIndex: 'allocationRequired',
      key: 'allocationRequired',
      width: 100,
      render: (val: boolean) => val ? <Tag color="green">是</Tag> : <Tag color="red">否</Tag>
    }
  ];

  return (
    <div>
      <Card title="费用统计分析">
        {/* 筛选条件 */}
        <Space style={{ marginBottom: 16 }}>
          <DatePicker
            picker="month"
            format="YYYY-MM"
            value={period}
            onChange={(date) => date && setPeriod(date)}
          />
          <Button
            icon={<ReloadOutlined />}
            onClick={loadStatistics}
          >
            刷新
          </Button>
        </Space>

        {/* 统计卡片 */}
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={6}>
            <Card>
              <Statistic
                title="总费用"
                value={totalAmount}
                precision={2}
                suffix="元"
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="固定费用"
                value={fixedAmount}
                precision={2}
                suffix="元"
                valueStyle={{ color: '#52c41a' }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="变动费用"
                value={variableAmount}
                precision={2}
                suffix="元"
                valueStyle={{ color: '#faad14' }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="直接费用"
                value={directAmount}
                precision={2}
                suffix="元"
                valueStyle={{ color: '#1890ff' }}
              />
            </Card>
          </Col>
        </Row>

        {/* 图表和表格 */}
        <Tabs defaultActiveKey="chart">
          <TabPane tab={<span><PieChartOutlined /> 图表分析</span>} key="chart">
            <Row gutter={16}>
              <Col span={12}>
                <Card>
                  {Object.keys(natureSummary).length > 0 ? (
                    <ReactECharts option={getNaturePieOption()} style={{ height: 400 }} />
                  ) : (
                    <Empty description="暂无数据" />
                  )}
                </Card>
              </Col>
              <Col span={12}>
                <Card>
                  {Object.keys(categorySummary).length > 0 ? (
                    <ReactECharts option={getCategoryPieOption()} style={{ height: 400 }} />
                  ) : (
                    <Empty description="暂无数据" />
                  )}
                </Card>
              </Col>
            </Row>
            <Card style={{ marginTop: 16 }}>
              {summary.length > 0 ? (
                <ReactECharts option={getRankBarOption()} style={{ height: 400 }} />
              ) : (
                <Empty description="暂无数据" />
              )}
            </Card>
          </TabPane>

          <TabPane tab={<span><BarChartOutlined /> 明细数据</span>} key="detail">
            <Table
              columns={columns}
              dataSource={summary}
              rowKey="costCode"
              loading={loading}
              pagination={{ pageSize: 20 }}
              scroll={{ x: 1200 }}
              summary={() => (
                <Table.Summary fixed>
                  <Table.Summary.Row>
                    <Table.Summary.Cell index={0} colSpan={5}>
                      <strong>合计</strong>
                    </Table.Summary.Cell>
                    <Table.Summary.Cell index={5}>
                      <strong>{totalAmount.toLocaleString()}</strong>
                    </Table.Summary.Cell>
                    <Table.Summary.Cell index={6}>
                      <strong>100%</strong>
                    </Table.Summary.Cell>
                    <Table.Summary.Cell index={7} />
                  </Table.Summary.Row>
                </Table.Summary>
              )}
            />
          </TabPane>
        </Tabs>
      </Card>
    </div>
  );
};

export default StatisticsPage;
