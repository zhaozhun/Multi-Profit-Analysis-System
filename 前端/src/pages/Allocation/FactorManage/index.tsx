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
  InputNumber,
  Switch,
  message,
  Tag,
  Popconfirm,
  Tooltip,
  Row,
  Col,
  Descriptions,
  Tabs,
  TreeSelect
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  CalculatorOutlined,
  SettingOutlined
} from '@ant-design/icons';
import { allocationApi } from '@/services/api';

const { Option } = Select;
const { TextArea } = Input;
const { TabPane } = Tabs;

interface AllocationFactor {
  id: number;
  factorCode: string;
  factorName: string;
  factorType: string;
  dataSource: string;
  calcFormula: string;
  description: string;
  applicableCostTypes: string;
  unit: string;
  precisionVal: number;
  status: string;
}

interface AllocationAlgorithm {
  algorithmCode: string;
  algorithmName: string;
  algorithmType: string;
  description: string;
  paramDefinition: string;
  isBuiltin: boolean;
  status: string;
}

const FactorManagePage: React.FC = () => {
  const [factors, setFactors] = useState<AllocationFactor[]>([]);
  const [algorithms, setAlgorithms] = useState<AllocationAlgorithm[]>([]);
  const [loading, setLoading] = useState(false);
  const [factorModalVisible, setFactorModalVisible] = useState(false);
  const [algorithmModalVisible, setAlgorithmModalVisible] = useState(false);
  const [editingFactor, setEditingFactor] = useState<AllocationFactor | null>(null);
  const [editingAlgorithm, setEditingAlgorithm] = useState<AllocationAlgorithm | null>(null);
  const [selectedFactor, setSelectedFactor] = useState<AllocationFactor | null>(null);
  const [selectedAlgorithm, setSelectedAlgorithm] = useState<AllocationAlgorithm | null>(null);
  const [factorForm] = Form.useForm();
  const [algorithmForm] = Form.useForm();

  // 因子类型选项
  const factorTypes = [
    { code: 'VOLUME', name: '业务量' },
    { code: 'REVENUE', name: '收入' },
    { code: 'HEADCOUNT', name: '人数' },
    { code: 'AREA', name: '面积' },
    { code: 'ASSET', name: '资产' },
    { code: 'SALARY', name: '薪资' },
    { code: 'WORK_HOURS', name: '工时' },
    { code: 'CUSTOM', name: '自定义' }
  ];

  // 算法类型选项
  const algorithmTypes = [
    { code: 'RATIO', name: '比例分摊' },
    { code: 'WEIGHTED', name: '加权分摊' },
    { code: 'STEP', name: '阶梯分摊' },
    { code: 'DIRECT', name: '直接归属' },
    { code: 'FORMULA', name: '公式分摊' }
  ];

  // 成本类型选项
  const costTypes = [
    { code: 'RENT', name: '房租物业' },
    { code: 'UTILITIES', name: '水电费' },
    { code: 'SALARY', name: '人力成本' },
    { code: 'IT', name: '信息技术' },
    { code: 'MARKETING', name: '营销费用' },
    { code: 'ADMIN', name: '行政费用' },
    { code: 'WELFARE', name: '福利费用' },
    { code: 'TRAINING', name: '培训费用' }
  ];

  // 加载数据
  const loadData = async () => {
    setLoading(true);
    try {
      const [factorsRes, algorithmsRes] = await Promise.all([
        allocationApi.getFactors(),
        allocationApi.getAlgorithms()
      ]);

      // API拦截器已返回data.data，直接使用
      setFactors(Array.isArray(factorsRes) ? factorsRes : []);
      setAlgorithms(Array.isArray(algorithmsRes) ? algorithmsRes : []);
    } catch (error) {
      message.error('加载数据失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // ========== 因子管理 ==========

  // 打开因子编辑弹窗
  const openFactorModal = (factor?: AllocationFactor) => {
    setEditingFactor(factor || null);
    if (factor) {
      const applicableCostTypes = factor.applicableCostTypes
        ? JSON.parse(factor.applicableCostTypes)
        : [];
      factorForm.setFieldsValue({
        ...factor,
        applicableCostTypes
      });
    } else {
      factorForm.resetFields();
    }
    setFactorModalVisible(true);
  };

  // 保存因子
  const handleSaveFactor = async () => {
    try {
      const values = await factorForm.validateFields();
      const factorData = {
        ...values,
        applicableCostTypes: JSON.stringify(values.applicableCostTypes || [])
      };

      if (editingFactor) {
        await allocationApi.updateFactor(editingFactor.factorCode, factorData);
        message.success('因子更新成功');
      } else {
        await allocationApi.createFactor(factorData);
        message.success('因子创建成功');
      }

      setFactorModalVisible(false);
      loadData();
    } catch (error) {
      message.error('保存失败');
    }
  };

  // 删除因子
  const handleDeleteFactor = async (factorCode: string) => {
    try {
      await allocationApi.deleteFactor(factorCode);
      message.success('因子删除成功');
      loadData();
    } catch (error) {
      message.error('删除失败');
    }
  };

  // ========== 算法管理 ==========

  // 打开算法编辑弹窗
  const openAlgorithmModal = (algorithm?: AllocationAlgorithm) => {
    setEditingAlgorithm(algorithm || null);
    if (algorithm) {
      algorithmForm.setFieldsValue(algorithm);
    } else {
      algorithmForm.resetFields();
    }
    setAlgorithmModalVisible(true);
  };

  // 保存算法
  const handleSaveAlgorithm = async () => {
    try {
      const values = await algorithmForm.validateFields();

      if (editingAlgorithm) {
        await allocationApi.updateAlgorithm(editingAlgorithm.algorithmCode, values);
        message.success('算法更新成功');
      } else {
        await allocationApi.createAlgorithm(values);
        message.success('算法创建成功');
      }

      setAlgorithmModalVisible(false);
      loadData();
    } catch (error) {
      message.error('保存失败');
    }
  };

  // 删除算法
  const handleDeleteAlgorithm = async (algorithmCode: string) => {
    try {
      await allocationApi.deleteAlgorithm(algorithmCode);
      message.success('算法删除成功');
      loadData();
    } catch (error) {
      message.error('删除失败');
    }
  };

  // 因子表格列定义
  const factorColumns = [
    {
      title: '因子编码',
      dataIndex: 'factorCode',
      key: 'factorCode',
      width: 120
    },
    {
      title: '因子名称',
      dataIndex: 'factorName',
      key: 'factorName',
      width: 150
    },
    {
      title: '因子类型',
      dataIndex: 'factorType',
      key: 'factorType',
      width: 100,
      render: (type: string) => {
        const t = factorTypes.find(f => f.code === type);
        return <Tag>{t ? t.name : type}</Tag>;
      }
    },
    {
      title: '数据来源',
      dataIndex: 'dataSource',
      key: 'dataSource',
      width: 150,
      ellipsis: true
    },
    {
      title: '计算公式',
      dataIndex: 'calcFormula',
      key: 'calcFormula',
      width: 200,
      ellipsis: true
    },
    {
      title: '适用成本类型',
      dataIndex: 'applicableCostTypes',
      key: 'applicableCostTypes',
      width: 200,
      render: (val: string) => {
        if (!val) return '-';
        try {
          const types = JSON.parse(val);
          return (
            <Space size={[0, 4]} wrap>
              {types.map((type: string) => {
                const t = costTypes.find(c => c.code === type);
                return <Tag key={type}>{t ? t.name : type}</Tag>;
              })}
            </Space>
          );
        } catch {
          return val;
        }
      }
    },
    {
      title: '单位',
      dataIndex: 'unit',
      key: 'unit',
      width: 80
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status: string) => (
        <Tag color={status === 'ACTIVE' ? 'green' : 'red'}>
          {status === 'ACTIVE' ? '启用' : '禁用'}
        </Tag>
      )
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_: any, record: any) => (
        <Space>
          <Tooltip title="编辑">
            <Button
              type="link"
              icon={<EditOutlined />}
              onClick={() => openFactorModal(record)}
            />
          </Tooltip>
          <Popconfirm
            title="确定要删除这个因子吗？"
            onConfirm={() => handleDeleteFactor(record.factorCode)}
          >
            <Tooltip title="删除">
              <Button type="link" danger icon={<DeleteOutlined />} />
            </Tooltip>
          </Popconfirm>
        </Space>
      )
    }
  ];

  // 算法表格列定义
  const algorithmColumns = [
    {
      title: '算法编码',
      dataIndex: 'algorithmCode',
      key: 'algorithmCode',
      width: 120
    },
    {
      title: '算法名称',
      dataIndex: 'algorithmName',
      key: 'algorithmName',
      width: 150
    },
    {
      title: '算法类型',
      dataIndex: 'algorithmType',
      key: 'algorithmType',
      width: 120,
      render: (type: string) => {
        const t = algorithmTypes.find(a => a.code === type);
        return <Tag>{t ? t.name : type}</Tag>;
      }
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      width: 300,
      ellipsis: true
    },
    {
      title: '是否内置',
      dataIndex: 'isBuiltin',
      key: 'isBuiltin',
      width: 100,
      render: (val: boolean) => val ? <Tag color="blue">内置</Tag> : <Tag>自定义</Tag>
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status: string) => (
        <Tag color={status === 'ACTIVE' ? 'green' : 'red'}>
          {status === 'ACTIVE' ? '启用' : '禁用'}
        </Tag>
      )
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_: any, record: any) => (
        <Space>
          <Tooltip title="编辑">
            <Button
              type="link"
              icon={<EditOutlined />}
              onClick={() => openAlgorithmModal(record)}
              disabled={record.isBuiltin}
            />
          </Tooltip>
          <Popconfirm
            title="确定要删除这个算法吗？"
            onConfirm={() => handleDeleteAlgorithm(record.algorithmCode)}
          >
            <Tooltip title="删除">
              <Button
                type="link"
                danger
                icon={<DeleteOutlined />}
                disabled={record.isBuiltin}
              />
            </Tooltip>
          </Popconfirm>
        </Space>
      )
    }
  ];

  return (
    <div>
      <Card title="分摊因子与算法管理">
        <Tabs defaultActiveKey="factor">
          <TabPane tab={<span><SettingOutlined /> 分摊因子</span>} key="factor">
            <div style={{ marginBottom: 16 }}>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => openFactorModal()}
              >
                新增因子
              </Button>
            </div>

            <Table
              columns={factorColumns}
              dataSource={factors}
              rowKey="factorCode"
              loading={loading}
              pagination={{ pageSize: 20 }}
              scroll={{ x: 1500 }}
            />
          </TabPane>

          <TabPane tab={<span><CalculatorOutlined /> 分摊算法</span>} key="algorithm">
            <div style={{ marginBottom: 16 }}>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => openAlgorithmModal()}
              >
                新增算法
              </Button>
            </div>

            <Table
              columns={algorithmColumns}
              dataSource={algorithms}
              rowKey="algorithmCode"
              loading={loading}
              pagination={{ pageSize: 20 }}
              scroll={{ x: 1200 }}
            />
          </TabPane>
        </Tabs>
      </Card>

      {/* 因子编辑弹窗 */}
      <Modal
        title={editingFactor ? '编辑分摊因子' : '新增分摊因子'}
        open={factorModalVisible}
        onOk={handleSaveFactor}
        onCancel={() => setFactorModalVisible(false)}
        width={600}
      >
        <Form form={factorForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="factorCode"
                label="因子编码"
                rules={[{ required: true, message: '请输入因子编码' }]}
              >
                <Input disabled={!!editingFactor} placeholder="如: VOLUME, REVENUE" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="factorName"
                label="因子名称"
                rules={[{ required: true, message: '请输入因子名称' }]}
              >
                <Input placeholder="如: 业务量, 收入" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="factorType"
                label="因子类型"
                rules={[{ required: true, message: '请选择因子类型' }]}
              >
                <Select placeholder="请选择因子类型">
                  {factorTypes.map(type => (
                    <Option key={type.code} value={type.code}>{type.name}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="unit"
                label="单位"
              >
                <Input placeholder="如: 元, 笔, 人, 平方米" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="dataSource"
            label="数据来源"
            rules={[{ required: true, message: '请输入数据来源' }]}
          >
            <Input placeholder="如: biz_ledger.revenue, employee_master.count" />
          </Form.Item>

          <Form.Item
            name="calcFormula"
            label="计算公式"
            rules={[{ required: true, message: '请输入计算公式' }]}
          >
            <Input placeholder="如: SUM(revenue) / TOTAL(revenue)" />
          </Form.Item>

          <Form.Item
            name="applicableCostTypes"
            label="适用成本类型"
          >
            <Select
              mode="multiple"
              placeholder="请选择适用的成本类型"
              allowClear
            >
              {costTypes.map(type => (
                <Option key={type.code} value={type.code}>{type.name}</Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="description"
            label="描述"
          >
            <TextArea rows={2} placeholder="请输入因子描述" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 算法编辑弹窗 */}
      <Modal
        title={editingAlgorithm ? '编辑分摊算法' : '新增分摊算法'}
        open={algorithmModalVisible}
        onOk={handleSaveAlgorithm}
        onCancel={() => setAlgorithmModalVisible(false)}
        width={600}
      >
        <Form form={algorithmForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="algorithmCode"
                label="算法编码"
                rules={[{ required: true, message: '请输入算法编码' }]}
              >
                <Input disabled={!!editingAlgorithm} placeholder="如: RATIO, WEIGHTED" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="algorithmName"
                label="算法名称"
                rules={[{ required: true, message: '请输入算法名称' }]}
              >
                <Input placeholder="如: 比例分摊, 加权分摊" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="algorithmType"
            label="算法类型"
            rules={[{ required: true, message: '请选择算法类型' }]}
          >
            <Select placeholder="请选择算法类型">
              {algorithmTypes.map(type => (
                <Option key={type.code} value={type.code}>{type.name}</Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="description"
            label="算法描述"
            rules={[{ required: true, message: '请输入算法描述' }]}
          >
            <TextArea rows={3} placeholder="请输入算法描述和计算公式" />
          </Form.Item>

          <Form.Item
            name="paramDefinition"
            label="参数定义 (JSON)"
          >
            <TextArea
              rows={3}
              placeholder='[{"name":"factor_code","type":"string","required":true,"description":"分摊因子编码"}]'
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default FactorManagePage;
