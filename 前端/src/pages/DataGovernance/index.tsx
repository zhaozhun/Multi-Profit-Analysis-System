import React, { useState, useEffect, useCallback } from 'react';
import { Card, Progress, Row, Col, Tag, Button, Space, Spin, message } from 'antd';
import { CheckCircleOutlined, WarningOutlined, CloseCircleOutlined, ReloadOutlined, DownloadOutlined } from '@ant-design/icons';
import api from '../../services/api';

interface QualityItem {
  name: string;
  score: number;
  status: string;
  desc: string;
}

interface Issue {
  type: string;
  level: string;
  title: string;
  aiAnalysis: string;
  suggestion: string;
  status: string;
}

const DataGovernance: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [period, setPeriod] = useState('2026-05');
  const [overallScore, setOverallScore] = useState(0);
  const [qualityItems, setQualityItems] = useState<QualityItem[]>([]);
  const [issues, setIssues] = useState<Issue[]>([]);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      // 并行获取质量报告和问题列表
      const [reportRes, issuesRes]: any[] = await Promise.all([
        api.post(`/governance/scan?period=${period}`),
        api.get(`/governance/issues?period=${period}`),
      ]);

      if (reportRes) {
        setOverallScore(reportRes.overallScore || 0);
        setQualityItems([
          { name: '完整性', score: reportRes.completeness?.score || 0, status: reportRes.completeness?.score >= 90 ? 'good' : 'warning', desc: reportRes.completeness?.desc || '' },
          { name: '一致性', score: reportRes.consistency?.score || 0, status: reportRes.consistency?.score >= 90 ? 'good' : 'warning', desc: reportRes.consistency?.desc || '' },
          { name: '准确性', score: reportRes.accuracy?.score || 0, status: reportRes.accuracy?.score >= 90 ? 'good' : 'warning', desc: reportRes.accuracy?.desc || '' },
          { name: '时效性', score: reportRes.timeliness?.score || 0, status: reportRes.timeliness?.score >= 90 ? 'good' : 'warning', desc: reportRes.timeliness?.desc || '' },
        ]);
      }

      if (issuesRes) {
        setIssues(issuesRes);
      }
    } catch (error) {
      console.error('Failed to fetch governance data:', error);
    } finally {
      setLoading(false);
    }
  }, [period]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const getScoreColor = (score: number) => {
    if (score >= 90) return '#52c41a';
    if (score >= 80) return '#fa8c16';
    return '#f5222d';
  };

  const getStatusIcon = (status: string) => {
    if (status === 'good') return <CheckCircleOutlined style={{ color: '#52c41a' }} />;
    if (status === 'warning') return <WarningOutlined style={{ color: '#fa8c16' }} />;
    return <CloseCircleOutlined style={{ color: '#f5222d' }} />;
  };

  const handleScan = () => {
    fetchData();
    message.success('扫描完成');
  };

  return (
    <Spin spinning={loading}>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h2 style={{ margin: 0 }}>🛡️ 数据质量治理</h2>
          <p style={{ color: '#999', margin: '4px 0 0' }}>AI自动检测数据质量问题，提供治理建议</p>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={handleScan}>重新扫描</Button>
          <Button icon={<DownloadOutlined />}>导出报告</Button>
        </Space>
      </div>

      {/* 质量评分 */}
      <Card style={{ marginBottom: 16 }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <Progress
            type="dashboard"
            percent={overallScore}
            strokeColor={getScoreColor(overallScore)}
            format={(percent) => (
              <div>
                <div style={{ fontSize: 36, fontWeight: 'bold', color: getScoreColor(percent!) }}>{percent}</div>
                <div style={{ fontSize: 14, color: '#999' }}>质量评分</div>
              </div>
            )}
            size={160}
          />
          <div style={{ marginTop: 8 }}>
            <Tag color={overallScore >= 90 ? 'green' : overallScore >= 80 ? 'orange' : 'red'}>
              {overallScore >= 90 ? '优秀' : overallScore >= 80 ? '良好' : '需改进'}
            </Tag>
          </div>
        </div>

        <Row gutter={16}>
          {qualityItems.map((item, i) => (
            <Col xs={12} md={6} key={i}>
              <Card size="small" hoverable>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                  <span style={{ fontWeight: 500 }}>{item.name}</span>
                  {getStatusIcon(item.status)}
                </div>
                <div style={{ fontSize: 28, fontWeight: 'bold', color: getScoreColor(item.score) }}>{item.score}分</div>
                <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>{item.desc}</div>
              </Card>
            </Col>
          ))}
        </Row>
      </Card>

      {/* 问题列表 */}
      <Card title={`发现 ${issues.length} 个质量问题`}>
        {issues.length === 0 ? (
          <div style={{ textAlign: 'center', color: '#999', padding: 40 }}>
            <CheckCircleOutlined style={{ fontSize: 48, color: '#52c41a', marginBottom: 16 }} />
            <div>数据质量良好，未发现问题</div>
          </div>
        ) : (
          issues.map((issue, i) => (
            <Card key={i} size="small" style={{ marginBottom: 12, borderLeft: `4px solid ${issue.level === 'critical' ? '#f5222d' : issue.level === 'warning' ? '#fa8c16' : '#1890ff'}` }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 8 }}>
                <div>
                  <Tag color={issue.level === 'critical' ? 'red' : issue.level === 'warning' ? 'orange' : 'blue'}>{issue.type}</Tag>
                  <span style={{ fontWeight: 500 }}>{issue.title}</span>
                </div>
                <Tag color={issue.status === 'processed' ? 'green' : 'red'}>
                  {issue.status === 'processed' ? '已处理' : '待处理'}
                </Tag>
              </div>
              <div style={{ background: '#f6ffed', padding: '8px 12px', borderRadius: 4, marginBottom: 8, fontSize: 13 }}>
                🤖 {issue.aiAnalysis}
              </div>
              <div style={{ color: '#666', fontSize: 13, marginBottom: 8 }}>
                💡 建议：{issue.suggestion}
              </div>
              <Space>
                <Button size="small" type="primary" onClick={() => message.success('已标记处理')}>标记已处理</Button>
                <Button size="small">查看详情</Button>
              </Space>
            </Card>
          ))
        )}
      </Card>
    </Spin>
  );
};

export default DataGovernance;
