/**
 * @desc 转换拓扑数据适配组件big-tree
 * @param { Array } target
 * @param { Number } level
 * @returns { Array }
 */
export const transformTopoTree = (target, level = 0) => {
    if (!target || target.length < 1) {
        return [];
    }
                
    return target.map((item) => {
        const { instanceId, instanceName, child, objectId } = item;
        
        const children = transformTopoTree(child, level + 1);

        return Object.freeze({
            id: `#${objectId}#${instanceId}`,
            name: instanceName,
            level,
            children,
            payload: item,
        });
    });
};
