/**
 * @desc 生成拓扑节点的key
 * @param { Object } node
 * @returns { string }
 */
export const genNodeKey = node => `#${node.objectId}#${node.instanceId}`;
