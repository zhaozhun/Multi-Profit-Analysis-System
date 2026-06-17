import React, { useState, useEffect, useCallback } from 'react';
import { Card, Table, Button, Space, Select, Input, Tag, message } from 'antd';
import { DownloadOutlined, SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import api from '../../services/api';

const { Option } = Select;

const Ledger: React.FC = () => {
  const [period, setPeriod] = useState('2026-05');
  const [orgFilter, setOrgFilter] = useState<string>();
  const [productFilter, setProductFilter] = useState<string>();
  const [customerFilter, setCustomerFilter] = useState<string>();
  const [data, setData] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const params: any = { period };
      if (orgFilter) params.orgName = orgFilter;
      if (productFilter) params.productName = productFilter;
      if (customerFilter) params.customerName = customerFilter;

      const result: any = await api.get('/report/ledger', { params });
      setData(result?.list || []);
      setTotal(result?.total || 0);
    } catch (error) {
      console.error('Failed to fetch ledger:', error);
    } finally {
      setLoading(false);
    }
  }, [period, orgFilter, productFilter, customerFilter]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const columns = [
    { title: '业务编号', dataIndex: 'biz_id', width: 130, fixed: 'left' as const },
    { title: '客户名称', dataIndex: 'customer_name', width: 120 },
    { title: '产品', dataIndex: 'product_name', width: 130 },
    { title: '机构', dataIndex: 'org_name', width: 100 },
    { title: '客户经理', dataIndex: 'manager_name', width: 100 },
    { title: '业务金额', dataIndex: 'biz_amount', width: 120, render: (v: number) => v?.toLocaleString() },
    { title: '业务收入', dataIndex: 'revenue', width: 120, render: (v: number) => v?.toLocaleString() },
    { title: 'FTP成本', dataIndex: 'ftp_cost', width: 120, render: (v: number) => v?.toLocaleString() },
    { title: '风险成本', dataIndex: 'risk_cost', width: 100, render: (v: number) => v?.toLocaleString() },
    { title: '运营成本', dataIndex: 'op_cost', width: 100, render: (v: number) => v?.toLocaleString() },
    { title: '净利润', dataIndex: 'net_profit', width: 120, render: (v: number) => (
      <span style={{ color: v >= 0 ? '#52c41a' : '#f5222d', fontWeight: 'bold' }}>{v?.toLocaleString()}</span>
    )},
  ];

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>📋 明细台账查询</h2>
        <p style={{ color: '#999', margin: '4px 0 0' }}>查询最细粒度的单笔业务明细</p>
      </div>

      <Card>
        <div style={{ marginBottom: 16, display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'center' }}>
          <Select value={period} onChange={setPeriod} style={{ width: 130 }}>
            <Option value="2026-05">2026年5月</Option>
            <Option value="2026-04">2026年4月</Option>
            <Option value="2026-03">2026年3月</Option>
          </Select>
          <Select placeholder="机构" allowClear style={{ width: 130 }} onChange={setOrgFilter}>
            <Option value="朝阳支行">朝阳支行</Option>
            <Option value="海淀支行">海淀支行</Option>
            <Option value="浦东支行">浦东支行</Option>
            <Option value="浦西支行">浦西支行</Option>
          </Select>
          <Select placeholder="产品" allowClear style={{ width: 150 }} onChange={setProductFilter}>
            <Option value="流动资金贷款">流动资金贷款</Option>
            <Option value="固定资产贷款">固定资产贷款</Option>
            <Option value="个人住房贷款">个人住房贷款</Option>
            <Option value="个人消费贷款">个人消费贷款</Option>
          </Select>
          <Input placeholder="客户名称" style={{ width: 150 }} prefix={<SearchOutlined />}
            onChange={(e) => setCustomerFilter(e.target.value)} />
          <Button icon={<ReloadOutlined />} onClick={() => {
            setOrgFilter(undefined);
            setProductFilter(undefined);
            setCustomerFilter(undefined);
          }}>重置</Button>
          <Button icon={<DownloadOutlined />} type="primary" onClick={async () => {
            try {
              const params = new URLSearchParams({ period });
              if (orgFilter) params.append('orgName', orgFilter);
              if (productFilter) params.append('productName', productFilter);
              const response = await fetch(`/api/export/ledger?${params}`);
              if (!response.ok) throw new Error('导出失败');
              const blob = await response.blob();
              const url = window.URL.createObjectURL(blob);
              const a = document.createElement('a');
              a.href = url;
              a.download = `ledger_${period}.xlsx`;
              document.body.appendChild(a);
              a.click();
              window.URL.revokeObjectURL(url);
              document.body.removeChild(a);
              message.success('导出成功');
            } catch (error) {
              message.error('导出失败');
            }
          }}>导出Excel</Button>
          <span style={{ color: '#999' }}>共 {total} 条</span>
        </div>

        <Table
          dataSource={data}
          columns={columns}
          rowKey="biz_id"
          loading={loading}
          size="small"
          scroll={{ x: 1400 }}
          pagination={{ pageSize: 20, showSizeChanger: true, showTotal: (t) => `共 ${t} 条` }}
        />
      </Card>
    </div>
  );
};

export default Ledger;
