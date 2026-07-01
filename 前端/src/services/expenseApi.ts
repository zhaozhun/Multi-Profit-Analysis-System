import axios from 'axios';

const api = axios.create({
  baseURL: '/api/expense',
  timeout: 30000,
});

/**
 * 获取运营成本汇总
 */
export const getExpenseSummary = async (
  period: string,
  caliberType: string = 'ASSESS',
  dimension: string = 'ORG'
): Promise<Record<string, any>> => {
  const response = await api.get('/summary', {
    params: { period, caliberType, dimension },
  });
  return response.data.data;
};

/**
 * 获取业务费用组成
 */
export const getBizExpenseComposition = async (
  period: string,
  bizId: string
): Promise<Record<string, any>[]> => {
  const response = await api.get('/biz-composition', {
    params: { period, bizId },
  });
  return response.data.data;
};

/**
 * 获取费用原始数据
 */
export const getExpenseOriginalData = async (
  period: string,
  expenseType: string
): Promise<Record<string, any>[]> => {
  const response = await api.get('/original', {
    params: { period, expenseType },
  });
  return response.data.data;
};

/**
 * 获取分摊因子列表
 */
export const getAllocationFactors = async (): Promise<Record<string, any>[]> => {
  const response = await api.get('/factors');
  return response.data.data;
};

/**
 * 获取分摊规则列表
 */
export const getAllocationRules = async (
  expenseType?: string
): Promise<Record<string, any>[]> => {
  const response = await api.get('/rules', {
    params: { expenseType },
  });
  return response.data.data;
};

/**
 * 保存分摊规则
 */
export const saveAllocationRule = async (
  rule: Record<string, any>
): Promise<Record<string, any>> => {
  const response = await api.post('/rules', rule);
  return response.data.data;
};

/**
 * 执行分摊
 */
export const executeAllocation = async (
  period: string,
  expenseType?: string
): Promise<Record<string, any>> => {
  const response = await api.post('/execute', null, {
    params: { period, expenseType },
  });
  return response.data.data;
};
