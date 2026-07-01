import axios from 'axios';

const api = axios.create({
  baseURL: '/api/indicator',
  timeout: 30000,
});

// 原子指标接口
export interface AtomicIndicator {
  code: string;
  name: string;
  businessLine: string;
  sourceTable: string;
  sourceField: string;
  filterCondition: string;
  detailTable: string;
  detailDimension: string;
  detailDisplayFields: string;
  detailGroupBy: string;
  unit: string;
  precisionVal: number;
  description: string;
  sortOrder: number;
  status: number;
}

// 派生指标接口
export interface DerivedIndicator {
  code: string;
  name: string;
  businessLine: string;
  calcFormula: string;
  formulaVars: string;
  unit: string;
  precisionVal: number;
  description: string;
  sortOrder: number;
  status: number;
}

// 指标值接口
export interface IndicatorValue {
  code: string;
  calcPeriod: string;
  periodValue: string;
  value: number | null;
  calcTime: string;
}

// 指标明细接口
export interface IndicatorDetail {
  code: string;
  name: string;
  calcPeriod: string;
  periodValue: string;
  page: number;
  size: number;
  details: Record<string, any>[];
}

/**
 * 获取所有原子指标列表
 */
export const getAtomicIndicators = async (): Promise<AtomicIndicator[]> => {
  const response = await api.get('/atomic');
  return response.data;
};

/**
 * 获取所有派生指标列表
 */
export const getDerivedIndicators = async (): Promise<DerivedIndicator[]> => {
  const response = await api.get('/derived');
  return response.data;
};

/**
 * 获取指标值
 */
export const getIndicatorValue = async (
  code: string,
  period: string,
  periodValue: string
): Promise<IndicatorValue> => {
  const response = await api.get(`/value/${code}`, {
    params: { period, periodValue },
  });
  return response.data;
};

/**
 * 获取指标趋势
 */
export const getIndicatorTrend = async (
  code: string,
  months: number = 12
): Promise<Record<string, any>[]> => {
  const response = await api.get(`/trend/${code}`, {
    params: { months },
  });
  return response.data;
};

/**
 * 获取指标明细（分页）
 */
export const getIndicatorDetail = async (
  code: string,
  period: string,
  periodValue: string,
  page: number = 1,
  size: number = 50
): Promise<IndicatorDetail> => {
  const response = await api.get(`/detail/${code}`, {
    params: { period, periodValue, page, size },
  });
  return response.data;
};

/**
 * 按分组获取明细
 */
export const getIndicatorDetailByGroup = async (
  code: string,
  groupValue: string,
  period: string,
  periodValue: string
): Promise<IndicatorDetail> => {
  const response = await api.get(`/detail/${code}/group/${groupValue}`, {
    params: { period, periodValue },
  });
  return response.data;
};

/**
 * 手动触发计算
 */
export const calcIndicators = async (
  period: string,
  periodValue: string
): Promise<Record<string, any>[]> => {
  const response = await api.post('/calc-new', null, {
    params: { period, periodValue },
  });
  return response.data;
};

// ============================================
// 新增：指标汇总API
// ============================================

/**
 * 获取指标汇总数据
 */
export const getIndicatorSummary = async (
  businessLine: string,
  period: string,
  statType: string = 'MONTHLY_DAILY_AVG'
): Promise<Record<string, any>[]> => {
  const response = await api.get('/summary', {
    params: { businessLine, period, statType },
  });
  return response.data.data;
};

/**
 * 获取费用类型列表
 */
export const getCostTypes = async (
  businessLine: string
): Promise<Record<string, any>[]> => {
  const response = await api.get('/cost-types', {
    params: { businessLine },
  });
  return response.data.data;
};

/**
 * 获取费用分摊结果
 */
export const getCostAllocationResult = async (
  costType: string,
  period: string,
  page: number = 1,
  size: number = 20
): Promise<Record<string, any>> => {
  const response = await api.get(`/cost-allocation/${costType}`, {
    params: { period, page, size },
  });
  return response.data.data;
};

/**
 * 获取费用原始数据
 */
export const getCostOriginalData = async (
  costType: string,
  period: string,
  dimType: string
): Promise<Record<string, any>> => {
  const response = await api.get(`/cost-original/${costType}`, {
    params: { period, dimType },
  });
  return response.data.data;
};

