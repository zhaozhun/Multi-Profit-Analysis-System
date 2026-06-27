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
import { employeeCostApi } from '@/services/api';
import ReactECharts from 'echarts-for-react';
import dayjs from 'dayjs';

const { Option } = Select;
const { TabPane } = Tabs;

interface EmployeeAllocation {
  id: number;
  batchNo: string;
  period: string;
  employeeCode: string;
  employeeName: string;
  orgCode: string;
  deptCode: string;
  costType: string;
  costTypeName: string;
  originalAmount: number;
  allocatedAmount: number;
  allocationFactor: string;
  factorValue: number;
  factorRatio: number;
}

interface SummaryItem {
  employeeCode: string;
  employeeName: string;
  orgCode: string;
  deptCode: string;
  totalAllocated: number;
}

const EmployeeAllocationPage: React.FC = () => {
  const [period, setPeriod] = useState<dayjs.Dayjs>(dayjs('2025-06-01'));
  const [costType, setCostType] = useState<string>('');
  const [factorType, setFactorType] = useState<string>('EMPLOYEE_COUNT');
  const [allocations, setAllocations] = useState<EmployeeAllocation[]>([]);
  const [employeeSummary, setEmployeeSummary] = useState<SummaryItem[]>([]);
  const [deptSummary, setDeptSummary] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [executing, setExecuting] = useState(false);

  // 成本类型选项
  const costTypes = [
    { code: 'RENT', name: '房租物业' },
    { code: 'UTILITIES', name: '水电费' },
    { code: 'WELFARE', name: '福利费用' },
    { code: 'TRAINING', name: '培训费用' },
    { code: 'ADMIN', name: '行政费用' },
    { code: 'OPERATION', name: '运营费用' }
  ];

  // 分摊因子选项
  const factorTypes = [
    { code: 'EMPLOYEE_COUNT', name: '按人数' },
    { code: 'WORK_HOURS', name: '按工时' },
    { code: 'SALARY', name: '按薪资' },
    { code: 'WORKSTATION_AREA', name: '按工位面积' }
  ];

  // 加载数据
  const loadData = async () => {
    setLoading(true);
    try {
      const [allocRes, empSummaryRes, deptSummaryRes] = await Promise.all([
        employeeCostApi.getAllocations(period.format('YYYY-MM'), costType || undefined),
        employeeCostApi.getEmployeeSummary(period.format('YYYY-MM')),
        employeeCostApi.getDeptSummary(period.format('YYYY-MM'))
      ]);

      if (Array.isArray(allocRes)) setAllocations(allocRes);
      if (Array.isArray(empSummaryRes)) setEmployeeSummary(empSummaryRes);
      if (Array.isArray(deptSummaryRes)) setDeptSummary(deptSummaryRes);
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
    if (!costType) {
      message.warning('请选择成本类型');
      return;
    }

    setExecuting(true);
    try {
      const response = await employeeCostApi.executeAllocation(
        period.format('YYYY-MM'),
        costType,
        factorType
      );

      if (response) {
        message.success('分摊执行成功');
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
  const totalAllocated = allocations.reduce((sum, a) => sum + a.allocatedAmount, 0);
  const uniqueEmployees = new Set(allocations.map(a => a.employeeCode)).size;
  const uniqueDepts = new Set(allocations.map(a => a.deptCode)).size;

  // 员工分摊排名柱状图
  const getEmployeeBarOption = () => {
    const top10 = employeeSummary.slice(0, 10);

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
        data: top10.map(item => item.employeeName),
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
        data: top10.map(item => item.totalAllocated),
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

  // 部门分摊饼图
  const getDeptPieOption = () => {
    const data = deptSummary.map(item => ({
      name: item.deptName || item.deptCode,
      value: item.totalAllocated
    }));

    return {
      title: {
        text: '部门费用分摊分布',
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
        name: '分摊金额',
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

  // 分摊明细表格列
  const detailColumns = [
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
      title: '部门',
      dataIndex: 'deptCode',
      key: 'deptCode',
      width: 100
    },
    {
      title: '费用类型',
      dataIndex: 'costTypeName',
      key: 'costTypeName',
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
      sorter: (a: EmployeeAllocation, b: EmployeeAllocation) => a.allocatedAmount - b.allocatedAmount
    },
    {
      title: '分摊因子',
      dataIndex: 'allocationFactor',
      key: 'allocationFactor',
      width: 120,
      render: (factor: string) => {
        const f = factorTypes.find(f => f.code === factor);
        return f ? f.name : factor;
      }
    },
    {
      title: '因子值',
      dataIndex: 'factorValue',
      key: 'factorValue',
      width: 100
    },
    {
      title: '因子占比',
      dataIndex: 'factorRatio',
      key: 'factorRatio',
      width: 100,
      render: (val: number) => val ? (val * 100).toFixed(2) + '%' : '-'
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
      title: '机构',
      dataIndex: 'orgCode',
      key: 'orgCode',
      width: 100
    },
    {
      title: '部门',
      dataIndex: 'deptCode',
      key: 'deptCode',
      width: 100
    },
    {
      title: '分摊总额',
      dataIndex: 'totalAllocated',
      key: 'totalAllocated',
      width: 120,
      render: (val: number) => val?.toLocaleString(),
      sorter: (a: SummaryItem, b: SummaryItem) => a.totalAllocated - b.totalAllocated
    }
  ];

  return (
    <div>
      <Card title="员工费用分摊">
        {/* 操作栏 */}
        <Space style={{ marginBottom: 16 }}>
          <DatePicker
            picker="month"
            format="YYYY-MM"
            value={period}
            onChange={(date) => date && setPeriod(date)}
          />
          <Select
            placeholder="成本类型"
            style={{ width: 150 }}
            value={costType || undefined}
            onChange={(value) => setCostType(value || '')}
            allowClear
          >
            {costTypes.map(type => (
              <Option key={type.code} value={type.code}>{type.name}</Option>
            ))}
          </Select>
          <Select
            placeholder="分摊因子"
            style={{ width: 150 }}
            value={factorType}
            onChange={setFactorType}
          >
            {factorTypes.map(factor => (
              <Option key={factor.code} value={factor.code}>{factor.name}</Option>
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
              <Statistic title="分摊总额" value={totalAllocated} precision={2} suffix="元" />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="涉及员工" value={uniqueEmployees} suffix="人" />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="涉及部门" value={uniqueDepts} suffix="个" />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="分摊记录" value={allocations.length} suffix="条" />
            </Card>
          </Col>
        </Row>

        {/* 图表和表格 */}
        <Tabs defaultActiveKey="chart">
          <TabPane tab={<span><PieChartOutlined /> 图表分析</span>} key="chart">
            <Row gutter={16}>
              <Col span={12}>
                <Card>
                  {employeeSummary.length > 0 ? (
                    <ReactECharts option={getEmployeeBarOption()} style={{ height: 400 }} />
                  ) : (
                    <Empty description="暂无数据" />
                  )}
                </Card>
              </Col>
              <Col span={12}>
                <Card>
                  {deptSummary.length > 0 ? (
                    <ReactECharts option={getDeptPieOption()} style={{ height: 400 }} />
                  ) : (
                    <Empty description="暂无数据" />
                  )}
                </Card>
              </Col>
            </Row>
          </TabPane>

          <TabPane tab={<span><BarChartOutlined /> 员工汇总</span>} key="employee">
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
              dataSource={allocations}
              rowKey="id"
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
                      <strong>{totalAllocated.toLocaleString()}</strong>
                    </Table.Summary.Cell>
                    <Table.Summary.Cell index={6} colSpan={3} />
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

export default EmployeeAllocationPage;
