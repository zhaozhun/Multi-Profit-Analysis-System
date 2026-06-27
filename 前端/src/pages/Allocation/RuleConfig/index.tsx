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
  Divider,
  Row,
  Col
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  CheckCircleOutlined,
  StopOutlined,
  CopyOutlined
} from '@ant-design/icons';
import { allocationApi } from '@/services/api';

const { Option } = Select;
const { TextArea } = Input;

interface RuleConfig {
  id: number;
  ruleCode: string;
  ruleName: string;
  costType: string;
  description: string;
  priority: number;
  sourceDimType: string;
  sourceDimCode: string;
  targetDimType: string;
  targetDimFilter: string;
  algorithmCode: string;
  algorithmParams: string;
  periodType: string;
  autoExecute: boolean;
  effectiveDate: string;
  expireDate: string;
  status: string;
  version: number;
}

interface FactorWeight {
  factorCode: string;
  weight: number;
}

const RuleConfigPage: React.FC = () => {
  const [rules, setRules] = useState<RuleConfig[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRule, setEditingRule] = useState<RuleConfig | null>(null);
  const [form] = Form.useForm();
  const [weights, setWeights] = useState<FactorWeight[]>([]);

  // 成本类型选项
  const costTypes = [
    { code: 'RENT', name: '房租物业' },
    { code: 'SALARY', name: '人力成本' },
    { code: 'IT', name: '信息技术' },
    { code: 'MARKETING', name: '营销费用' },
    { code: 'ADMIN', name: '行政费用' },
    { code: 'RISK', name: '风险准备' }
  ];

  // 维度类型选项
  const dimTypes = [
    { code: 'ORG', name: '机构' },
    { code: 'DEPT', name: '部门' },
    { code: 'PRODUCT', name: '产品' },
    { code: 'CHANNEL', name: '渠道' },
    { code: 'MANAGER', name: '客户经理' }
  ];

  // 算法选项
  const algorithms = [
    { code: 'RATIO', name: '比例分摊' },
    { code: 'WEIGHTED', name: '加权分摊' },
    { code: 'STEP', name: '阶梯分摊' },
    { code: 'DIRECT', name: '直接归属' },
    { code: 'FORMULA', name: '公式分摊' }
  ];

  // 因子选项
  const factors = [
    { code: 'VOLUME', name: '业务量' },
    { code: 'REVENUE', name: '收入' },
    { code: 'HEADCOUNT', name: '人员数量' },
    { code: 'AREA', name: '办公面积' },
    { code: 'ASSET', name: '资产规模' }
  ];

  // 加载规则列表
  const loadRules = async () => {
    setLoading(true);
    try {
      const response = await allocationApi.getRules();
      if (response) {
        setRules(response.data);
      }
    } catch (error) {
      message.error('加载规则列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRules();
  }, []);

  // 打开新增/编辑弹窗
  const openModal = (rule?: RuleConfig) => {
    setEditingRule(rule || null);
    setWeights([]);
    if (rule) {
      form.setFieldsValue(rule);
      // 解析算法参数中的权重
      try {
        const params = JSON.parse(rule.algorithmParams);
        if (params.factors) {
          setWeights(params.factors);
        }
      } catch (e) {
        // 忽略解析错误
      }
    } else {
      form.resetFields();
    }
    setModalVisible(true);
  };

  // 关闭弹窗
  const closeModal = () => {
    setModalVisible(false);
    setEditingRule(null);
    form.resetFields();
    setWeights([]);
  };

  // 保存规则
  const handleSave = async () => {
    try {
      const values = await form.validateFields();

      // 构建算法参数
      let algorithmParams = '{}';
      if (values.algorithmCode === 'RATIO') {
        algorithmParams = JSON.stringify({ factor_code: values.factorCode });
      } else if (values.algorithmCode === 'WEIGHTED') {
        algorithmParams = JSON.stringify({ factors: weights });
      } else if (values.algorithmCode === 'FORMULA') {
        algorithmParams = JSON.stringify({ expression: values.expression });
      }

      const ruleData = {
        ...values,
        algorithmParams,
        weights: values.algorithmCode === 'WEIGHTED' ? weights : undefined
      };

      if (editingRule) {
        await allocationApi.updateRule(editingRule.id, ruleData);
        message.success('规则更新成功');
      } else {
        await allocationApi.createRule(ruleData);
        message.success('规则创建成功');
      }

      closeModal();
      loadRules();
    } catch (error) {
      message.error('保存失败');
    }
  };

  // 删除规则
  const handleDelete = async (id: number) => {
    try {
      await allocationApi.deleteRule(id);
      message.success('规则删除成功');
      loadRules();
    } catch (error) {
      message.error('删除失败');
    }
  };

  // 启用/禁用规则
  const handleToggleStatus = async (id: number, status: string) => {
    try {
      if (status === 'ACTIVE') {
        await allocationApi.disableRule(id);
        message.success('规则已禁用');
      } else {
        await allocationApi.enableRule(id);
        message.success('规则已启用');
      }
      loadRules();
    } catch (error) {
      message.error('操作失败');
    }
  };

  // 添加权重
  const addWeight = () => {
    setWeights([...weights, { factorCode: '', weight: 0 }]);
  };

  // 删除权重
  const removeWeight = (index: number) => {
    const newWeights = [...weights];
    newWeights.splice(index, 1);
    setWeights(newWeights);
  };

  // 更新权重
  const updateWeight = (index: number, field: string, value: any) => {
    const newWeights = [...weights];
    newWeights[index] = { ...newWeights[index], [field]: value };
    setWeights(newWeights);
  };

  // 表格列定义
  const columns = [
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
      title: '来源维度',
      dataIndex: 'sourceDimType',
      key: 'sourceDimType',
      width: 100
    },
    {
      title: '目标维度',
      dataIndex: 'targetDimType',
      key: 'targetDimType',
      width: 100
    },
    {
      title: '算法',
      dataIndex: 'algorithmCode',
      key: 'algorithmCode',
      width: 100,
      render: (code: string) => {
        const algo = algorithms.find(a => a.code === code);
        return algo ? algo.name : code;
      }
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 80
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status: string) => (
        <Tag color={status === 'ACTIVE' ? 'green' : status === 'DRAFT' ? 'orange' : 'red'}>
          {status === 'ACTIVE' ? '生效' : status === 'DRAFT' ? '草稿' : '失效'}
        </Tag>
      )
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_: any, record: any) => (
        <Space>
          <Tooltip title="编辑">
            <Button
              type="link"
              icon={<EditOutlined />}
              onClick={() => openModal(record)}
            />
          </Tooltip>
          <Tooltip title={record.status === 'ACTIVE' ? '禁用' : '启用'}>
            <Button
              type="link"
              icon={record.status === 'ACTIVE' ? <StopOutlined /> : <CheckCircleOutlined />}
              onClick={() => handleToggleStatus(record.id, record.status)}
            />
          </Tooltip>
          <Popconfirm
            title="确定要删除这条规则吗？"
            onConfirm={() => handleDelete(record.id)}
          >
            <Tooltip title="删除">
              <Button type="link" danger icon={<DeleteOutlined />} />
            </Tooltip>
          </Popconfirm>
        </Space>
      )
    }
  ];

  return (
    <div>
      <Card
        title="分摊规则配置"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>
            新增规则
          </Button>
        }
      >
        <Table
          columns={columns}
          dataSource={rules}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      <Modal
        title={editingRule ? '编辑规则' : '新增规则'}
        open={modalVisible}
        onOk={handleSave}
        onCancel={closeModal}
        width={800}
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="ruleCode"
                label="规则编码"
                rules={[{ required: true, message: '请输入规则编码' }]}
              >
                <Input disabled={!!editingRule} placeholder="请输入规则编码" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="ruleName"
                label="规则名称"
                rules={[{ required: true, message: '请输入规则名称' }]}
              >
                <Input placeholder="请输入规则名称" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="costType"
                label="成本类型"
                rules={[{ required: true, message: '请选择成本类型' }]}
              >
                <Select placeholder="请选择成本类型">
                  {costTypes.map(type => (
                    <Option key={type.code} value={type.code}>{type.name}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="priority"
                label="优先级"
                initialValue={100}
              >
                <InputNumber min={1} max={999} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="sourceDimType"
                label="来源维度"
                rules={[{ required: true, message: '请选择来源维度' }]}
              >
                <Select placeholder="请选择来源维度">
                  {dimTypes.map(dim => (
                    <Option key={dim.code} value={dim.code}>{dim.name}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="targetDimType"
                label="目标维度"
                rules={[{ required: true, message: '请选择目标维度' }]}
              >
                <Select placeholder="请选择目标维度">
                  {dimTypes.map(dim => (
                    <Option key={dim.code} value={dim.code}>{dim.name}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="algorithmCode"
            label="分摊算法"
            rules={[{ required: true, message: '请选择分摊算法' }]}
          >
            <Select placeholder="请选择分摊算法">
              {algorithms.map(algo => (
                <Option key={algo.code} value={algo.code}>{algo.name}</Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            noStyle
            shouldUpdate={(prevValues, currentValues) =>
              prevValues.algorithmCode !== currentValues.algorithmCode
            }
          >
            {({ getFieldValue }) => {
              const algorithmCode = getFieldValue('algorithmCode');
              if (algorithmCode === 'RATIO' || algorithmCode === 'STEP') {
                return (
                  <Form.Item
                    name="factorCode"
                    label="分摊因子"
                    rules={[{ required: true, message: '请选择分摊因子' }]}
                  >
                    <Select placeholder="请选择分摊因子">
                      {factors.map(factor => (
                        <Option key={factor.code} value={factor.code}>{factor.name}</Option>
                      ))}
                    </Select>
                  </Form.Item>
                );
              }
              if (algorithmCode === 'WEIGHTED') {
                return (
                  <Form.Item label="因子权重配置">
                    {weights.map((w, index) => (
                      <Space key={index} style={{ marginBottom: 8 }}>
                        <Select
                          placeholder="选择因子"
                          value={w.factorCode}
                          onChange={(v) => updateWeight(index, 'factorCode', v)}
                          style={{ width: 150 }}
                        >
                          {factors.map(factor => (
                            <Option key={factor.code} value={factor.code}>{factor.name}</Option>
                          ))}
                        </Select>
                        <InputNumber
                          placeholder="权重"
                          min={0}
                          max={1}
                          step={0.1}
                          value={w.weight}
                          onChange={(v) => updateWeight(index, 'weight', v)}
                          style={{ width: 100 }}
                        />
                        <Button
                          type="link"
                          danger
                          onClick={() => removeWeight(index)}
                        >
                          删除
                        </Button>
                      </Space>
                    ))}
                    <Button type="dashed" onClick={addWeight} block>
                      添加因子
                    </Button>
                  </Form.Item>
                );
              }
              if (algorithmCode === 'FORMULA') {
                return (
                  <Form.Item
                    name="expression"
                    label="分摊公式"
                    rules={[{ required: true, message: '请输入分摊公式' }]}
                  >
                    <TextArea rows={3} placeholder="请输入SpEL表达式，如: #total_cost * (#factor_value / #factor_total)" />
                  </Form.Item>
                );
              }
              return null;
            }}
          </Form.Item>

          <Form.Item
            name="description"
            label="规则描述"
          >
            <TextArea rows={2} placeholder="请输入规则描述" />
          </Form.Item>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item
                name="periodType"
                label="分摊周期"
                initialValue="MONTHLY"
              >
                <Select>
                  <Option value="MONTHLY">月度</Option>
                  <Option value="QUARTERLY">季度</Option>
                  <Option value="YEARLY">年度</Option>
                  <Option value="ON_DEMAND">按需</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="autoExecute"
                label="自动执行"
                valuePropName="checked"
                initialValue={true}
              >
                <Switch />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="status"
                label="状态"
                initialValue="DRAFT"
              >
                <Select>
                  <Option value="DRAFT">草稿</Option>
                  <Option value="ACTIVE">生效</Option>
                  <Option value="INACTIVE">失效</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
};

export default RuleConfigPage;
