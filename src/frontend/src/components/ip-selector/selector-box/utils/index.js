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

/**
 * @desc 获取指定节点下面的所有节点
 * @param { Array } tree
 * @param { Object } node
 * @returns { Array }
 */
export const getDirectChildrenNodesByNodeIds = (tree, nodeIdList = []) => {
    const nodeIdMap = nodeIdList.reduce((result, item) => ({
        ...result,
        [item]: true,
    }), {});
    const childrenList = [];
    tree.forEach((node) => {
        if (nodeIdMap[node.id]) {
            childrenList.push(...node.children);
        } else {
            getDirectChildrenNodesByNodeIds(node.children);
        }
    });
    return childrenList;
};

export const execCopy = (value, message = '复制成功') => {
    const textarea = document.createElement('textarea');
    document.body.appendChild(textarea);
    textarea.value = value;
    textarea.select();
    if (document.execCommand('copy')) {
        document.execCommand('copy');
        alert(message);
    }
    document.body.removeChild(textarea);
};
