
export const topologyHostCount = (data = []) => data.map(item => ({
    object_id: item.objectId,
    object_name: item.objectName,
    instance_id: item.instanceId,
    instance_name: item.instanceName,
    count: item.count,
    lazy: item.lazy,
    expanded: item.expanded,
    child: topologyHostCount(item.child),
}));
