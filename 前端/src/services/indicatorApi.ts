import axios from 'axios';

const api = axios.create({
  baseURL: '/api/indicator',
  timeout: 10000,
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
