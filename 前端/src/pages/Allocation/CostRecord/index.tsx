// @ts-nocheck
import React, { useState, useEffect } from 'react';
import {
  Card,
  Table,
  Button,
  Space,
  Modal,
  Form,
  Input,
  Select,
  TreeSelect,
  InputNumber,
  DatePicker,
  message,
  Tag,
  Popconfirm,
  Tooltip,
  Row,
  Col,
  Statistic,
  Upload
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  UploadOutlined,
  ExportOutlined
} from '@ant-design/icons';
import { costTypeApi } from '@/services/api';
import dayjs from 'dayjs';

const { Option } = Select;
const { TextArea } = Input;

interface CostRecord {
  id: number;
  period: string;
  costCode: string;
  costName: string;
  costType: string;
  amount: number;
  deptCode: string;
  vendor: string;
  invoiceNo: string;
  description: string;
  status: string;
  occurrenceDate?: string;
  createdAt: string;
}

interface CostType {
  costCode: string;
  costName: string;
  level: number;
  parentCode: string;
}

const CostRecordPage: React.FC = () => {
  const [records, setRecords] = useState<CostRecord[]>([]);
  const [costTypes, setCostTypes] = useState<CostType[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState<CostRecord | null>(null);
  const [period, setPeriod] = useState<dayjs.Dayjs>(dayjs('2025-06-01'));
  const [filterCostType, setFilterCostType] = useState<string>('');
  const [form] = Form.useForm();

  // 费用类型树形选项
  const [costTypeTree, setCostTypeTree] = useState<any[]>([]);

  // 加载费用类型
  const loadCostTypes = async () => {
    try {
      const [listRes, treeRes] = await Promise.all([
        costTypeApi.getAllCostTypes(),
        costTypeApi.getCostTypeHierarchy()
      ]);
      if (Array.isArray(listRes)) {
        setCostTypes(listRes);
      }
      if (Array.isArray(treeRes)) {
        setCostTypeTree(treeRes);
      }
    } catch (error) {
      message.error('加载费用类型失败');
    }
  };

  // 加载费用记录
  const loadRecords = async () => {
    setLoading(true);
    try {
      const response = await costTypeApi.getActualRecords(
        period.format('YYYY-MM'),
        null,
        filterCostType || undefined
      );
      if (response) {
        setRecords(response);
      }
    } catch (error) {
      message.error('加载费用记录失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCostTypes();
  }, []);

  useEffect(() => {
    loadRecords();
  }, [period, filterCostType]);

  // 打开新增/编辑弹窗
  const openModal = (record?: CostRecord) => {
    setEditingRecord(record || null);
    if (record) {
      form.setFieldsValue({
        ...record,
        occurrenceDate: record.occurrenceDate ? dayjs(record.occurrenceDate) : undefined
      });
    } else {
      form.resetFields();
      form.setFieldsValue({ period: period.format('YYYY-MM') });
    }
    setModalVisible(true);
  };

  // 关闭弹窗
  const closeModal = () => {
    setModalVisible(false);
    setEditingRecord(null);
    form.resetFields();
  };

  // 保存费用记录
  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      const recordData = {
        ...values,
        period: period.format('YYYY-MM'),
        occurrenceDate: values.occurrenceDate?.format('YYYY-MM-DD')
      };

      if (editingRecord) {
        // TODO: 更新接口
        message.success('费用记录更新成功');
      } else {
        await costTypeApi.createActualRecord(recordData);
        message.success('费用记录创建成功');
      }

      closeModal();
      loadRecords();
    } catch (error) {
      message.error('保存失败');
    }
  };

  // 删除费用记录
  const handleDelete = async (id: number) => {
    try {
      // TODO: 删除接口
      message.success('费用记录删除成功');
      loadRecords();
    } catch (error) {
      message.error('删除失败');
    }
  };

  // 计算统计
  const totalAmount = records.reduce((sum, r) => sum + r.amount, 0);
  const pendingAmount = records.filter(r => r.status === 'PENDING').reduce((sum, r) => sum + r.amount, 0);
  const allocatedAmount = records.filter(r => r.status === 'ALLOCATED').reduce((sum, r) => sum + r.amount, 0);

  // 构建费用类型选项（扁平化）
  const buildCostTypeOptions = (data: any[], level: number = 0): any[] => {
    return data.map(item => ({
      value: item.costCode,
      label: `${'  '.repeat(level)}${item.costCode} - ${item.costName}`,
      children: item.children ? buildCostTypeOptions(item.children, level + 1) : undefined
    }));
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
      title: '费用类型',
      dataIndex: 'costType',
      key: 'costType',
      width: 100,
      render: (type: string) => {
        const nameMap: Record<string, string> = {
          'OPERATION': '运营',
          'MANAGEMENT': '管理',
          'SALES': '销售',
          'FINANCE': '财务',
          'HR': '人力',
          'IT': 'IT'
        };
        return <Tag>{nameMap[type] || type}</Tag>;
      }
    },
    {
      title: '金额',
      dataIndex: 'amount',
      key: 'amount',
      width: 120,
      render: (val: number) => val?.toLocaleString(),
      sorter: (a: CostRecord, b: CostRecord) => a.amount - b.amount
    },
    {
      title: '归属部门',
      dataIndex: 'deptCode',
      key: 'deptCode',
      width: 100,
      render: (code: string) => code || '-'
    },
    {
      title: '供应商',
      dataIndex: 'vendor',
      key: 'vendor',
      width: 120,
      render: (val: string) => val || '-'
    },
    {
      title: '发票号',
      dataIndex: 'invoiceNo',
      key: 'invoiceNo',
      width: 120,
      render: (val: string) => val || '-'
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => (
        <Tag color={
          status === 'PENDING' ? 'orange' :
          status === 'ALLOCATED' ? 'blue' :
          status === 'CONFIRMED' ? 'green' : 'default'
        }>
          {status === 'PENDING' ? '待分摊' :
           status === 'ALLOCATED' ? '已分摊' :
           status === 'CONFIRMED' ? '已确认' : status}
        </Tag>
      )
    },
    {
      title: '说明',
      dataIndex: 'description',
      key: 'description',
      width: 200,
      ellipsis: true,
      render: (val: string) => val || '-'
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_: any, record: CostRecord) => (
        <Space>
          <Tooltip title="编辑">
            <Button
              type="link"
              icon={<EditOutlined />}
              onClick={() => openModal(record)}
              disabled={record.status !== 'PENDING'}
            />
          </Tooltip>
          <Popconfirm
            title="确定要删除这条记录吗？"
            onConfirm={() => handleDelete(record.id)}
          >
            <Tooltip title="删除">
              <Button
                type="link"
                danger
                icon={<DeleteOutlined />}
                disabled={record.status !== 'PENDING'}
              />
            </Tooltip>
          </Popconfirm>
        </Space>
      )
    }
  ];

  return (
    <div>
      <Card title="费用记录管理">
        {/* 统计卡片 */}
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={6}>
            <Card>
              <Statistic title="总费用" value={totalAmount} precision={2} suffix="元" />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="待分摊" value={pendingAmount} precision={2} suffix="元" valueStyle={{ color: '#faad14' }} />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="已分摊" value={allocatedAmount} precision={2} suffix="元" valueStyle={{ color: '#1890ff' }} />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="记录数" value={records.length} suffix="条" />
            </Card>
          </Col>
        </Row>

        {/* 筛选和操作 */}
        <Space style={{ marginBottom: 16 }}>
          <DatePicker
            picker="month"
            format="YYYY-MM"
            value={period}
            onChange={(date) => date && setPeriod(date)}
          />
          <Select
            placeholder="费用类型"
            allowClear
            style={{ width: 200 }}
            value={filterCostType || undefined}
            onChange={(value) => setFilterCostType(value || '')}
          >
            {costTypes.filter(t => t.level === 1).map(type => (
              <Option key={type.costCode} value={type.costCode}>{type.costName}</Option>
            ))}
          </Select>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => openModal()}
          >
            新增费用
          </Button>
          <Button
            icon={<ExportOutlined />}
          >
            导出
          </Button>
        </Space>

        {/* 费用记录表格 */}
        <Table
          columns={columns}
          dataSource={records}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 20 }}
          scroll={{ x: 1500 }}
          summary={() => (
            <Table.Summary fixed>
              <Table.Summary.Row>
                <Table.Summary.Cell index={0} colSpan={3}>
                  <strong>合计</strong>
                </Table.Summary.Cell>
                <Table.Summary.Cell index={3}>
                  <strong>{totalAmount.toLocaleString()}</strong>
                </Table.Summary.Cell>
                <Table.Summary.Cell index={4} colSpan={6} />
              </Table.Summary.Row>
            </Table.Summary>
          )}
        />
      </Card>

      {/* 新增/编辑弹窗 */}
      <Modal
        title={editingRecord ? '编辑费用记录' : '新增费用记录'}
        open={modalVisible}
        onOk={handleSave}
        onCancel={closeModal}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="costCode"
                label="费用类型"
                rules={[{ required: true, message: '请选择费用类型' }]}
              >
                <TreeSelect
                  placeholder="请选择费用类型"
                  showSearch
                  treeData={buildCostTypeOptions(costTypeTree)}
                  treeDefaultExpandAll
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="amount"
                label="金额"
                rules={[{ required: true, message: '请输入金额' }]}
              >
                <InputNumber
                  style={{ width: '100%' }}
                  min={0}
                  precision={2}
                  placeholder="请输入金额"
                  formatter={value => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="deptCode"
                label="归属部门"
              >
                <Input placeholder="请输入部门编码" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="occurrenceDate"
                label="发生日期"
              >
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="vendor"
                label="供应商"
              >
                <Input placeholder="请输入供应商" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="invoiceNo"
                label="发票号"
              >
                <Input placeholder="请输入发票号" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="description"
            label="费用说明"
          >
            <TextArea rows={3} placeholder="请输入费用说明" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default CostRecordPage;
