import React from 'react';
import { Card, Tag, Steps, Spin, Typography } from 'antd';
import {
  CheckCircleOutlined,
  LoadingOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons';

const { Text, Paragraph } = Typography;

interface WorkflowStep {
  id: string;
  name: string;
  status: 'pending' | 'running' | 'completed' | 'failed';
  tool?: string;
  duration?: number;
  output?: string;
}

interface AgentWorkflowCardProps {
  agentName: string;
  agentIcon: string;
  status: 'running' | 'completed' | 'failed';
  steps?: WorkflowStep[];
  result?: string;
  confidence?: number;
  suggestions?: string[];
}

const AgentWorkflowCard: React.FC<AgentWorkflowCardProps> = ({
  agentName,
  agentIcon,
  status,
  steps = [],
  result,
  confidence,
  suggestions,
}) => {
  const getStatusTag = () => {
    switch (status) {
      case 'running':
        return <Tag icon={<LoadingOutlined />} color="processing">执行中</Tag>;
      case 'completed':
        return <Tag icon={<CheckCircleOutlined />} color="success">已完成</Tag>;
      case 'failed':
        return <Tag icon={<CloseCircleOutlined />} color="error">失败</Tag>;
      default:
        return null;
    }
  };

  const getStepIcon = (stepStatus: string) => {
    switch (stepStatus) {
      case 'completed':
        return <CheckCircleOutlined style={{ color: '#52c41a' }} />;
      case 'running':
        return <LoadingOutlined style={{ color: '#1890ff' }} />;
      case 'failed':
        return <CloseCircleOutlined style={{ color: '#ff4d4f' }} />;
      default:
        return <ClockCircleOutlined style={{ color: '#d9d9d9' }} />;
    }
  };

  return (
    <Card
      size="small"
      style={{
        marginBottom: 16,
        border: status === 'running' ? '1px solid #1890ff' : undefined,
      }}
    >
      {/* 标题栏 */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <span style={{ fontSize: 18, marginRight: 8 }}>{agentIcon}</span>
          <Text strong>{agentName}</Text>
        </div>
        {getStatusTag()}
      </div>

      {/* 步骤列表 */}
      {steps.length > 0 && (
        <div style={{ marginBottom: 16 }}>
          {steps.map((step, index) => (
            <div
              key={step.id}
              style={{
                display: 'flex',
                alignItems: 'flex-start',
                marginBottom: 12,
                padding: '8px 12px',
                background: step.status === 'running' ? '#f0f5ff' : undefined,
                borderRadius: 4,
              }}
            >
              <div style={{ marginRight: 12, marginTop: 4 }}>
                {getStepIcon(step.status)}
              </div>
              <div style={{ flex: 1 }}>
                <div>
                  <Text strong>步骤{index + 1}：{step.name}</Text>
                  {step.tool && (
                    <Tag style={{ marginLeft: 8 }} color="default">{step.tool}</Tag>
                  )}
                </div>
                {step.output && (
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {step.output}
                  </Text>
                )}
              </div>
              {step.duration && (
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {step.duration}ms
                </Text>
              )}
            </div>
          ))}
        </div>
      )}

      {/* 加载中状态 */}
      {status === 'running' && (
        <div style={{ textAlign: 'center', padding: '16px 0' }}>
          <Spin />
          <div style={{ marginTop: 8 }}>
            <Text type="secondary">正在分析中...</Text>
          </div>
        </div>
      )}

      {/* 结果展示 */}
      {result && (
        <div style={{ marginTop: 16 }}>
          <div style={{ marginBottom: 8 }}>
            <Text strong>📊 分析结果</Text>
            {confidence && (
              <Tag style={{ marginLeft: 8 }} color="blue">
                置信度：{confidence}%
              </Tag>
            )}
          </div>
          <Paragraph
            style={{
              background: '#f6ffed',
              padding: 16,
              borderRadius: 4,
              whiteSpace: 'pre-wrap',
            }}
          >
            {result}
          </Paragraph>
        </div>
      )}

      {/* 建议的后续问题 */}
      {suggestions && suggestions.length > 0 && (
        <div style={{ marginTop: 16 }}>
          <Text type="secondary" style={{ fontSize: 12 }}>💡 您可能还想问：</Text>
          <div style={{ marginTop: 8 }}>
            {suggestions.map((suggestion, index) => (
              <Tag
                key={index}
                style={{ marginBottom: 8, cursor: 'pointer' }}
                color="default"
              >
                {suggestion}
              </Tag>
            ))}
          </div>
        </div>
      )}
    </Card>
  );
};

export default AgentWorkflowCard;
