import React, { useState, useEffect } from 'react';
import { Card, Tree, Spin, message } from 'antd';
import { getIndicatorDefinitions } from '../../../services/indicatorApi';
import IndicatorDetail from './IndicatorDetail';

const { DirectoryTree } = Tree;

interface IndicatorNode {
  key: string;
  title: string;
  isLeaf?: boolean;
  children?: IndicatorNode[];
  data?: any;
}

const IndicatorLibrary: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [treeData, setTreeData] = useState<IndicatorNode[]>([]);
  const [selectedIndicator, setSelectedIndicator] = useState<any>(null);

  useEffect(() => {
    loadIndicators();
  }, []);

  const loadIndicators = async () => {
    setLoading(true);
    try {
      const indicators = await getIndicatorDefinitions();

      // 按业务线分组
      const assetIndicators = indicators.filter((i: any) => i.business_line === 'ASSET');
      const liabilityIndicators = indicators.filter((i: any) => i.business_line === 'LIABILITY');
      const allIndicators = indicators.filter((i: any) => i.business_line === 'ALL');

      // 按指标类型分组
      const groupByType = (items: any[]) => {
        const groups: Record<string, any[]> = {};
        items.forEach(item => {
          const type = item.indicator_type || 'OTHER';
          if (!groups[type]) groups[type] = [];
          groups[type].push(item);
        });
        return groups;
      };

      const buildTree = (businessLine: string, indicators: any[]): IndicatorNode => {
        const groups = groupByType(indicators);
        const children: IndicatorNode[] = Object.entries(groups).map(([type, items]) => ({
          key: `${businessLine}-${type}`,
          title: getTypeName(type),
          children: items.map(item => ({
            key: item.indicator_code,
            title: item.indicator_name,
            isLeaf: true,
            data: item,
          })),
        }));

        return {
          key: businessLine,
          title: getBusinessLineName(businessLine),
          children,
        };
      };

      const tree: IndicatorNode[] = [
        {
          key: 'root',
          title: '指标库',
          children: [
            buildTree('ASSET', assetIndicators),
            buildTree('LIABILITY', liabilityIndicators),
            buildTree('ALL', allIndicators),
          ],
        },
      ];

      setTreeData(tree);
    } catch (error) {
      console.error('加载指标失败:', error);
      message.error('加载指标失败');
    } finally {
      setLoading(false);
    }
  };

  const getBusinessLineName = (code: string) => {
    const map: Record<string, string> = {
      ASSET: '资产类指标',
      LIABILITY: '负债类指标',
      ALL: '全部指标',
    };
    return map[code] || code;
  };

  const getTypeName = (code: string) => {
    const map: Record<string, string> = {
      SCALE: '规模类',
      REVENUE: '收入类',
      COST: '成本类',
      PROFIT: '利润类',
      EFFICIENCY: '效率类',
      DAILY_AVG: '日均类',
    };
    return map[code] || code;
  };

  const handleSelect = (keys: any[], info: any) => {
    const node = info.node;
    if (node.isLeaf && node.data) {
      setSelectedIndicator(node.data);
    }
  };

  return (
    <div style={{ display: 'flex', height: '100%' }}>
      <Card
        title="指标库"
        style={{ width: 300, marginRight: 16 }}
        bodyStyle={{ padding: 0 }}
      >
        <Spin spinning={loading}>
          <DirectoryTree
            treeData={treeData}
            onSelect={handleSelect}
            defaultExpandAll
          />
        </Spin>
      </Card>
      <Card
        title="指标详情"
        style={{ flex: 1 }}
      >
        {selectedIndicator ? (
          <IndicatorDetail indicator={selectedIndicator} />
        ) : (
          <div style={{ textAlign: 'center', padding: 50, color: '#999' }}>
            请选择一个指标查看详情
          </div>
        )}
      </Card>
    </div>
  );
};

export default IndicatorLibrary;
