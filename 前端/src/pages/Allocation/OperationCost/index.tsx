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
  PlayCircleOutlined,
  ReloadOutlined,
  BarChartOutlined,
  PieChartOutlined
} from '@ant-design/icons';
import { operationCostApi } from '@/services/api';
import ReactECharts from 'echarts-for-react';
import dayjs from 'dayjs';

const { Option } = Select;
const { TabPane } = Tabs;

interface OperationCostResult {
  id: number;
  batchNo: string;
  period: string;
  costCode: string;
  costName: string;
  costType: string;
  totalAmount: number;
  targetType: string;
  targetCode: string;
  targetName: string;
  allocatedAmount: number;
  allocationFactor: string;
  factorValue: number;
  factorRatio: number;
}

interface SummaryItem {
  costType?: string;
  costName?: string;
  employeeCode?: string;
  employeeName?: string;
  totalAmount?: number;
  totalAllocated?: number;
}

const OperationCostPage: React.FC = () => {
  const [period, setPeriod] = useState<dayjs.Dayjs>(dayjs('2025-06-01'));
  const [costType, setCostType] = useState<string>('');
  const [results, setResults] = useState<OperationCostResult[]>([]);
  const [costTypeSummary, setCostTypeSummary] = useState<SummaryItem[]>([]);
  const [employeeSummary, setEmployeeSummary] = useState<SummaryItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [executing, setExecuting] = useState(false);

  // 费用类型选项
  const costTypes = [
    { code: '', name: '全部' },
    { code: 'RENT', name: '房租物业' },
    { code: 'UTILITIES', name: '水电费' },
    { code: 'WORKSTATION', name: '工位费' },
    { code: 'REIMBURSE', name: '报销费用' },
    { code: 'COLLECTION', name: '催收费用' },
    { code: 'DATA_FEE', name: '数据使用费' },
    { code: 'IT_OPS', name: 'IT运维费' },
    { code: 'MARKETING', name: '营销费用' },
    { code: 'TRAINING', name: '培训费用' },
    { code: 'ADMIN', name: '行政费用' }
  ];

  // 加载数据
  const loadData = async () => {
    setLoading(true);
    try {
      const [resultsRes, costTypeRes, employeeRes] = await Promise.all([
        operationCostApi.getAllocationResults(period.format('YYYY-MM'), costType || undefined),
        operationCostApi.getSummaryByCostType(period.format('YYYY-MM')),
        operationCostApi.getSummaryByEmployee(period.format('YYYY-MM'))
      ]);

      if (resultsRes.success) setResults(resultsRes.data);
      if (costTypeRes.success) setCostTypeSummary(costTypeRes.data);
      if (employeeRes.success) setEmployeeSummary(employeeRes.data);
    } catch (error) {
      message.error('加载数据失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [period, costType]);

  // 执行分摊
  const handleExecute = async () => {
    setExecuting(true);
    try {
      const response = await operationCostApi.executeAllocation(
        period.format('YYYY-MM'),
        costType || undefined
      );

      if (response) {
        message.success('运营费用分摊执行成功');
        loadData();
      } else {
        message.error('分摊执行失败');
      }
    } catch (error) {
      message.error('分摊执行失败');
    } finally {
      setExecuting(false);
    }
  };

  // 计算统计
  const totalAmount = results.reduce((sum, r) => sum + r.allocatedAmount, 0);
  const uniqueCostTypes = new Set(results.map(r => r.costCode)).size;
  const uniqueTargets = new Set(results.map(r => r.targetCode)).size;

  // 费用类型分布饼图
  const getCostTypePieOption = () => {
    const data = costTypeSummary.map(item => ({
      name: item.costName || item.costType,
      value: item.totalAmount || 0
    }));

    return {
      title: {
        text: '费用类型分布',
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

  // 员工分摊排名柱状图
  const getEmployeeBarOption = () => {
    const top10 = employeeSummary
      .filter(item => item.totalAllocated)
      .sort((a, b) => (b.totalAllocated || 0) - (a.totalAllocated || 0))
      .slice(0, 10);

    return {
      title: {
        text: '员工费用分摊排名（Top 10）',
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
        data: top10.map(item => item.employeeName || item.employeeCode),
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
        name: '分摊金额',
        type: 'bar',
        data: top10.map(item => item.totalAllocated || 0),
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

  // 分摊明细表格列
  const detailColumns = [
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
      width: 120
    },
    {
      title: '费用类型',
      dataIndex: 'costType',
      key: 'costType',
      width: 100,
      render: (type: string) => {
        const t = costTypes.find(c => c.code === type);
        return <Tag>{t ? t.name : type}</Tag>;
      }
    },
    {
      title: '总金额',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      width: 120,
      render: (val: number) => val?.toLocaleString()
    },
    {
      title: '目标类型',
      dataIndex: 'targetType',
      key: 'targetType',
      width: 100,
      render: (type: string) => (
        <Tag>{type === 'EMPLOYEE' ? '员工' : type === 'DEPT' ? '部门' : '机构'}</Tag>
      )
    },
    {
      title: '目标编码',
      dataIndex: 'targetCode',
      key: 'targetCode',
      width: 100
    },
    {
      title: '目标名称',
      dataIndex: 'targetName',
      key: 'targetName',
      width: 100
    },
    {
      title: '分摊金额',
      dataIndex: 'allocatedAmount',
      key: 'allocatedAmount',
      width: 120,
      render: (val: number) => val?.toLocaleString(),
      sorter: (a: OperationCostResult, b: OperationCostResult) => a.allocatedAmount - b.allocatedAmount
    },
    {
      title: '分摊因子',
      dataIndex: 'allocationFactor',
      key: 'allocationFactor',
      width: 120
    },
    {
      title: '因子占比',
      dataIndex: 'factorRatio',
      key: 'factorRatio',
      width: 100,
      render: (val: number) => val ? (val * 100).toFixed(2) + '%' : '-'
    }
  ];

  // 费用类型汇总表格列
  const costTypeColumns = [
    {
      title: '费用类型',
      dataIndex: 'costType',
      key: 'costType',
      width: 120
    },
    {
      title: '费用名称',
      dataIndex: 'costName',
      key: 'costName',
      width: 150
    },
    {
      title: '金额',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      width: 120,
      render: (val: number) => val?.toLocaleString(),
      sorter: (a: SummaryItem, b: SummaryItem) => (a.totalAmount || 0) - (b.totalAmount || 0)
    }
  ];

  // 员工汇总表格列
  const employeeColumns = [
    {
      title: '员工编码',
      dataIndex: 'employeeCode',
      key: 'employeeCode',
      width: 100
    },
    {
      title: '员工姓名',
      dataIndex: 'employeeName',
      key: 'employeeName',
      width: 100
    },
    {
      title: '分摊总额',
      dataIndex: 'totalAllocated',
      key: 'totalAllocated',
      width: 120,
      render: (val: number) => val?.toLocaleString(),
      sorter: (a: SummaryItem, b: SummaryItem) => (a.totalAllocated || 0) - (b.totalAllocated || 0)
    }
  ];

  return (
    <div>
      <Card title="运营费用分摊">
        {/* 操作栏 */}
        <Space style={{ marginBottom: 16 }}>
          <DatePicker
            picker="month"
            format="YYYY-MM"
            value={period}
            onChange={(date) => date && setPeriod(date)}
          />
          <Select
            placeholder="费用类型"
            style={{ width: 150 }}
            value={costType || undefined}
            onChange={(value) => setCostType(value || '')}
            allowClear
          >
            {costTypes.map(type => (
              <Option key={type.code} value={type.code}>{type.name}</Option>
            ))}
          </Select>
          <Button
            type="primary"
            icon={<PlayCircleOutlined />}
            onClick={handleExecute}
            loading={executing}
          >
            执行分摊
          </Button>
          <Button
            icon={<ReloadOutlined />}
            onClick={loadData}
          >
            刷新
          </Button>
        </Space>

        {/* 统计卡片 */}
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={6}>
            <Card>
              <Statistic title="分摊总额" value={totalAmount} precision={2} suffix="元" />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="费用类型数" value={uniqueCostTypes} suffix="种" />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="分摊目标数" value={uniqueTargets} suffix="个" />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="分摊记录数" value={results.length} suffix="条" />
            </Card>
          </Col>
        </Row>

        {/* 图表和表格 */}
        <Tabs defaultActiveKey="chart">
          <TabPane tab={<span><PieChartOutlined /> 图表分析</span>} key="chart">
            <Row gutter={16}>
              <Col span={12}>
                <Card>
                  {costTypeSummary.length > 0 ? (
                    <ReactECharts option={getCostTypePieOption()} style={{ height: 400 }} />
                  ) : (
                    <Empty description="暂无数据" />
                  )}
                </Card>
              </Col>
              <Col span={12}>
                <Card>
                  {employeeSummary.length > 0 ? (
                    <ReactECharts option={getEmployeeBarOption()} style={{ height: 400 }} />
                  ) : (
                    <Empty description="暂无数据" />
                  )}
                </Card>
              </Col>
            </Row>
          </TabPane>

          <TabPane tab={<span><BarChartOutlined /> 费用类型汇总</span>} key="costType">
            <Table
              columns={costTypeColumns}
              dataSource={costTypeSummary}
              rowKey="costType"
              loading={loading}
              pagination={false}
            />
          </TabPane>

          <TabPane tab="员工汇总" key="employee">
            <Table
              columns={employeeColumns}
              dataSource={employeeSummary}
              rowKey="employeeCode"
              loading={loading}
              pagination={{ pageSize: 20 }}
            />
          </TabPane>

          <TabPane tab="分摊明细" key="detail">
            <Table
              columns={detailColumns}
              dataSource={results}
              rowKey="id"
              loading={loading}
              pagination={{ pageSize: 20 }}
              scroll={{ x: 1500 }}
              summary={() => (
                <Table.Summary fixed>
                  <Table.Summary.Row>
                    <Table.Summary.Cell index={0} colSpan={7}>
                      <strong>合计</strong>
                    </Table.Summary.Cell>
                    <Table.Summary.Cell index={7}>
                      <strong>{totalAmount.toLocaleString()}</strong>
                    </Table.Summary.Cell>
                    <Table.Summary.Cell index={8} colSpan={2} />
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

export default OperationCostPage;
