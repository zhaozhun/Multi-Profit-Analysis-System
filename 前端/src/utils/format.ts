/**
 * 格式化数值显示
 * 根据数值大小自动选择单位：元、万元、亿元
 */
export function formatValue(value: number): { text: string; unit: string } {
    const absValue = Math.abs(value);

    if (absValue >= 100000000) {
        // >= 1亿：显示为亿元
        return {
            text: (value / 100000000).toFixed(2),
            unit: '亿元'
        };
    } else if (absValue >= 10000) {
        // >= 1万：显示为万元
        return {
            text: (value / 10000).toFixed(2),
            unit: '万元'
        };
    } else {
        // < 1万：显示为元
        return {
            text: value.toFixed(2),
            unit: '元'
        };
    }
}

/**
 * 格式化数值为带单位的字符串
 */
export function formatValueWithUnit(value: number): string {
    const { text, unit } = formatValue(value);
    return `${text} ${unit}`;
}
