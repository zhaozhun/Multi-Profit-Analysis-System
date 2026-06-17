import React, { useState, useRef, useEffect } from 'react';
import { Card, Input, Button, Space, Spin, Tag, Tabs, message } from 'antd';
import {
  SendOutlined, RobotOutlined, UserOutlined,
  FileExcelOutlined, ExperimentOutlined, SearchOutlined,
  BarChartOutlined,
} from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import api from '../../services/api';

const { TextArea } = Input;

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  sql?: string;
  data?: any[];
  chartType?: string;
  timestamp: string;
}

const AiAssistant: React.FC = () => {
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      role: 'assistant',
      content: '你好！我是多维盈利分析系统的AI助手。我可以帮你：\n\n' +
        '📊 **数据查询** — "本月哪个分行利润最高？"\n' +
        '📈 **趋势分析** — "最近6个月收入趋势如何？"\n' +
        '🔍 **异常诊断** — "为什么XX分行利润下降了？"\n' +
        '📋 **生成简报** — 点击按钮一键生成经营简报\n' +
        '🛡️ **数据治理** — 检测数据质量问题\n\n' +
        '有什么可以帮你的？',
      timestamp: new Date().toLocaleTimeString(),
    },
  ]);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [briefLoading, setBriefLoading] = useState(false);
  const [anomalyLoading, setAnomalyLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // 判断是否是数据查询类问题
  const isDataQuery = (text: string): boolean => {
    const keywords = ['排名', '最高', '最低', '多少', '趋势', '对比', '分析', '查询',
      '哪个', '哪些', '几家', '多少家', '利润', '收入', '成本', '规模', '机构', '产品',
      '条线', '渠道', '客户经理', '客户', '分行', '支行'];
    return keywords.some(k => text.includes(k));
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
      let result: any;

      if (isDataQuery(question)) {
        // 数据查询类问题，调用AI探查API
        result = await api.post('/ai-explore/query', { message: question });
        const assistantMsg: ChatMessage = {
          role: 'assistant',
          content: result.answer || '查询完成',
          sql: result.sql,
          data: result.data,
          chartType: result.chartType,
          timestamp: new Date().toLocaleTimeString(),
        };
        setMessages(prev => [...prev, assistantMsg]);
      } else {
        // 通用问答
        result = await api.post('/ai/chat', { message: question });
        const assistantMsg: ChatMessage = {
          role: 'assistant',
          content: result.answer || '抱歉，暂时无法回答。',
          timestamp: new Date().toLocaleTimeString(),
        };
        setMessages(prev => [...prev, assistantMsg]);
      }
    } catch (error) {
      message.error('请求失败');
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateBrief = async () => {
    setBriefLoading(true);
    try {
      const result: any = await api.post('/ai/brief?period=2026-05');
      const msg: ChatMessage = {
        role: 'assistant',
        content: `📋 **2026年5月经营简报**\n\n${result}`,
        timestamp: new Date().toLocaleTimeString(),
      };
      setMessages(prev => [...prev, msg]);
    } catch (error) {
      message.error('生成简报失败');
    } finally {
      setBriefLoading(false);
    }
  };

  const handleDetectAnomaly = async () => {
    setAnomalyLoading(true);
    try {
      const result: any = await api.post('/governance/monitor?period=2026-05');
      let content = '🔍 **数据异常检测结果**\n\n';
      if (result && result.length > 0) {
        result.forEach((item: any, index: number) => {
          content += `${index + 1}. **${item.message}**\n`;
          if (item.aiAnalysis) {
            content += `   💡 ${item.aiAnalysis}\n`;
          }
          content += '\n';
        });
      } else {
        content += '未检测到异常数据，各项指标正常。';
      }
      const msg: ChatMessage = {
        role: 'assistant',
        content,
        timestamp: new Date().toLocaleTimeString(),
      };
      setMessages(prev => [...prev, msg]);
    } catch (error) {
      message.error('异常检测失败');
    } finally {
      setAnomalyLoading(false);
    }
  };

  const quickQuestions = [
    '本月哪个分行利润最高？',
    '最近6个月收入趋势如何？',
    '各产品线的盈利情况？',
    '成本收入比最高的是哪个机构？',
    '客户维度TOP10',
    '各条线盈利对比',
  ];

  const getChartOption = (data: any[], chartType: string) => {
    if (!data || data.length === 0) return null;

    if (chartType === 'bar') {
      const names = data.map((d: any) => d.name || d.org_name || d.product_name || '');
      const values = data.map((d: any) => d.net_profit || d.revenue || 0);
      return {
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: names, axisLabel: { rotate: 30 } },
        yAxis: { type: 'value', name: '金额(万元)' },
        series: [{
          type: 'bar',
          data: values,
          itemStyle: { color: '#1890ff' },
          label: { show: true, position: 'top' },
        }],
      };
    }

    if (chartType === 'line') {
      const names = data.map((d: any) => d.period || d.month || '');
      const values = data.map((d: any) => d.net_profit || d.revenue || 0);
      return {
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: names },
        yAxis: { type: 'value', name: '金额(万元)' },
        series: [{
          type: 'line',
          data: values,
          smooth: true,
          areaStyle: { opacity: 0.3 },
          itemStyle: { color: '#52c41a' },
        }],
      };
    }

    return null;
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 160px)' }}>
      {/* 标题栏 */}
      <div style={{ marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>
          <RobotOutlined style={{ marginRight: 8, color: '#1890ff' }} />
          AI 智能助手
        </h2>
        <p style={{ color: '#999', margin: '4px 0 0' }}>
          支持数据查询、趋势分析、异常诊断、经营简报生成
        </p>
      </div>

      {/* 快捷操作 */}
      <div style={{ marginBottom: 16 }}>
        <Space>
          <Button
            icon={<FileExcelOutlined />}
            loading={briefLoading}
            onClick={handleGenerateBrief}
          >
            生成经营简报
          </Button>
          <Button
            icon={<ExperimentOutlined />}
            loading={anomalyLoading}
            onClick={handleDetectAnomaly}
          >
            异常检测
          </Button>
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
                  {msg.content}
                  {msg.sql && (
                    <div style={{
                      background: '#f5f5f5', padding: 8, borderRadius: 4,
                      marginTop: 8, fontSize: 12, fontFamily: 'monospace',
                    }}>
                      📝 {msg.sql}
                    </div>
                  )}
                  {msg.data && msg.data.length > 0 && getChartOption(msg.data, msg.chartType || 'bar') && (
                    <div style={{ marginTop: 12 }}>
                      <ReactECharts
                        option={getChartOption(msg.data, msg.chartType || 'bar')}
                        style={{ height: 250 }}
                      />
                    </div>
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
                <Spin size="small" /> 思考中...
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
          placeholder="输入问题，例如：本月哪个分行利润最高？"
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
    </div>
  );
};

export default AiAssistant;
