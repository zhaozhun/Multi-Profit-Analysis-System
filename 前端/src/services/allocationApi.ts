import api from './api';

// ==================== 费用类型管理 API ====================

export const costTypeApi = {
  getAllCostTypes: () => api.get('/cost-type/list'),
  getCostTypeHierarchy: () => api.get('/cost-type/hierarchy'),
  getCostTypesByLevel: (level: number) => api.get(`/cost-type/level/${level}`),
  getCostTypesByParent: (parentCode: string) => api.get(`/cost-type/parent/${parentCode}`),
  getAllocationRequiredTypes: () => api.get('/cost-type/allocation-required'),
  getCostTypesByNature: (costNature: string) => api.get(`/cost-type/nature/${costNature}`),
  getCostTypesByCategory: (costCategory: string) => api.get(`/cost-type/category/${costCategory}`),
  getCostType: (costCode: string) => api.get(`/cost-type/${costCode}`),
  createCostType: (data: any) => api.post('/cost-type', data),
  updateCostType: (costCode: string, data: any) => api.put(`/cost-type/${costCode}`, data),
  deleteCostType: (costCode: string) => api.delete(`/cost-type/${costCode}`),
  getAllocationRules: () => api.get('/cost-type/allocation-rule/list'),
  getAllocationRule: (costCode: string) => api.get(`/cost-type/allocation-rule/${costCode}`),
  updateAllocationRule: (costCode: string, data: any) => api.put(`/cost-type/allocation-rule/${costCode}`, data),
  createActualRecord: (data: any) => api.post('/cost-type/actual-record', data),
  getActualRecords: (period: string, costCode?: string, costType?: string) => {
    let url = `/cost-type/actual-record?period=${period}`;
    if (costCode) url += `&costCode=${costCode}`;
    if (costType) url += `&costType=${costType}`;
    return api.get(url);
  },
  getCostSummary: (period: string) => api.get(`/cost-type/summary?period=${period}`),
  getSummaryByNature: (period: string) => api.get(`/cost-type/summary/nature?period=${period}`),
  getSummaryByCategory: (period: string) => api.get(`/cost-type/summary/category?period=${period}`),
};

// ==================== 员工费用分摊 API ====================

export const employeeCostApi = {
  executeAllocation: (period: string, costType: string, factorType: string) =>
    api.post('/allocation/employee/execute', { period, costType, factorType }),
  getAllocations: (period: string, costType?: string, employeeCode?: string) => {
    let url = `/allocation/employee/result?period=${period}`;
    if (costType) url += `&costType=${costType}`;
    if (employeeCode) url += `&employeeCode=${employeeCode}`;
    return api.get(url);
  },
  getEmployeeSummary: (period: string) => api.get(`/allocation/employee/summary/employee?period=${period}`),
  getDeptSummary: (period: string) => api.get(`/allocation/employee/summary/dept?period=${period}`),
};

// ==================== 产品分润配置 API ====================

export const commissionConfigApi = {
  getAllConfigs: () => api.get('/allocation/commission-config/list'),
  getCommissionConfigs: () => api.get('/allocation/commission-config/commission'),
  getNonCommissionConfigs: () => api.get('/allocation/commission-config/non-commission'),
  updateConfig: (productCode: string, data: any) => api.put(`/allocation/commission-config/${productCode}`, data),
  calculateCommission: (period: string) => api.post('/allocation/commission-config/calculate', { period }),
  previewCommission: (period: string) => api.get(`/allocation/commission-config/preview?period=${period}`),
};

// ==================== 运营费用分摊 API ====================

export const operationCostApi = {
  executeAllocation: (period: string, costType?: string) =>
    api.post('/allocation/operation-cost/execute', { period, costType }),
  getAllocationResults: (period: string, costType?: string) => {
    let url = `/allocation/operation-cost/result?period=${period}`;
    if (costType) url += `&costType=${costType}`;
    return api.get(url);
  },
  getSummaryByCostType: (period: string) => api.get(`/allocation/operation-cost/summary/cost-type?period=${period}`),
  getSummaryByEmployee: (period: string) => api.get(`/allocation/operation-cost/summary/employee?period=${period}`),
};

// ==================== 分摊因子管理 API ====================

