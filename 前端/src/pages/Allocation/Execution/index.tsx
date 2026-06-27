// @ts-nocheck
import React, { useState, useEffect } from 'react';
import {
  Card,
  Button,
  Space,
  Form,
  Select,
  DatePicker,
  message,
  Table,
  Tag,
  Modal,
  Descriptions,
  Spin,
  Row,
  Col,
  Statistic,
  Alert
} from 'antd';
import {
  PlayCircleOutlined,
  EyeOutlined,
  ReloadOutlined
} from '@ant-design/icons';
import { allocationApi } from '@/services/api';
import dayjs from 'dayjs';

const { Option } = Select;

interface Batch {
  id: number;
  batchNo: string;
  period: string;
  costType: string;
  totalAmount: number;
  allocatedAmount: number;
  recordCount: number;
  status: string;
  startTime: string;
  endTime: string;
  triggerType: string;
  triggeredBy: string;
}

interface PreviewItem {
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
  factorValue: number;
  algorithmCode: string;
}

const ExecutionPage: React.FC = () => {
  const [form] = Form.useForm();
  const [batches, setBatches] = useState<Batch[]>([]);
  const [loading, setLoading] = useState(false);
  const [executing, setExecuting] = useState(false);
  const [previewVisible, setPreviewVisible] = useState(false);
  const [previewData, setPreviewData] = useState<PreviewItem[]>([]);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [detailVisible, setDetailVisible] = useState(false);
  const [selectedBatch, setSelectedBatch] = useState<Batch | null>(null);

  // 成本类型选项
  const costTypes = [
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
      const period = form.getFieldValue('period');
      if (!period) {
        message.warning('请先选择期间');
        return;
      }
      const response = await allocationApi.getResults(period.format('YYYY-MM'));
      if (response) {
        setBatches(response.data);
      }
    } catch (error) {
      message.error('加载批次列表失败');
    } finally {
      setLoading(false);
    }
  };

  // 执行分摊
  const handleExecute = async () => {
    try {
      const values = await form.validateFields();
      setExecuting(true);

      const request = {
        period: values.period.format('YYYY-MM'),
        costType: values.costType || null,
        triggerType: 'MANUAL',
        triggeredBy: 'admin'
      };

      const response = await allocationApi.executeAllocation(request);
      if (response) {
        message.success('分摊执行成功');
        loadBatches();
      } else {
        message.error('分摊执行失败: ' + response.message);
      }
    } catch (error) {
      message.error('分摊执行失败');
    } finally {
      setExecuting(false);
    }
  };

  // 预览分摊
  const handlePreview = async () => {
    try {
      const values = await form.validateFields();
      setPreviewLoading(true);
      setPreviewVisible(true);

      const request = {
        period: values.period.format('YYYY-MM'),
        costType: values.costType || null
      };

      const response = await allocationApi.previewAllocation(request);
      if (response) {
        setPreviewData(response.data.items || []);
      } else {
        message.error('预览失败: ' + response.message);
      }
    } catch (error) {
      message.error('预览失败');
    } finally {
      setPreviewLoading(false);
    }
  };

  // 查看详情
  const handleViewDetail = (batch: Batch) => {
    setSelectedBatch(batch);
    setDetailVisible(true);
  };

  // 预览表格列定义
  const previewColumns = [
    {
      title: '规则编码',
      dataIndex: 'ruleCode',
      key: 'ruleCode',
      width: 120
    },
    {
      title: '规则名称',
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
      title: '目标维度',
      dataIndex: 'targetDimCode',
      key: 'targetDimCode',
      width: 120
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
      render: (val: number) => val?.toLocaleString()
    },
    {
      title: '分摊比例',
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

  // 批次表格列定义
  const batchColumns = [
    {
      title: '批次号',
      dataIndex: 'batchNo',
      key: 'batchNo',
      width: 180
    },
    {
      title: '期间',
      dataIndex: 'period',
      key: 'period',
      width: 100
    },
    {
      title: '成本类型',
      dataIndex: 'costType',
      key: 'costType',
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
      key: 'totalAmount',
      width: 120,
      render: (val: number) => val?.toLocaleString()
    },
    {
      title: '已分摊',
      dataIndex: 'allocatedAmount',
      key: 'allocatedAmount',
      width: 120,
      render: (val: number) => val?.toLocaleString()
    },
    {
      title: '记录数',
      dataIndex: 'recordCount',
      key: 'recordCount',
      width: 80
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status: string) => (
        <Tag color={
          status === 'COMPLETED' ? 'green' :
          status === 'PROCESSING' ? 'blue' :
          status === 'FAILED' ? 'red' : 'default'
        }>
          {status === 'COMPLETED' ? '完成' :
           status === 'PROCESSING' ? '处理中' :
           status === 'FAILED' ? '失败' : status}
        </Tag>
      )
    },
    {
      title: '触发方式',
      dataIndex: 'triggerType',
      key: 'triggerType',
      width: 80,
      render: (type: string) => (
        <Tag color={type === 'AUTO' ? 'blue' : 'default'}>
          {type === 'AUTO' ? '自动' : '手动'}
        </Tag>
      )
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: any, record: any) => (
        <Button
          type="link"
          icon={<EyeOutlined />}
          onClick={() => handleViewDetail(record)}
        >
          详情
        </Button>
      )
    }
  ];

  return (
    <div>
      <Card title="分摊执行">
        <Form form={form} layout="inline" style={{ marginBottom: 16 }}>
          <Form.Item
            name="period"
            label="期间"
            rules={[{ required: true, message: '请选择期间' }]}
          >
            <DatePicker picker="month" format="YYYY-MM" />
          </Form.Item>
          <Form.Item
            name="costType"
            label="成本类型"
          >
            <Select placeholder="全部" allowClear style={{ width: 150 }}>
              {costTypes.map(type => (
                <Option key={type.code} value={type.code}>{type.name}</Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item>
            <Space>
              <Button
                type="primary"
                icon={<PlayCircleOutlined />}
                onClick={handleExecute}
                loading={executing}
              >
                执行分摊
              </Button>
              <Button
                icon={<EyeOutlined />}
                onClick={handlePreview}
              >
                预览结果
              </Button>
              <Button
                icon={<ReloadOutlined />}
                onClick={loadBatches}
              >
                刷新列表
              </Button>
            </Space>
          </Form.Item>
        </Form>

        <Table
          columns={batchColumns}
          dataSource={batches}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      {/* 预览弹窗 */}
      <Modal
        title="分摊预览"
        open={previewVisible}
        onCancel={() => setPreviewVisible(false)}
        footer={null}
        width={1000}
      >
        <Spin spinning={previewLoading}>
          <Table
            columns={previewColumns}
            dataSource={previewData}
            rowKey={(record, index) => index?.toString() || '0'}
            pagination={{ pageSize: 10 }}
          />
        </Spin>
      </Modal>

      {/* 详情弹窗 */}
      <Modal
        title="批次详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={null}
        width={800}
      >
        {selectedBatch && (
          <Descriptions bordered column={2}>
            <Descriptions.Item label="批次号">{selectedBatch.batchNo}</Descriptions.Item>
            <Descriptions.Item label="期间">{selectedBatch.period}</Descriptions.Item>
            <Descriptions.Item label="成本类型">
              {costTypes.find(t => t.code === selectedBatch.costType)?.name || selectedBatch.costType}
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={selectedBatch.status === 'COMPLETED' ? 'green' : 'red'}>
                {selectedBatch.status}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="总金额">{selectedBatch.totalAmount?.toLocaleString()}</Descriptions.Item>
            <Descriptions.Item label="已分摊">{selectedBatch.allocatedAmount?.toLocaleString()}</Descriptions.Item>
            <Descriptions.Item label="记录数">{selectedBatch.recordCount}</Descriptions.Item>
            <Descriptions.Item label="触发方式">{selectedBatch.triggerType}</Descriptions.Item>
            <Descriptions.Item label="开始时间">{selectedBatch.startTime}</Descriptions.Item>
            <Descriptions.Item label="结束时间">{selectedBatch.endTime}</Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  );
};

export default ExecutionPage;
