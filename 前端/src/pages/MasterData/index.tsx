import React, { useState, useEffect, useCallback } from 'react';
import { Card, Table, Button, Space, Select, Tag, Modal, Form, Input, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, UploadOutlined, DownloadOutlined, ReloadOutlined } from '@ant-design/icons';
import api from '../../services/api';

const { Option } = Select;

const dimTypes = [
  { value: 'ORG', label: '机构', icon: '🏦' },
  { value: 'BIZ_LINE', label: '条线', icon: '📊' },
  { value: 'DEPT', label: '部门', icon: '🏢' },
  { value: 'PRODUCT', label: '产品', icon: '📦' },
  { value: 'CHANNEL', label: '渠道', icon: '🔗' },
  { value: 'MANAGER', label: '客户经理', icon: '👤' },
  { value: 'CUSTOMER', label: '客户', icon: '🤝' },
];

const MasterData: React.FC = () => {
  const [dimType, setDimType] = useState('ORG');
  const [data, setData] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState<any>(null);
  const [expandedRowKeys, setExpandedRowKeys] = useState<string[]>([]);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const result: any = await api.get(`/master/${dimType}`);
      setData(result?.tree || []);
    } catch (error) {
      console.error('Failed to fetch data:', error);
    } finally {
      setLoading(false);
    }
  }, [dimType]);

  useEffect(() => {
    fetchData();
    setExpandedRowKeys([]);
  }, [fetchData]);

  const handleCreate = async () => {
    try {
      const values = await form.validateFields();
      await api.post(`/master/${dimType}`, values);
      message.success('新增成功');
      setModalVisible(false);
      form.resetFields();
      fetchData();
    } catch (error: any) {
      if (error.message) message.error(error.message);
    }
  };

  const handleUpdate = async () => {
    try {
      const values = await form.validateFields();
      await api.put(`/master/${dimType}/${editingRecord.id}`, values);
      message.success('更新成功');
      setModalVisible(false);
      form.resetFields();
      fetchData();
    } catch (error: any) {
      if (error.message) message.error(error.message);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await api.delete(`/master/${dimType}/${id}`);
      message.success('删除成功');
      fetchData();
    } catch (error: any) {
      message.error(error.message || '删除失败');
    }
  };

  // 获取所有子孙节点的key
  const getAllDescendantKeys = (record: any): string[] => {
    const keys: string[] = [];
    const collectKeys = (node: any) => {
      keys.push(String(node.id));
      if (node.children && node.children.length > 0) {
        node.children.forEach(collectKeys);
      }
    };
    if (record.children && record.children.length > 0) {
      record.children.forEach(collectKeys);
    }
    return keys;
  };

  // 双击展开/折叠
  const handleDoubleClick = (record: any) => {
    if (!record.hasChildren) return;

    const recordKey = String(record.id);
    const descendantKeys = getAllDescendantKeys(record);
    const allKeys = [recordKey, ...descendantKeys];

    // 检查是否已展开
    const isExpanded = expandedRowKeys.includes(recordKey);

    if (isExpanded) {
      // 折叠：移除当前节点和所有子孙
      setExpandedRowKeys(prev => prev.filter(k => !allKeys.includes(k)));
    } else {
      // 展开：添加当前节点和所有子孙
      setExpandedRowKeys(prev => [...new Set([...prev, ...allKeys])]);
    }
  };

  const columns = [
    { title: '编码', dataIndex: 'code', width: 120 },
    {
      title: '名称', dataIndex: 'name', width: 200,
      render: (text: string, record: any) => (
        <span
          style={{ cursor: record.hasChildren ? 'pointer' : 'default', color: record.hasChildren ? '#1890ff' : undefined }}
          onDoubleClick={() => handleDoubleClick(record)}
        >
          {text}
          {record.hasChildren && (
            <Tag color="blue" style={{ marginLeft: 8, fontSize: 11 }}>
              {record.children?.length || 0}个子节点
            </Tag>
          )}
        </span>
      ),
    },
    { title: '层级', dataIndex: 'level', width: 80, render: (v: number) => `第${v}级` },
    { title: '状态', dataIndex: 'status', width: 80, render: (v: number) => (
      <Tag color={v === 1 ? 'green' : 'red'}>{v === 1 ? '启用' : '停用'}</Tag>
    )},
    { title: '操作', width: 200, render: (_: any, record: any) => (
      <Space>
        <Button size="small" icon={<PlusOutlined />} onClick={() => {
          setEditingRecord(null);
          form.resetFields();
          form.setFieldsValue({ parentId: record.id, level: record.level + 1 });
          setModalVisible(true);
        }}>新增子节点</Button>
        <Button size="small" icon={<EditOutlined />} onClick={() => {
          setEditingRecord(record);
          form.setFieldsValue(record);
          setModalVisible(true);
        }}>编辑</Button>
        <Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)}>删除</Button>
      </Space>
    )},
  ];

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>📁 主数据管理</h2>
        <p style={{ color: '#999', margin: '4px 0 0' }}>
          管理各类维度主数据，双击名称可展开/折叠子节点
        </p>
      </div>

      <Card>
        <div style={{ marginBottom: 16, display: 'flex', gap: 16, alignItems: 'center' }}>
          <span>维度类型：</span>
          <Select value={dimType} onChange={setDimType} style={{ width: 150 }}>
            {dimTypes.map(d => (
              <Option key={d.value} value={d.value}>{d.icon} {d.label}</Option>
            ))}
          </Select>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => {
            setEditingRecord(null);
            form.resetFields();
            form.setFieldsValue({ level: 1 });
            setModalVisible(true);
          }}>新增顶级</Button>
          <Button icon={<ReloadOutlined />} onClick={fetchData}>刷新</Button>
          <Button icon={<UploadOutlined />}>导入</Button>
          <Button icon={<DownloadOutlined />}>导出</Button>
          <Button onClick={() => setExpandedRowKeys(data.map((d: any) => String(d.id)))}>
            全部展开
          </Button>
          <Button onClick={() => setExpandedRowKeys([])}>全部折叠</Button>
        </div>

        <Table
          dataSource={data}
          columns={columns}
          rowKey={(record) => String(record.id)}
          loading={loading}
          size="small"
          pagination={false}
          defaultExpandAllRows={true}
          expandable={{
            expandedRowKeys,
            onExpand: (expanded, record) => {
              const key = String(record.id);
              if (expanded) {
                setExpandedRowKeys(prev => [...prev, key]);
              } else {
                setExpandedRowKeys(prev => prev.filter(k => k !== key));
              }
            },
          }}
        />
      </Card>

      <Modal
        title={editingRecord ? '编辑' : '新增'}
        open={modalVisible}
        onOk={editingRecord ? handleUpdate : handleCreate}
        onCancel={() => setModalVisible(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item label="编码" name="code" rules={[{ required: true, message: '请输入编码' }]}>
            <Input placeholder="如：ORG001" disabled={!!editingRecord} />
          </Form.Item>
          <Form.Item label="名称" name="name" rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="如：北京分行" />
          </Form.Item>
          <Form.Item label="父级ID" name="parentId" hidden>
            <Input />
          </Form.Item>
          <Form.Item label="层级" name="level">
            <Select>
              <Option value={1}>第1级</Option>
              <Option value={2}>第2级</Option>
              <Option value={3}>第3级</Option>
              <Option value={4}>第4级</Option>
            </Select>
          </Form.Item>
          <Form.Item label="排序号" name="sortOrder">
            <Input type="number" placeholder="数字越小越靠前" />
          </Form.Item>
          <Form.Item label="状态" name="status" initialValue={1}>
            <Select>
              <Option value={1}>启用</Option>
              <Option value={0}>停用</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default MasterData;
