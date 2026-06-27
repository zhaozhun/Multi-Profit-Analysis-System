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
  message,
  Tag,
  Popconfirm,
  Tooltip,
  Row,
  Col,
  Statistic,
  Tabs,
  Switch,
  Descriptions
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  CalculatorOutlined,
  EyeOutlined
} from '@ant-design/icons';
import { commissionConfigApi } from '@/services/api';
import dayjs from 'dayjs';

const { Option } = Select;
const { TabPane } = Tabs;

interface ProductCommissionConfig {
  id: number;
  productCode: string;
  productName: string;
  productType: string;
  needCommission: boolean;
  commissionType: string;
  calcBase: string;
  commissionRate: number;
  minCommission: number;
  maxCommission: number;
  receiverType: string;
  receiverCode: string;
  receiverName: string;
  status: string;
}

interface CommissionDetail {
  productCode: string;
  productName: string;
  productType: string;
  commissionType: string;
  calcBase: string;
  baseAmount: number;
  commissionRate: number;
  commissionAmount: number;
  receiverType: string;
  receiverCode: string;
  receiverName: string;
}

const ProductCommissionPage: React.FC = () => {
  const [configs, setConfigs] = useState<ProductCommissionConfig[]>([]);
  const [commissionProducts, setCommissionProducts] = useState<ProductCommissionConfig[]>([]);
  const [nonCommissionProducts, setNonCommissionProducts] = useState<ProductCommissionConfig[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [previewVisible, setPreviewVisible] = useState(false);
  const [calculateVisible, setCalculateVisible] = useState(false);
  const [editingConfig, setEditingConfig] = useState<ProductCommissionConfig | null>(null);
  const [previewData, setPreviewData] = useState<any[]>([]);
  const [calculateResult, setCalculateResult] = useState<any>(null);
  const [period, setPeriod] = useState<dayjs.Dayjs>(dayjs('2025-06-01'));
  const [form] = Form.useForm();

  // 产品类型选项
  const productTypes = [
    { code: 'LOAN', name: '贷款' },
    { code: 'DEPOSIT', name: '存款' },
    { code: 'WEALTH', name: '理财' },
    { code: 'FUND', name: '基金' },
    { code: 'INSURANCE', name: '保险' },
    { code: 'FEE', name: '中间业务' }
  ];

  // 分润类型选项
  const commissionTypes = [
    { code: 'REVENUE_SHARE', name: '收入分润' },
    { code: 'PROFIT_SHARE', name: '利润分润' },
    { code: 'BALANCE_SHARE', name: '余额分润' }
  ];

  // 计算基数选项
  const calcBases = [
    { code: 'INTEREST_INCOME', name: '利息收入' },
    { code: 'LOAN_BALANCE', name: '贷款余额' },
    { code: 'FEE_INCOME', name: '手续费收入' },
    { code: 'NET_PROFIT', name: '净利润' },
    { code: 'TOTAL_REVENUE', name: '总收入' }
  ];

  // 接收方类型选项
  const receiverTypes = [
    { code: 'BRANCH', name: '机构' },
    { code: 'DEPT', name: '部门' },
    { code: 'PERSON', name: '个人' }
  ];

  // 加载配置
  const loadConfigs = async () => {
    setLoading(true);
    try {
      const [allRes, commissionRes, nonCommissionRes] = await Promise.all([
        commissionConfigApi.getAllConfigs(),
        commissionConfigApi.getCommissionConfigs(),
        commissionConfigApi.getNonCommissionConfigs()
      ]);

      if (Array.isArray(allRes)) setConfigs(allRes);
      if (Array.isArray(commissionRes)) setCommissionProducts(commissionRes);
      if (Array.isArray(nonCommissionRes)) setNonCommissionProducts(nonCommissionRes);
    } catch (error) {
      message.error('加载配置失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadConfigs();
  }, []);

  // 打开编辑弹窗
  const openModal = (config?: ProductCommissionConfig) => {
    setEditingConfig(config || null);
    if (config) {
      form.setFieldsValue(config);
    } else {
      form.resetFields();
    }
    setModalVisible(true);
  };

  // 关闭弹窗
  const closeModal = () => {
    setModalVisible(false);
    setEditingConfig(null);
    form.resetFields();
  };

  // 保存配置
  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      if (editingConfig) {
        await commissionConfigApi.updateConfig(editingConfig.productCode, values);
        message.success('配置更新成功');
      }
      closeModal();
      loadConfigs();
    } catch (error) {
      message.error('保存失败');
    }
  };

  // 预览分润
  const handlePreview = async () => {
    try {
      const response = await commissionConfigApi.previewCommission(period.format('YYYY-MM'));
      if (response) {
        setPreviewData(response.data);
        setPreviewVisible(true);
      }
    } catch (error) {
      message.error('预览失败');
    }
  };

  // 计算分润
  const handleCalculate = async () => {
    try {
      const response = await commissionConfigApi.calculateCommission(period.format('YYYY-MM'));
      if (response) {
        setCalculateResult(response.data);
        setCalculateVisible(true);
      }
    } catch (error) {
      message.error('计算失败');
    }
  };

  // 计算统计
  const totalCommissionProducts = commissionProducts.length;
  const totalNonCommissionProducts = nonCommissionProducts.length;
  const estimatedCommission = previewData.reduce((sum, item) => sum + (item.estimatedCommission || 0), 0);

  // 配置表格列定义
  const configColumns = [
    {
      title: '产品编码',
      dataIndex: 'productCode',
      key: 'productCode',
      width: 120
    },
    {
      title: '产品名称',
      dataIndex: 'productName',
      key: 'productName',
      width: 150
    },
    {
      title: '产品类型',
      dataIndex: 'productType',
      key: 'productType',
      width: 100,
      render: (type: string) => {
        const t = productTypes.find(p => p.code === type);
        return <Tag>{t ? t.name : type}</Tag>;
      }
    },
    {
      title: '是否分润',
      dataIndex: 'needCommission',
      key: 'needCommission',
      width: 100,
      render: (val: boolean) => val ? <Tag color="green">是</Tag> : <Tag color="red">否</Tag>
    },
    {
      title: '分润类型',
      dataIndex: 'commissionType',
      key: 'commissionType',
      width: 120,
      render: (type: string) => {
        const t = commissionTypes.find(c => c.code === type);
        return t ? t.name : type || '-';
      }
    },
    {
      title: '计算基数',
      dataIndex: 'calcBase',
      key: 'calcBase',
      width: 120,
      render: (base: string) => {
        const b = calcBases.find(c => c.code === base);
        return b ? b.name : base || '-';
      }
    },
    {
      title: '分润费率',
      dataIndex: 'commissionRate',
      key: 'commissionRate',
      width: 100,
      render: (rate: number) => rate ? (rate * 100).toFixed(2) + '%' : '-'
    },
    {
      title: '接收方',
      dataIndex: 'receiverName',
      key: 'receiverName',
      width: 120,
      render: (name: string) => name || '-'
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: any, record: any) => (
        <Space>
          <Tooltip title="编辑">
            <Button
              type="link"
              icon={<EditOutlined />}
              onClick={() => openModal(record)}
            />
          </Tooltip>
        </Space>
      )
    }
  ];

  // 预览表格列定义
  const previewColumns = [
    {
      title: '产品编码',
      dataIndex: 'productCode',
      key: 'productCode',
      width: 120
    },
    {
      title: '产品名称',
      dataIndex: 'productName',
      key: 'productName',
      width: 150
    },
    {
      title: '产品类型',
      dataIndex: 'productType',
      key: 'productType',
      width: 100
    },
    {
      title: '是否分润',
      dataIndex: 'needCommission',
      key: 'needCommission',
      width: 100,
      render: (val: boolean) => val ? <Tag color="green">是</Tag> : <Tag color="red">否</Tag>
    },
    {
      title: '利息收入',
      dataIndex: 'interestIncome',
      key: 'interestIncome',
      width: 120,
      render: (val: number) => val?.toLocaleString() || '-'
    },
    {
      title: '贷款余额',
      dataIndex: 'loanBalance',
      key: 'loanBalance',
      width: 120,
      render: (val: number) => val?.toLocaleString() || '-'
    },
    {
      title: '分润费率',
      dataIndex: 'commissionRate',
      key: 'commissionRate',
      width: 100,
      render: (rate: number) => rate ? (rate * 100).toFixed(2) + '%' : '-'
    },
    {
      title: '预估分润',
      dataIndex: 'estimatedCommission',
      key: 'estimatedCommission',
      width: 120,
      render: (val: number) => val ? val.toLocaleString() : '-'
    }
  ];

  // 计算结果表格列定义
  const resultColumns = [
    {
      title: '产品编码',
      dataIndex: 'productCode',
      key: 'productCode',
      width: 120
    },
    {
      title: '产品名称',
      dataIndex: 'productName',
      key: 'productName',
      width: 150
    },
    {
      title: '分润类型',
      dataIndex: 'commissionType',
      key: 'commissionType',
      width: 120
    },
    {
      title: '计算基数',
      dataIndex: 'calcBase',
      key: 'calcBase',
      width: 120
    },
    {
      title: '基数金额',
      dataIndex: 'baseAmount',
      key: 'baseAmount',
      width: 120,
      render: (val: number) => val?.toLocaleString()
    },
    {
      title: '分润费率',
      dataIndex: 'commissionRate',
      key: 'commissionRate',
      width: 100,
      render: (rate: number) => rate ? (rate * 100).toFixed(2) + '%' : '-'
    },
    {
      title: '分润金额',
      dataIndex: 'commissionAmount',
      key: 'commissionAmount',
      width: 120,
      render: (val: number) => val?.toLocaleString()
    },
    {
      title: '接收方',
      dataIndex: 'receiverName',
      key: 'receiverName',
      width: 120
    }
  ];

  return (
    <div>
      <Card title="产品分润配置">
        {/* 统计卡片 */}
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={6}>
            <Card>
              <Statistic title="分润产品数" value={totalCommissionProducts} suffix="个" />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="非分润产品数" value={totalNonCommissionProducts} suffix="个" />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="预估分润总额" value={estimatedCommission} precision={2} suffix="元" />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Space>
                <Button
                  type="primary"
                  icon={<EyeOutlined />}
                  onClick={handlePreview}
                >
                  预览分润
                </Button>
                <Button
                  icon={<CalculatorOutlined />}
                  onClick={handleCalculate}
                >
                  计算分润
                </Button>
              </Space>
            </Card>
          </Col>
        </Row>

        {/* 产品配置表格 */}
        <Tabs defaultActiveKey="commission">
          <TabPane tab={`分润产品 (${totalCommissionProducts})`} key="commission">
            <Table
              columns={configColumns}
              dataSource={commissionProducts}
              rowKey="productCode"
              loading={loading}
              pagination={false}
            />
          </TabPane>
          <TabPane tab={`非分润产品 (${totalNonCommissionProducts})`} key="non-commission">
            <Table
              columns={configColumns}
              dataSource={nonCommissionProducts}
              rowKey="productCode"
              loading={loading}
              pagination={false}
            />
          </TabPane>
          <TabPane tab="全部产品" key="all">
            <Table
              columns={configColumns}
              dataSource={configs}
              rowKey="productCode"
              loading={loading}
              pagination={{ pageSize: 20 }}
            />
          </TabPane>
        </Tabs>
      </Card>

      {/* 编辑弹窗 */}
      <Modal
        title="编辑产品分润配置"
        open={modalVisible}
        onOk={handleSave}
        onCancel={closeModal}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="productCode" label="产品编码">
                <Input disabled />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="productName" label="产品名称">
                <Input disabled />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="needCommission" label="是否需要分润" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="commissionType" label="分润类型">
                <Select placeholder="请选择分润类型">
                  {commissionTypes.map(t => (
                    <Option key={t.code} value={t.code}>{t.name}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="calcBase" label="计算基数">
                <Select placeholder="请选择计算基数">
                  {calcBases.map(b => (
                    <Option key={b.code} value={b.code}>{b.name}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="commissionRate" label="分润费率">
                <InputNumber
                  style={{ width: '100%' }}
                  min={0}
                  max={1}
                  step={0.01}
                  formatter={value => `${(Number(value) * 100).toFixed(2)}%`}
                  parser={value => Number(value!.replace('%', '')) / 100 as any}
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="minCommission" label="最低分润金额">
                <InputNumber style={{ width: '100%' }} min={0} precision={2} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="maxCommission" label="最高分润金额">
                <InputNumber style={{ width: '100%' }} min={0} precision={2} />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="receiverType" label="接收方类型">
                <Select placeholder="请选择接收方类型">
                  {receiverTypes.map(r => (
                    <Option key={r.code} value={r.code}>{r.name}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="receiverName" label="接收方名称">
                <Input placeholder="请输入接收方名称" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      {/* 预览弹窗 */}
      <Modal
        title="分润预览"
        open={previewVisible}
        onCancel={() => setPreviewVisible(false)}
        footer={null}
        width={1200}
      >
        <Table
          columns={previewColumns}
          dataSource={previewData}
          rowKey="productCode"
          pagination={false}
          scroll={{ x: 1000 }}
          summary={() => (
            <Table.Summary fixed>
              <Table.Summary.Row>
                <Table.Summary.Cell index={0} colSpan={7}>
                  <strong>预估分润合计</strong>
                </Table.Summary.Cell>
                <Table.Summary.Cell index={7}>
                  <strong>{estimatedCommission.toLocaleString()}</strong>
                </Table.Summary.Cell>
              </Table.Summary.Row>
            </Table.Summary>
          )}
        />
      </Modal>

      {/* 计算结果弹窗 */}
      <Modal
        title="分润计算结果"
        open={calculateVisible}
        onCancel={() => setCalculateVisible(false)}
        footer={null}
        width={1200}
      >
        {calculateResult && (
          <>
            <Descriptions bordered column={3} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="期间">{calculateResult.period}</Descriptions.Item>
              <Descriptions.Item label="产品总数">{calculateResult.totalProducts}</Descriptions.Item>
              <Descriptions.Item label="分润产品数">{calculateResult.commissionProducts}</Descriptions.Item>
              <Descriptions.Item label="分润总额">{calculateResult.totalCommission?.toLocaleString()}</Descriptions.Item>
            </Descriptions>
            <Table
              columns={resultColumns}
              dataSource={calculateResult.details}
              rowKey="productCode"
              pagination={false}
              scroll={{ x: 1000 }}
            />
          </>
        )}
      </Modal>
    </div>
  );
};

export default ProductCommissionPage;
