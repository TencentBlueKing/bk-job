import Manager from '../manager';

export const formatInput = data => ({
    host_list: (data[Manager.nameStyle('hostList')] || []).map(item => ({
        host_id: item[Manager.nameStyle('hostId')],
        ip: item[Manager.nameStyle('ip')],
        ipv6: item[Manager.nameStyle('ipv6')],
        meta: item[Manager.nameStyle('meta')],
    })),
    node_list: (data[Manager.nameStyle('nodeList')] || []).map(item => ({
        object_id: item[Manager.nameStyle('objectId')],
        instance_id: item[Manager.nameStyle('instanceId')],
        meta: item[Manager.nameStyle('meta')],
    })),
    dynamic_group_list: (data[Manager.nameStyle('dynamicGroupList')] || []).map(item => ({
        id: item[Manager.nameStyle('id')],
        name: item[Manager.nameStyle('name')],
    })),
});

export const formatOutput = data => ({
    [Manager.nameStyle('hostList')]: (data.hostList || []).map(item => ({
        [Manager.nameStyle('hostId')]: item.host_id,
        [Manager.nameStyle('ip')]: item.ip,
        [Manager.nameStyle('ipv6')]: item.ipv6,
        [Manager.nameStyle('meta')]: item.meta,
    })),
    [Manager.nameStyle('nodeList')]: (data.nodeList || []).map(item => ({
        [Manager.nameStyle('objectId')]: item.object_id,
        [Manager.nameStyle('instanceId')]: item.instance_id,
        [Manager.nameStyle('meta')]: item.meta,
    })),
    [Manager.nameStyle('dynamicGroupList')]: (data.dynamicGroupList || []).map(item => ({
        [Manager.nameStyle('id')]: item.id,
        [Manager.nameStyle('meta')]: item.meta,
    })),
});