/**
 * 获取指标定义列表
 */
export const getIndicatorDefinitions = async (
  indicatorType?: string,
  businessLine?: string
): Promise<Record<string, any>[]> => {
  const response = await api.get('/definitions', {
    params: { indicatorType, businessLine },
  });
  return response.data.data;
};

/**
 * 触发指标预计算
 */
export const calculateIndicators = async (
  period: string,
  indicatorCode?: string
): Promise<Record<string, any>> => {
  const response = await api.post('/calculate', null, {
    params: { period, indicatorCode },
  });
  return response.data;
};

/**
 * 执行费用分摊
 */
export const executeAllocation = async (
  period: string,
  costType?: string
): Promise<Record<string, any>> => {
  const response = await api.post('/execute-allocation', null, {
    params: { period, costType },
  });
  return response.data.data;
};

/**
 * 获取分摊配置
 */
export const getAllocationConfig = async (
  costType: string
): Promise<Record<string, any>[]> => {
  const response = await api.get(`/allocation-config/${costType}`);
  return response.data.data;
};

/**
 * 更新分摊配置
 */
export const updateAllocationConfig = async (
  id: number,
  data: Record<string, any>
): Promise<Record<string, any>> => {
  const response = await api.put(`/allocation-config/${id}`, data);
  return response.data.data;
};

// ============================================
// 新增：指标明细API
// ============================================

/**
 * 获取贷款指标汇总数据
 */
export const getLoanIndicatorSummary = async (
  period: string,
  caliberType: string = 'BOOK'
): Promise<Record<string, any>> => {
  const response = await api.get('/loan/summary', {
    params: { period, caliberType },
  });
  return response.data.data;
};

/**
 * 获取存款指标汇总数据
 */
export const getDepositIndicatorSummary = async (
  period: string,
  caliberType: string = 'BOOK'
): Promise<Record<string, any>> => {
  const response = await api.get('/deposit/summary', {
    params: { period, caliberType },
  });
  return response.data.data;
};

/**
 * 获取贷款指标明细列表
 */
export const getLoanIndicatorDetailList = async (
  period: string,
  caliberType: string = 'BOOK',
  dimension?: string,
  dimensionValue?: string,
  page: number = 1,
  pageSize: number = 20
): Promise<Record<string, any>> => {
  const response = await api.get('/loan/detail', {
    params: { period, caliberType, dimension, dimensionValue, page, pageSize },
  });
  return response.data.data;
};

/**
 * 获取存款指标明细列表
 */
export const getDepositIndicatorDetailList = async (
  period: string,
  caliberType: string = 'BOOK',
  dimension?: string,
  dimensionValue?: string,
  page: number = 1,
  pageSize: number = 20
): Promise<Record<string, any>> => {
  const response = await api.get('/deposit/detail', {
    params: { period, caliberType, dimension, dimensionValue, page, pageSize },
  });
  return response.data.data;
};

/**
 * 按维度汇总贷款指标
 */
export const getLoanIndicatorByDimension = async (
  period: string,
  caliberType: string = 'BOOK',
  dimension: string = 'ORG'
): Promise<Record<string, any>[]> => {
  const response = await api.get('/loan/dimension', {
    params: { period, caliberType, dimension },
  });
  return response.data.data;
};

/**
 * 按维度汇总存款指标
 */
export const getDepositIndicatorByDimension = async (
  period: string,
  caliberType: string = 'BOOK',
  dimension: string = 'ORG'
): Promise<Record<string, any>[]> => {
  const response = await api.get('/deposit/dimension', {
    params: { period, caliberType, dimension },
  });
  return response.data.data;
};

/**
 * 获取贷款指标趋势
 */
export const getLoanIndicatorTrend = async (
  months: number = 6,
  caliberType: string = 'BOOK'
): Promise<Record<string, any>[]> => {
  const response = await api.get('/loan/trend', {
    params: { months, caliberType },
  });
  return response.data.data;
};

/**
 * 获取存款指标趋势
 */
export const getDepositIndicatorTrend = async (
  months: number = 6,
  caliberType: string = 'BOOK'
): Promise<Record<string, any>[]> => {
  const response = await api.get('/deposit/trend', {
    params: { months, caliberType },
  });
  return response.data.data;
};
