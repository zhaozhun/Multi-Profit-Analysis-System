import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
});

api.interceptors.response.use(
  (response) => {
    const { data } = response;
    // 支持两种返回格式: {code: 200, data: ...} 和 {success: true, data: ...}
    if (data.code === 200 || data.success === true) {
      return data.data !== undefined ? data.data : data;
    }
    return Promise.reject(new Error(data.message || '请求失败'));
  },
  (error) => {
    console.error('API Error:', error);
    return Promise.reject(error);
  }
);

// ==================== 驾驶舱 ====================
export const getDashboardData = (period: string, caliberType: string = 'BOOK') =>
  api.get('/dashboard/overview', { params: { period, caliberType } });

export const getWaterfallData = (period: string, caliberType: string = 'BOOK') =>
  api.get('/dashboard/waterfall', { params: { period, caliberType } });

export const getTrendData = (period: string, caliberType: string = 'BOOK') =>
  api.get('/dashboard/trend', { params: { period, caliberType } });

export const getDimOverview = (dimType: string, period: string, caliberType: string = 'BOOK') =>
  api.get(`/dashboard/dim-overview/${dimType}`, { params: { period, caliberType } });

// ==================== 维度分析 ====================
export const getDimensionAnalysis = (dimType: string, period: string, caliberType: string = 'BOOK', parentId?: number, level?: number) =>
  api.get(`/dimension/${dimType}/analysis`, { params: { period, caliberType, parentId, level } });

export const getDimensionTree = (dimType: string, period: string, caliberType: string = 'BOOK', parentId: number = 0) =>
  api.get(`/dimension/${dimType}/tree`, { params: { period, caliberType, parentId } });

export const getDimensionRanking = (dimType: string, period: string, rankBy: string = 'net_profit', limit: number = 10) =>
  api.get(`/dimension/${dimType}/ranking`, { params: { period, rankBy, limit } });

export const getDimensionDetail = (dimType: string, dimId: number, period: string) =>
  api.get(`/dimension/${dimType}/detail/${dimId}`, { params: { period } });

export const crossDrill = (fromDimType: string, fromDimName: string, toDimType: string, period: string, caliberType: string = 'BOOK') =>
  api.get('/dimension/cross-drill', { params: { fromDimType, fromDimName, toDimType, period, caliberType } });

export const getDrillPath = (dimType: string, dimId: number) =>
  api.get(`/dimension/${dimType}/drill-path/${dimId}`);

export const getDimHierarchy = (dimType: string) =>
  api.get(`/dimension/${dimType}/hierarchy`);

// ==================== AI ====================
export const aiChat = (message: string, context?: string) =>
  api.post('/ai/chat', { message, context });

export const generateBrief = (period: string) =>
  api.post('/ai/brief', null, { params: { period } });

export const nl2sql = (question: string) =>
  api.post('/ai/nl2sql', { message: question });

export const exportExcel = (period: string) =>
  api.get('/ai/export/excel', { params: { period }, responseType: 'blob' });

export const aiHealthCheck = () =>
  api.get('/ai/health');

// ==================== 数据校验 ====================
export const detectAnomaly = (period: string, dimType: string = 'ORG') =>
  api.post('/validation/detect', null, { params: { period, dimType } });

// ==================== 费用分摊（重新导出）====================
export {
  factorApi,
  allocationApi,
  algorithmApi,
  costTypeApi,
  commissionConfigApi,
  employeeCostApi,
  operationCostApi,
  allocationAiApi,
} from './allocationApi';

export default api;
