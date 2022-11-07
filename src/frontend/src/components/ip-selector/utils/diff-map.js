import { genNodeKey } from './gen-node-key';

export const getHostDiffMap = (lastHostList, originalValue, invalidList = []) => {
    const isShowOriginalValueDiff = Boolean(originalValue);
    const {
        host_list: originalHostList = [],
    } = originalValue || {};
    
    const diffMap = {};
    const lastHostIPMemo = {};

    originalHostList.forEach((item) => {
        diffMap[item.host_id] = isShowOriginalValueDiff ? 'remove' : '';
    });
    
    lastHostList.forEach((item) => {
        if (diffMap[item.host_id]) {
            diffMap[item.host_id] = '';
        } else {
            diffMap[item.host_id] = isShowOriginalValueDiff ? 'new' : '';
            if (item.ip) {
                if (lastHostIPMemo[item.ip]) {
                    diffMap[item.host_id] = 'repeat';
                } else {
                    lastHostIPMemo[item.ip] = true;
                }
            }
        }
    });

    invalidList.forEach((item) => {
        diffMap[item.host_id] = 'invalid';
    });

    return diffMap;
};

export const getNodeDiffMap = (lastNodeList, originalValue, invalidList = []) => {
    const isShowOriginalValueDiff = Boolean(originalValue);

    const {
        node_list: originalNodeList = [],
    } = originalValue || {};

    const diffMap = lastNodeList.reduce((result, item) => ({
        ...result,
        [genNodeKey(item)]: isShowOriginalValueDiff ? 'new' : '',
    }), {});

    originalNodeList.forEach((item) => {
        const nodeKey = genNodeKey(item);
        if (diffMap[nodeKey]) {
            diffMap[nodeKey] = '';
        } else {
            diffMap[nodeKey] = isShowOriginalValueDiff ? 'remove' : '';
        }
    });
    invalidList.forEach((item) => {
        diffMap[genNodeKey(item)] = 'invalid';
    });
    return diffMap;
};

export const getDynamicGroupDiffMap = (lastDynamicGroupList, originalValue, invalidList = []) => {
    const isShowOriginalValueDiff = Boolean(originalValue);

    const {
        dynamic_group_list: originalDynamicGroupList = [],
    } = originalValue || {};

    const diffMap = lastDynamicGroupList.reduce((result, item) => ({
        ...result,
        [item.id]: isShowOriginalValueDiff ? 'new' : '',
    }), {});

    originalDynamicGroupList.forEach((item) => {
        if (diffMap[item.id]) {
            diffMap[item.id] = '';
        } else {
            diffMap[item.id] = isShowOriginalValueDiff ? 'remove' : '';
        }
    });

    invalidList.forEach((item) => {
        diffMap[item.id] = 'invalid';
    });
    return diffMap;
};
