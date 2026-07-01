import React, { useEffect, useRef } from 'react';
import * as echarts from 'echarts';

interface IndicatorRelationGraphProps {
  indicatorCode: string;
}

const IndicatorRelationGraph: React.FC<IndicatorRelationGraphProps> = ({ indicatorCode }) => {
  const chartRef = useRef<HTMLDivElement>(null);
  const chartInstance = useRef<echarts.ECharts | null>(null);

  useEffect(() => {
    if (chartRef.current) {
      chartInstance.current = echarts.init(chartRef.current);
      renderGraph();
    }

    return () => {
      chartInstance.current?.dispose();
    };
  }, [indicatorCode]);

  const renderGraph = () => {
    // 指标关联关系数据
    const relations: Record<string, { parents: string[]; children: string[] }> = {
      TOTAL_PROFIT: { parents: [], children: ['LOAN_PROFIT', 'DEPOSIT_PROFIT'] },
      LOAN_PROFIT: { parents: ['TOTAL_PROFIT'], children: ['INTEREST_INCOME', 'FTP_COST', 'RISK_COST', 'OP_COST'] },
      DEPOSIT_PROFIT: { parents: ['TOTAL_PROFIT'], children: ['FTP_INCOME', 'DEPOSIT_INTEREST', 'OP_COST'] },
      INTEREST_INCOME: { parents: ['LOAN_PROFIT'], children: [] },
      FTP_COST: { parents: ['LOAN_PROFIT'], children: [] },
      RISK_COST: { parents: ['LOAN_PROFIT'], children: [] },
      OP_COST: { parents: ['LOAN_PROFIT', 'DEPOSIT_PROFIT'], children: [] },
      FTP_INCOME: { parents: ['DEPOSIT_PROFIT'], children: [] },
      DEPOSIT_INTEREST: { parents: ['DEPOSIT_PROFIT'], children: [] },
    };

    const indicatorNames: Record<string, string> = {
      TOTAL_PROFIT: '总利润',
      LOAN_PROFIT: '贷款利润',
      DEPOSIT_PROFIT: '存款利润',
      INTEREST_INCOME: '利息收入',
      FTP_COST: 'FTP成本',
      RISK_COST: '风险成本',
      OP_COST: '运营成本',
      FTP_INCOME: 'FTP收入',
      DEPOSIT_INTEREST: '利息支出',
    };

    const relation = relations[indicatorCode];
    if (!relation) return;

    // 构建节点和边
    const nodes: any[] = [];
    const links: any[] = [];
    const addedNodes = new Set<string>();

    const addNode = (code: string, level: number) => {
      if (addedNodes.has(code)) return;
      addedNodes.add(code);
      nodes.push({
        name: indicatorNames[code] || code,
        x: level * 200,
        y: nodes.length * 100,
        itemStyle: {
          color: code === indicatorCode ? '#1890ff' : '#91d5ff',
        },
      });
    };

    const addLink = (source: string, target: string) => {
      links.push({
        source: indicatorNames[source] || source,
        target: indicatorNames[target] || target,
      });
    };

    // 添加当前节点
    addNode(indicatorCode, 1);

    // 添加父节点
    relation.parents.forEach((parent, index) => {
      addNode(parent, 0);
      addLink(parent, indicatorCode);
    });

    // 添加子节点
    relation.children.forEach((child, index) => {
      addNode(child, 2);
      addLink(indicatorCode, child);
    });

    const option = {
      tooltip: {},
      series: [
        {
          type: 'graph',
          layout: 'none',
          symbolSize: 50,
          label: {
            show: true,
          },
          edgeSymbol: ['circle', 'arrow'],
          edgeSymbolSize: [4, 10],
          data: nodes,
          links: links,
          lineStyle: {
            opacity: 0.9,
            width: 2,
            curveness: 0,
          },
        },
      ],
    };

    chartInstance.current?.setOption(option);
  };

  return <div ref={chartRef} style={{ width: '100%', height: 400 }} />;
};

export default IndicatorRelationGraph;
