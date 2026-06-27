// @ts-nocheck
import React, { useState, useEffect, useCallback } from 'react';
import { Card, Table, Button, Space, Tag, Modal, Form, Input, Select, message } from 'antd';
import { PlusOutlined, EditOutlined, CalculatorOutlined, ReloadOutlined } from '@ant-design/icons';
import api from '../../../../services/api';

const { Option } = Select;

interface IndicatorPageProps {
  category: string;
  title: string;
  icon: string;
  color: string;
}

const IndicatorPage: React.FC<IndicatorPageProps> = ({ category, title, icon, color }) => {
  const [data, setData] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [calcModalVisible, setCalcModalVisible] = useState(false);
  const [selectedIndicator, setSelectedIndicator] = useState<any>(null);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const result: any = await api.get('/indicator', { params: { category } });
      setData(result?.list || []);
    } catch (error) {
      console.error('Failed to fetch indicators:', error);
    } finally {
      setLoading(false);
    }
  }, [category]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleCreate = async () => {
    try {
      const values = await form.validateFields();
      values.category = category;
      await api.post('/indicator', values);
      message.success('新增成功');
      setModalVisible(false);
      form.resetFields();
      fetchData();
    } catch (error: any) {
      message.error(error.message || '新增失败');
    }
  };

  const handleCalc = async () => {
    if (!selectedIndicator) return;
    try {
      await api.post(`/indicator/${selectedIndicator.id}/calc`, null, {
        params: { period: '2026-05', calcPeriod: 'MONTH' }
      });
      message.success('预计算完成');
      setCalcModalVisible(false);
    } catch (error: any) {
      message.error(error.message || '计算失败');
    }
  };

  const columns = [
    { title: '编码', dataIndex: 'code', width: 180 },
    { title: '名称', dataIndex: 'name', width: 150 },
    { title: '分类', dataIndex: 'category', width: 100, render: (v: string) => (
      <Tag color={color}>{title}</Tag>
    )},
    { title: '单位', dataIndex: 'unit', width: 80 },
    { title: '计算公式', dataIndex: 'calc_formula', width: 250, ellipsis: true },
    { title: '预计算周期', dataIndex: 'pre_calc_periods', width: 200, render: (v: string) => {
      if (!v) return '-';
      try {
        const periods = JSON.parse(v);
        return <Space>{periods.map((p: string) => <Tag key={p}>{p}</Tag>)}</Space>;
      } catch { return v; }
    }},
    { title: '状态', dataIndex: 'status', width: 80, render: (v: number) => (
      <Tag color={v === 1 ? 'green' : 'red'}>{v === 1 ? '启用' : '停用'}</Tag>
    )},
    { title: '操作', width: 180, render: (_: any, record: any) => (
      <Space>
        <Button size="small" icon={<EditOutlined />} onClick={() => {
          form.setFieldsValue(record);
          setModalVisible(true);
        }}>编辑</Button>
        <Button size="small" icon={<CalculatorOutlined />} type="primary" ghost onClick={() => {
          setSelectedIndicator(record);
          setCalcModalVisible(true);
        }}>计算</Button>
      </Space>
    )},
  ];

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>{icon} {title}</h2>
        <p style={{ color: '#999', margin: '4px 0 0' }}>管理{title}，配置预计算周期</p>
      </div>

      <Card>
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={fetchData}>刷新</Button>
          </Space>
          <Space>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => {
              form.resetFields();
              setModalVisible(true);
            }}>新增指标</Button>
          </Space>
        </div>

        <Table
          dataSource={data}
          columns={columns}
          rowKey="id"
          loading={loading}
          size="small"
          pagination={{ pageSize: 15 }}
        />
      </Card>

      <Modal
        title="新增指标"
        open={modalVisible}
        onOk={handleCreate}
        onCancel={() => setModalVisible(false)}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item label="指标编码" name="code" rules={[{ required: true, message: '请输入编码' }]}>
            <Input placeholder="如：REV_TOTAL" />
          </Form.Item>
          <Form.Item label="指标名称" name="name" rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="如：业务总收入" />
          </Form.Item>
          <Form.Item label="单位" name="unit">
            <Select placeholder="选择单位">
              <Option value="万元">万元</Option>
              <Option value="%">%</Option>
              <Option value="个">个</Option>
              <Option value="笔">笔</Option>
            </Select>
          </Form.Item>
          <Form.Item label="计算公式" name="calcFormula">
            <Input.TextArea rows={2} placeholder="如：sum(revenue)" />
          </Form.Item>
          <Form.Item label="来源字段" name="sourceField">
            <Input placeholder="如：revenue" />
          </Form.Item>
          <Form.Item label="预计算周期" name="preCalcPeriods">
            <Select mode="multiple" placeholder="选择预计算周期">
              <Option value="DAY">自然日</Option>
              <Option value="MONTH">自然月</Option>
              <Option value="QUARTER">自然季度</Option>
              <Option value="YEAR">自然年</Option>
            </Select>
          </Form.Item>
          <Form.Item label="口径说明" name="description">
            <Input.TextArea rows={3} placeholder="指标口径和计算逻辑说明" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="触发预计算"
        open={calcModalVisible}
        onOk={handleCalc}
        onCancel={() => setCalcModalVisible(false)}
      >
        {selectedIndicator && (
          <div>
            <p><strong>指标：</strong>{selectedIndicator.name} ({selectedIndicator.code})</p>
            <p><strong>计算周期：</strong>2026-05（自然月）</p>
            <p><strong>说明：</strong>将按机构维度计算该指标的汇总值</p>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default IndicatorPage;
