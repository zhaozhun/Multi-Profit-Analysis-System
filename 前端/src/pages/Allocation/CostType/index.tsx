// @ts-nocheck
import React, { useState, useEffect } from 'react';
import {
  Card,
  Tree,
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
  Tabs
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  FolderOutlined,
  FileOutlined
} from '@ant-design/icons';
import { costTypeApi } from '@/services/api';

const { Option } = Select;
const { TextArea } = Input;
const { TabPane } = Tabs;

interface CostType {
  id: number;
  costCode: string;
  costName: string;
  parentCode: string;
  level: number;
  costCategory: string;
  costNature: string;
  allocationRequired: boolean;
  allocationMethod: string;
  allocationFactor: string;
  description: string;
  status: string;
}

interface TreeNode {
  title: string;
  key: string;
  icon: React.ReactNode;
  children?: TreeNode[];
  data: CostType;
}

const CostTypePage: React.FC = () => {
  const [costTypes, setCostTypes] = useState<CostType[]>([]);
  const [hierarchy, setHierarchy] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingType, setEditingType] = useState<CostType | null>(null);
  const [selectedType, setSelectedType] = useState<CostType | null>(null);
  const [form] = Form.useForm();

  // 费用性质选项
  const costNatures = [
    { code: 'OPERATION', name: '运营费用' },
    { code: 'MANAGEMENT', name: '管理费用' },
    { code: 'SALES', name: '销售费用' },
    { code: 'FINANCE', name: '财务费用' },
    { code: 'HR', name: '人力成本' },
    { code: 'IT', name: '信息技术' }
  ];

  // 费用类别选项
  const costCategories = [
    { code: 'FIXED', name: '固定费用' },
    { code: 'VARIABLE', name: '变动费用' },
    { code: 'DIRECT', name: '直接费用' }
  ];

  // 分摊方法选项
  const allocationMethods = [
    { code: 'EMPLOYEE_COUNT', name: '按人数' },
    { code: 'WORK_HOURS', name: '按工时' },
    { code: 'SALARY', name: '按薪资' },
    { code: 'AREA', name: '按面积' },
    { code: 'BIZ_VOLUME', name: '按业务量' },
    { code: 'DEPT_DIRECT', name: '部门归属' },
    { code: 'ASSET_VALUE', name: '按资产价值' },
    { code: 'REVENUE', name: '按收入' },
    { code: 'CUSTOM', name: '自定义' }
  ];

  // 加载费用类型列表
  const loadCostTypes = async () => {
    setLoading(true);
    try {
      const [listRes, hierarchyRes] = await Promise.all([
        costTypeApi.getAllCostTypes(),
        costTypeApi.getCostTypeHierarchy()
      ]);
      // API拦截器已返回data.data，直接使用
      setCostTypes(Array.isArray(listRes) ? listRes : []);
      setHierarchy(Array.isArray(hierarchyRes) ? hierarchyRes : []);
    } catch (error) {
      message.error('加载费用类型失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCostTypes();
  }, []);

  // 构建树形数据
  const buildTreeData = (data: any[]): TreeNode[] => {
    return data.map(item => ({
      title: `${item.costCode} - ${item.costName}`,
      key: item.costCode,
      icon: item.children ? <FolderOutlined /> : <FileOutlined />,
      children: item.children ? buildTreeData(item.children) : undefined,
      data: item
    }));
  };

  // 选择树节点
  const onTreeSelect = (selectedKeys: React.Key[]) => {
    if (selectedKeys.length > 0) {
      const code = selectedKeys[0] as string;
      const type = costTypes.find(t => t.costCode === code);
      setSelectedType(type || null);
    } else {
      setSelectedType(null);
    }
  };

  // 打开新增/编辑弹窗
  const openModal = (type?: CostType, parentCode?: string) => {
    setEditingType(type || null);
    if (type) {
      form.setFieldsValue(type);
    } else {
      form.resetFields();
      if (parentCode) {
        form.setFieldsValue({ parentCode });
      }
    }
    setModalVisible(true);
  };

  // 关闭弹窗
  const closeModal = () => {
    setModalVisible(false);
    setEditingType(null);
    form.resetFields();
  };

  // 保存费用类型
  const handleSave = async () => {
    try {
      const values = await form.validateFields();

      if (editingType) {
        await costTypeApi.updateCostType(editingType.costCode, values);
        message.success('费用类型更新成功');
      } else {
        await costTypeApi.createCostType(values);
        message.success('费用类型创建成功');
      }

      closeModal();
      loadCostTypes();
    } catch (error) {
      message.error('保存失败');
    }
  };

  // 删除费用类型
  const handleDelete = async (costCode: string) => {
    try {
      await costTypeApi.deleteCostType(costCode);
      message.success('费用类型删除成功');
      loadCostTypes();
      if (selectedType?.costCode === costCode) {
        setSelectedType(null);
      }
    } catch (error) {
      message.error('删除失败');
    }
  };

  // 渲染费用性质标签
  const renderNatureTag = (nature: string) => {
    const colorMap: Record<string, string> = {
      'OPERATION': 'blue',
      'MANAGEMENT': 'green',
      'SALES': 'orange',
      'FINANCE': 'purple',
      'HR': 'cyan',
      'IT': 'geekblue'
    };
    const nameMap: Record<string, string> = {
      'OPERATION': '运营',
      'MANAGEMENT': '管理',
      'SALES': '销售',
      'FINANCE': '财务',
      'HR': '人力',
      'IT': 'IT'
    };
    return <Tag color={colorMap[nature]}>{nameMap[nature] || nature}</Tag>;
  };

  // 渲染费用类别标签
  const renderCategoryTag = (category: string) => {
    const colorMap: Record<string, string> = {
      'FIXED': 'green',
      'VARIABLE': 'orange',
      'DIRECT': 'blue'
    };
    const nameMap: Record<string, string> = {
      'FIXED': '固定',
      'VARIABLE': '变动',
      'DIRECT': '直接'
    };
    return <Tag color={colorMap[category]}>{nameMap[category] || category}</Tag>;
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
      render: renderNatureTag
    },
    {
      title: '费用类别',
      dataIndex: 'costCategory',
      key: 'costCategory',
      width: 100,
      render: renderCategoryTag
    },
    {
      title: '需要分摊',
      dataIndex: 'allocationRequired',
      key: 'allocationRequired',
      width: 100,
      render: (val: boolean) => val ? <Tag color="green">是</Tag> : <Tag color="red">否</Tag>
    },
    {
      title: '分摊方法',
      dataIndex: 'allocationMethod',
      key: 'allocationMethod',
      width: 120,
      render: (method: string) => {
        const m = allocationMethods.find(a => a.code === method);
        return m ? m.name : method || '-';
      }
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_: any, record: any) => (
        <Space>
          {record.level < 3 && (
            <Tooltip title="添加子类型">
              <Button
                type="link"
                icon={<PlusOutlined />}
                onClick={() => openModal(undefined, record.costCode)}
              />
            </Tooltip>
          )}
          <Tooltip title="编辑">
            <Button
              type="link"
              icon={<EditOutlined />}
              onClick={() => openModal(record)}
            />
          </Tooltip>
          <Popconfirm
            title="确定要删除这个费用类型吗？"
            onConfirm={() => handleDelete(record.costCode)}
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
      <Row gutter={16}>
        {/* 左侧：树形结构 */}
        <Col span={8}>
          <Card
            title="费用类型结构"
            extra={
              <Button
                type="primary"
                icon={<PlusOutlined />}
                size="small"
                onClick={() => openModal()}
              >
                新增大类
              </Button>
            }
          >
            <Tree
              showIcon
              defaultExpandAll
              onSelect={onTreeSelect}
              treeData={buildTreeData(hierarchy)}
            />
          </Card>
        </Col>

        {/* 右侧：详情和列表 */}
        <Col span={16}>
          {selectedType ? (
            <Card title="费用类型详情">
              <Descriptions bordered column={2}>
                <Descriptions.Item label="费用编码">{selectedType.costCode}</Descriptions.Item>
                <Descriptions.Item label="费用名称">{selectedType.costName}</Descriptions.Item>
                <Descriptions.Item label="层级">
                  {selectedType.level === 1 ? '大类' : selectedType.level === 2 ? '中类' : '小类'}
                </Descriptions.Item>
                <Descriptions.Item label="父级编码">{selectedType.parentCode || '-'}</Descriptions.Item>
                <Descriptions.Item label="费用性质">
                  {renderNatureTag(selectedType.costNature)}
                </Descriptions.Item>
                <Descriptions.Item label="费用类别">
                  {renderCategoryTag(selectedType.costCategory)}
                </Descriptions.Item>
                <Descriptions.Item label="需要分摊">
                  {selectedType.allocationRequired ? '是' : '否'}
                </Descriptions.Item>
                <Descriptions.Item label="分摊方法">
                  {allocationMethods.find(m => m.code === selectedType.allocationMethod)?.name || '-'}
                </Descriptions.Item>
                <Descriptions.Item label="描述" span={2}>
                  {selectedType.description || '-'}
                </Descriptions.Item>
              </Descriptions>

              <div style={{ marginTop: 16 }}>
                <Space>
                  <Button
                    type="primary"
                    icon={<EditOutlined />}
                    onClick={() => openModal(selectedType)}
                  >
                    编辑
                  </Button>
                  {selectedType.level < 3 && (
                    <Button
                      icon={<PlusOutlined />}
                      onClick={() => openModal(undefined, selectedType.costCode)}
                    >
                      添加子类型
                    </Button>
                  )}
                </Space>
              </div>
            </Card>
          ) : (
            <Card title="费用类型列表">
              <Table
                columns={columns}
                dataSource={costTypes}
                rowKey="costCode"
                loading={loading}
                pagination={{ pageSize: 20 }}
                scroll={{ y: 600 }}
              />
            </Card>
          )}
        </Col>
      </Row>

      {/* 新增/编辑弹窗 */}
      <Modal
        title={editingType ? '编辑费用类型' : '新增费用类型'}
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
                label="费用编码"
                rules={[{ required: true, message: '请输入费用编码' }]}
              >
                <Input disabled={!!editingType} placeholder="请输入费用编码" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="costName"
                label="费用名称"
                rules={[{ required: true, message: '请输入费用名称' }]}
              >
                <Input placeholder="请输入费用名称" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="parentCode"
                label="父级编码"
              >
                <Input placeholder="留空为顶级" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="level"
                label="层级"
                rules={[{ required: true, message: '请选择层级' }]}
              >
                <Select placeholder="请选择层级">
                  <Option value={1}>大类</Option>
                  <Option value={2}>中类</Option>
                  <Option value={3}>小类</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="costNature"
                label="费用性质"
                rules={[{ required: true, message: '请选择费用性质' }]}
              >
                <Select placeholder="请选择费用性质">
                  {costNatures.map(n => (
                    <Option key={n.code} value={n.code}>{n.name}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="costCategory"
                label="费用类别"
                rules={[{ required: true, message: '请选择费用类别' }]}
              >
                <Select placeholder="请选择费用类别">
                  {costCategories.map(c => (
                    <Option key={c.code} value={c.code}>{c.name}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="allocationRequired"
                label="需要分摊"
                valuePropName="checked"
                initialValue={true}
              >
                <Switch />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="allocationMethod"
                label="分摊方法"
              >
                <Select placeholder="请选择分摊方法" allowClear>
                  {allocationMethods.map(m => (
                    <Option key={m.code} value={m.code}>{m.name}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="allocationFactor"
            label="分摊因子配置"
          >
            <Input placeholder='JSON格式，如: {"factor":"EMPLOYEE_COUNT"}' />
          </Form.Item>

          <Form.Item
            name="description"
            label="描述"
          >
            <TextArea rows={2} placeholder="请输入描述" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default CostTypePage;