export const factorApi = {
  getFactors: (factorType?: string, costType?: string) => {
    let url = '/allocation/factor';
    const params = new URLSearchParams();
    if (factorType) params.append('factorType', factorType);
    if (costType) params.append('costType', costType);
    if (params.toString()) url += '?' + params.toString();
    return api.get(url);
  },
  getFactor: (code: string) => api.get(`/allocation/factor/${code}`),
  createFactor: (data: any) => api.post('/allocation/factor', data),
  updateFactor: (code: string, data: any) => api.put(`/allocation/factor/${code}`, data),
  deleteFactor: (code: string) => api.delete(`/allocation/factor/${code}`),
  calculateFactor: (code: string, period: string, dimType: string) =>
    api.post(`/allocation/factor/${code}/calc`, { period, dimType }),
};

// ==================== 分摊规则管理 API ====================

export const allocationApi = {
  getRules: (costType?: string, status?: string) => {
    let url = '/allocation/rule';
    const params = new URLSearchParams();
    if (costType) params.append('costType', costType);
    if (status) params.append('status', status);
    if (params.toString()) url += '?' + params.toString();
    return api.get(url);
  },
  getFactors: factorApi.getFactors,
  createFactor: factorApi.createFactor,
  updateFactor: factorApi.updateFactor,
  deleteFactor: factorApi.deleteFactor,
  getAlgorithms: (algorithmType?: string) => {
    let url = '/allocation/algorithm';
    if (algorithmType) url += `?algorithmType=${algorithmType}`;
    return api.get(url);
  },
  createAlgorithm: (data: any) => api.post('/allocation/algorithm', data),
  updateAlgorithm: (code: string, data: any) => api.put(`/allocation/algorithm/${code}`, data),
  deleteAlgorithm: (code: string) => api.delete(`/allocation/algorithm/${code}`),
  getRule: (id: number) => api.get(`/allocation/rule/${id}`),
  createRule: (data: any) => api.post('/allocation/rule', data),
  updateRule: (id: number, data: any) => api.put(`/allocation/rule/${id}`, data),
  deleteRule: (id: number) => api.delete(`/allocation/rule/${id}`),
  enableRule: (id: number) => api.post(`/allocation/rule/${id}/enable`),
  disableRule: (id: number) => api.post(`/allocation/rule/${id}/disable`),
  executeAllocation: (data: any) => api.post('/allocation/execute', data),
  previewAllocation: (data: any) => api.post('/allocation/preview', data),
  getResults: (period: string, costType?: string) => {
    let url = `/allocation/result?period=${period}`;
    if (costType) url += `&costType=${costType}`;
    return api.get(url);
  },
  getResultDetail: (batchId: number) => api.get(`/allocation/result/${batchId}`),
  getAllocationSummary: (batchId: number, targetDimType?: string) => {
    let url = `/allocation/result/${batchId}/summary`;
    if (targetDimType) url += `?targetDimType=${targetDimType}`;
    return api.get(url);
  },
};

// ==================== 分摊算法管理 API ====================

export const algorithmApi = {
  getAlgorithms: (algorithmType?: string) => {
    let url = '/allocation/algorithm';
    if (algorithmType) url += `?algorithmType=${algorithmType}`;
    return api.get(url);
  },
  getAlgorithm: (code: string) => api.get(`/allocation/algorithm/${code}`),
  createAlgorithm: (data: any) => api.post('/allocation/algorithm', data),
  updateAlgorithm: (code: string, data: any) => api.put(`/allocation/algorithm/${code}`, data),
  deleteAlgorithm: (code: string) => api.delete(`/allocation/algorithm/${code}`),
  testAlgorithm: (data: any) => api.post('/allocation/algorithm/test', data),
};

// ==================== AI相关 API ====================

export const allocationAiApi = {
  chat: (message: string, context?: string) => api.post('/allocation/ai/chat', { message, context }),
  analyzeAllocation: (period: string, dimType?: string, costType?: string) =>
    api.post('/allocation/ai/analyze', { period, dimType, costType }),
  suggestRule: (costType: string, costAmount?: number, businessContext?: string) =>
    api.post('/allocation/ai/suggest', { costType, costAmount, businessContext }),
  diagnoseRules: (period: string) => api.post('/allocation/ai/diagnose', { period }),
  generateBrief: (period: string) => api.post('/allocation/ai/brief', { period }),
};
