import React, { useState, useRef, useEffect } from 'react';
import { Card, Input, Button, Space, Tag, message } from 'antd';
import {
  SendOutlined, RobotOutlined, UserOutlined,
  ClearOutlined, ThunderboltOutlined,
} from '@ant-design/icons';
import AgentWorkflowCard from '../../components/AgentWorkflowCard';
import agentApi, { AgentChatResponse, AgentInfo } from '../../services/agentApi';

const { TextArea } = Input;

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  agentResponse?: AgentChatResponse;
  timestamp: string;
}

const AiAssistant: React.FC = () => {
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      role: 'assistant',
      content: '你好！我是多维盈利分析系统的AI助手。我可以帮你：\n\n' +
        '📊 **数据查询** — "本月哪个分行利润最高？"\n' +
        '📈 **趋势分析** — "最近6个月收入趋势如何？"\n' +
        '🔍 **深度分析** — "Q3利润为什么下降了？"\n' +
        '💰 **费用分摊** — "检查分摊规则配置"\n' +
        '⚠️ **风险预警** — "检查风险指标"\n' +
        '📋 **生成报告** — "生成月度经营简报"\n\n' +
        '有什么可以帮你的？',
      timestamp: new Date().toLocaleTimeString(),
    },
  ]);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [sessionId, setSessionId] = useState<string>('');
  const [agents, setAgents] = useState<AgentInfo[]>([]);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    // 加载Agent列表
    loadAgents();
  }, []);

  const loadAgents = async () => {
    try {
      const agentList = await agentApi.listAgents();
      setAgents(agentList);
    } catch (error) {
      console.error('加载Agent列表失败:', error);
    }
  };

  const handleSend = async () => {
    if (!inputValue.trim() || loading) return;

    const userMsg: ChatMessage = {
      role: 'user',
      content: inputValue,
      timestamp: new Date().toLocaleTimeString(),
    };
    setMessages(prev => [...prev, userMsg]);
    const question = inputValue;
    setInputValue('');
    setLoading(true);

    try {
      const response = await agentApi.chat({
        message: question,
        sessionId: sessionId || undefined,
      });

      // 更新sessionId
      if (response.sessionId) {
        setSessionId(response.sessionId);
      }

      const assistantMsg: ChatMessage = {
        role: 'assistant',
        content: response.answer,
        agentResponse: response,
        timestamp: new Date().toLocaleTimeString(),
      };
      setMessages(prev => [...prev, assistantMsg]);
    } catch (error) {
      message.error('请求失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  const handleClearSession = () => {
    setSessionId('');
    setMessages([{
      role: 'assistant',
      content: '会话已清除，可以开始新的对话。',
      timestamp: new Date().toLocaleTimeString(),
    }]);
    message.success('会话已清除');
  };

  const quickQuestions = [
    '本月哪个分行利润最高？',
    'Q3利润为什么下降了？',
    '客户价值分布分析',
    '检查分摊规则配置',
    '检查风险指标',
    '生成月度经营简报',
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 160px)' }}>
      {/* 标题栏 */}
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h2 style={{ margin: 0 }}>
            <RobotOutlined style={{ marginRight: 8, color: '#1890ff' }} />
            AI 智能助手
          </h2>
          <p style={{ color: '#999', margin: '4px 0 0' }}>
            支持数据查询、深度分析、费用分摊、风险预警
          </p>
        </div>
        <Space>
          {sessionId && (
            <Tag color="blue">会话ID: {sessionId.substring(0, 8)}...</Tag>
          )}
          <Button
            icon={<ClearOutlined />}
            onClick={handleClearSession}
            size="small"
          >
            清除会话
          </Button>
        </Space>
      </div>

      {/* 快捷Agent入口 */}
      <div style={{ marginBottom: 16 }}>
        <Space wrap>
          {agents.filter(a => !a.triggers.includes('默认')).map(agent => (
            <Tag
              key={agent.name}
              icon={<ThunderboltOutlined />}
              color="blue"
              style={{ cursor: 'pointer' }}
              onClick={() => {
                setInputValue(`请帮我${agent.description}`);
              }}
            >
              {agent.icon} {agent.name}
            </Tag>
          ))}
        </Space>
      </div>

      {/* 对话区 */}
      <Card
        style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}
        bodyStyle={{ flex: 1, overflow: 'auto', padding: 16 }}
      >
        <div style={{ minHeight: '100%' }}>
          {messages.map((msg, index) => (
            <div
              key={index}
              style={{
                display: 'flex',
                justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start',
                marginBottom: 16,
              }}
            >
              <div style={{
                maxWidth: '85%',
                display: 'flex',
                gap: 8,
                flexDirection: msg.role === 'user' ? 'row-reverse' : 'row',
              }}>
                <div style={{
                  width: 36, height: 36, borderRadius: '50%',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  background: msg.role === 'user' ? '#1890ff' : '#52c41a',
                  color: '#fff', flexShrink: 0,
                }}>
                  {msg.role === 'user' ? <UserOutlined /> : <RobotOutlined />}
                </div>
                <div style={{
                  background: msg.role === 'user' ? '#e6f7ff' : '#f6ffed',
                  padding: '12px 16px',
                  borderRadius: 8,
                  whiteSpace: 'pre-wrap',
                  lineHeight: 1.6,
                  maxWidth: '100%',
                }}>
                  {/* 如果有Agent响应，使用卡片展示 */}
                  {msg.agentResponse ? (
                    <AgentWorkflowCard
                      agentName={msg.agentResponse.agentName}
                      agentIcon={msg.agentResponse.agentIcon}
                      status={msg.agentResponse.status === 'COMPLETED' ? 'completed' :
                              msg.agentResponse.status === 'FAILED' ? 'failed' : 'running'}
                      result={msg.agentResponse.answer}
                      confidence={msg.agentResponse.confidence}
                      suggestions={msg.agentResponse.suggestions}
                    />
                  ) : (
                    msg.content
                  )}
                </div>
              </div>
            </div>
          ))}
          {loading && (
            <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
              <div style={{
                width: 36, height: 36, borderRadius: '50%',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                background: '#52c41a', color: '#fff',
              }}>
                <RobotOutlined />
              </div>
              <div style={{
                background: '#f6ffed', padding: '12px 16px',
                borderRadius: 8,
              }}>
                <div className="loading-dots">思考中...</div>
              </div>
            </div>
          )}
          <div ref={messagesEndRef} />
        </div>
      </Card>

      {/* 快捷问题 */}
      <div style={{ margin: '8px 0' }}>
        <Space wrap>
          {quickQuestions.map((q, i) => (
            <Tag
              key={i}
              style={{ cursor: 'pointer' }}
              onClick={() => { setInputValue(q); }}
            >
              {q}
            </Tag>
          ))}
        </Space>
      </div>

      {/* 输入区 */}
      <div style={{ display: 'flex', gap: 8 }}>
        <TextArea
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          placeholder="输入问题，例如：Q3利润为什么下降了？"
          autoSize={{ minRows: 1, maxRows: 4 }}
          onPressEnter={(e) => {
            if (!e.shiftKey) {
              e.preventDefault();
              handleSend();
            }
          }}
        />
        <Button
          type="primary"
          icon={<SendOutlined />}
          onClick={handleSend}
          loading={loading}
          style={{ height: 'auto' }}
        >
          发送
        </Button>
      </div>

      {/* 样式 */}
      <style>{`
        .loading-dots::after {
          content: '...';
          animation: dots 1.5s steps(3, end) infinite;
        }
        @keyframes dots {
          0%, 20% { content: '.'; }
          40% { content: '..'; }
          60%, 100% { content: '...'; }
        }
      `}</style>
    </div>
  );
};

export default AiAssistant;
