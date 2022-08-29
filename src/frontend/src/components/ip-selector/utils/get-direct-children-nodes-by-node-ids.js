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
