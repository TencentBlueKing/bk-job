export const formatOutput = (data) => {
    const {
        hostList = [],
        nodeList = [],
        dynamicGroupList = [],
    } = data;

    return {
        hostList: hostList.map(item => ({
            hostId: item.hostId,
            ip: item.ip,
            ipv6: item.ipv6,
            meta: item.meta,
        })),
        nodeList: nodeList.map(item => ({
            objectId: item.objectId,
            instanceId: item.instanceId,
            meta: item.meta,
        })),
        dynamicGroupList: dynamicGroupList.map(item => ({
            id: item.id,
            meta: item.meta,
        })),
    };
};
