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
// 后端 AiController 端点: /api/ai/agents、/api/ai/agent/chat、/api/ai/session/{sessionId}
// api.ts 的 axios baseURL 为 /api,故此处路径为 /ai/...
export const agentApi = {
  /**
   * Agent对话
   */
  chat: async (request: AgentChatRequest): Promise<AgentChatResponse> => {
    const response = await api.post('/ai/agent/chat', request);
    return response as unknown as AgentChatResponse;
  },

  /**
   * 获取Agent列表
   */
  listAgents: async (): Promise<AgentInfo[]> => {
    const response = await api.get('/ai/agents');
    return response as unknown as AgentInfo[];
  },

  /**
   * 清除会话
   */
  clearSession: async (sessionId: string): Promise<void> => {
    await api.delete(`/ai/session/${sessionId}`);
  },
};

export default agentApi;
