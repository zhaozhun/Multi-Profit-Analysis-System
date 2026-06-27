import api from './api';

// 类型定义
export interface AgentChatRequest {
  message: string;
  sessionId?: string;
  context?: Record<string, any>;
}

export interface AgentChatResponse {
  agentName: string;
  agentIcon: string;
  answer: string;
  data?: Record<string, any>;
  sessionId: string;
  status: string;
  confidence?: number;
  suggestions?: string[];
}

export interface AgentInfo {
  name: string;
  icon: string;
  description: string;
  triggers: string[];
}

// API服务
export const agentApi = {
  /**
   * Agent对话
   */
  chat: async (request: AgentChatRequest): Promise<AgentChatResponse> => {
    const response = await api.post('/agent/chat', request);
    return response as unknown as AgentChatResponse;
  },

  /**
   * 获取Agent列表
   */
  listAgents: async (): Promise<AgentInfo[]> => {
    const response = await api.get('/agent/list');
    return response as unknown as AgentInfo[];
  },

  /**
   * 清除会话
   */
  clearSession: async (sessionId: string): Promise<void> => {
    await api.delete(`/agent/session/${sessionId}`);
  },
};

export default agentApi;
