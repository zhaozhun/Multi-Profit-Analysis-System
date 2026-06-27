import React, { useState, useEffect } from 'react';
import { Table, Collapse, Spin, Empty } from 'antd';
import type { ColumnsType } from 'antd/es/table';

const { Panel } = Collapse;

interface DetailTableProps {
  indicatorCode: string;
  period: string;
  periodValue: string;
  displayFields: string[];
  groupByField: string;
}

interface DetailData {
  groupName: string;
  groupValue: string;
  total: number;
  details: Record<string, any>[];
}

const DetailTable: React.FC<DetailTableProps> = ({
  indicatorCode,
  period,
  periodValue,
  displayFields,
  groupByField,
}) => {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<DetailData[]>([]);
  const [activeKey, setActiveKey] = useState<string[]>([]);

  useEffect(() => {
    fetchDetailData();
  }, [indicatorCode, period, periodValue]);

  const fetchDetailData = async () => {
    setLoading(true);
    try {
      // 这里调用API获取明细数据
      // const result = await getIndicatorDetail(indicatorCode, period, periodValue);
      // setData(result.groups || []);

      // 暂时使用模拟数据
      setData([
        {
          groupName: '2024年1月',
          groupValue: '2024-01',
          total: 100,
          details: [
            { contractId: 'LOAN001', customerName: '华为', balance: 1000, interestAmount: 3.63 },
            { contractId: 'LOAN002', customerName: '腾讯', balance: 800, interestAmount: 3.00 },
          ],
        },
      ]);
    } catch (error) {
      console.error('获取明细数据失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const columns: ColumnsType<Record<string, any>> = displayFields.map((field) => ({
    title: field,
    dataIndex: field,
    key: field,
    ellipsis: true,
  }));

  const renderPanel = (item: DetailData) => {
    return (
      <Panel
        header={
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span>{item.groupName}</span>
            <span>合计: {item.total?.toFixed(2)}</span>
          </div>
        }
        key={item.groupValue}
      >
        <Table
          columns={columns}
          dataSource={item.details}
          pagination={false}
          size="small"
          scroll={{ x: 'max-content' }}
        />
      </Panel>
    );
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px 0' }}>
        <Spin size="large" />
      </div>
    );
  }

  if (data.length === 0) {
    return <Empty description="暂无数据" />;
  }

  return (
    <Collapse
      activeKey={activeKey}
      onChange={(key) => setActiveKey(key as string[])}
    >
      {data.map(renderPanel)}
    </Collapse>
  );
};

export default DetailTable;
