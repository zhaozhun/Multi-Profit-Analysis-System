import React, { useState, useEffect } from 'react';
import { Card, Table, Collapse, Spin, Button, Descriptions, message } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { useParams, useNavigate } from 'react-router-dom';
import { getCostAllocationResult, getCostOriginalData } from '../../../services/indicatorApi';

const { Panel } = Collapse;

const ExpenseDetailPage: React.FC = () => {
  const { costType } = useParams<{ costType: string }>();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [allocationData, setAllocationData] = useState<any>(null);
  const [originalData, setOriginalData] = useState<any>(null);

  useEffect(() => {
    if (costType) {
      loadData();
    }
  }, [costType]);

  const loadData = async () => {
    setLoading(true);
    try {
      const [allocation, original] = await Promise.all([
        getCostAllocationResult(costType!, '2026-01'),
        getCostOriginalData(costType!, '2026-01', 'DEPT')
      ]);
      setAllocationData(allocation);
      setOriginalData(original);
    } catch (error) {
      console.error('加载费用数据失败:', error);
      message.error('加载费用数据失败');
    } finally {
      setLoading(false);
    }
  };

  const allocationColumns = [
    { title: '账户ID', dataIndex: 'targetAccountId', key: 'targetAccountId' },
    { title: '账户名称', dataIndex: 'accountName', key: 'accountName' },
    { title: '分摊金额', dataIndex: 'allocatedAmount', key: 'allocatedAmount' },
    { title: '分摊规则', dataIndex: 'allocationRule', key: 'allocationRule' },
  ];

  const originalColumns = [
    { title: '维度编码', dataIndex: 'dimCode', key: 'dimCode' },
    { title: '维度名称', dataIndex: 'dimName', key: 'dimName' },
    { title: '金额', dataIndex: 'amount', key: 'amount' },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Button
        icon={<ArrowLeftOutlined />}
        onClick={() => navigate(-1)}
        style={{ marginBottom: 16 }}
      >
        返回
      </Button>

      <Card title={`费用详情 - ${costType}`}>
        <Spin spinning={loading}>
          {/* 汇总数据 */}
          {allocationData && (
            <Card title="汇总数据" style={{ marginBottom: 16 }}>
              <Descriptions>
                <Descriptions.Item label="费用类型">{allocationData.costType}</Descriptions.Item>
                <Descriptions.Item label="总金额">{allocationData.totalAmount} 万元</Descriptions.Item>
                <Descriptions.Item label="记录数">{allocationData.total}</Descriptions.Item>
              </Descriptions>
            </Card>
          )}

          {/* 账户级明细 */}
          {allocationData && (
            <Card title="账户级明细" style={{ marginBottom: 16 }}>
              <Table
                columns={allocationColumns}
                dataSource={allocationData.details}
                rowKey="id"
                pagination={{ pageSize: 10 }}
              />
            </Card>
          )}

          {/* 原始数据 */}
          {originalData && (
            <Card title="原始数据">
              <Collapse>
                <Panel header="按部门汇总" key="dept">
                  <Table
                    columns={originalColumns}
                    dataSource={originalData.details}
                    rowKey="dimCode"
                    pagination={false}
                  />
                </Panel>
              </Collapse>
            </Card>
          )}
        </Spin>
      </Card>
    </div>
  );
};

export default ExpenseDetailPage;
