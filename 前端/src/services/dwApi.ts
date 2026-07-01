import axios from 'axios';

const api = axios.create({
  baseURL: '/api/dw',
  timeout: 30000,
});

/**
 * 获取指标列表
 */
export const getIndicatorList = async (): Promise<Record<string, any>[]> => {
  const response = await api.get('/indicator/list');
  return response.data.data;
};

/**
 * 获取指标汇总
 */
export const getIndicatorSummary = async (
  indicatorCode: string,
  period: string,
  caliberType: string = 'ASSESS'
): Promise<Record<string, any>> => {
  const response = await api.get('/indicator/summary', {
    params: { indicatorCode, period, caliberType },
  });
  return response.data.data;
};

/**
 * 获取指标维度数据
 */
export const getIndicatorDimension = async (
  indicatorCode: string,
  period: string,
  dimType: string,
  caliberType: string = 'ASSESS'
): Promise<Record<string, any>[]> => {
  const response = await api.get('/indicator/dimension', {
    params: { indicatorCode, period, dimType, caliberType },
  });
  return response.data.data;
};

/**
 * 获取指标明细数据
 */
export const getIndicatorDetail = async (
  indicatorCode: string,
  period: string,
  dimType: string,
  dimId: number,
  caliberType: string = 'ASSESS'
): Promise<Record<string, any>> => {
  const response = await api.get('/indicator/detail', {
    params: { indicatorCode, period, dimType, dimId, caliberType },
  });
  return response.data.data;
};

/**
 * 获取指标趋势
 */
export const getIndicatorTrend = async (
  indicatorCode: string,
  months: number = 6,
  caliberType: string = 'ASSESS'
): Promise<Record<string, any>[]> => {
  const response = await api.get('/indicator/trend', {
    params: { indicatorCode, months, caliberType },
  });
  return response.data.data;
};

/**
 * 执行ETL
 */
export const executeETL = async (period: string): Promise<Record<string, any>> => {
  const response = await api.post('/etl/execute', null, {
    params: { period },
  });
  return response.data.data;
};
