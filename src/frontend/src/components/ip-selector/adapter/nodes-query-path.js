
export const topologyQueryPath = (data = []) => data.map(nodeStack => nodeStack.map(nodeData => ({
    object_id: nodeData.objectId,
    object_name: nodeData.objectName,
    instance_id: nodeData.instanceId,
    instance_name: nodeData.instanceName,
    count: nodeData.count,
    lazy: nodeData.lazy,
})));
