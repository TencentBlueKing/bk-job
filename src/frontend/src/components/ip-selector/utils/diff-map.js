import { genNodeKey } from './gen-node-key';

export const getHostDiffMap = (lastHostList, originalValue = {}, invalidList = []) => {
    const {
        hostList: originalHostList = [],
    } = originalValue;
    
    const diffMap = {};
    const lastHostIPMemo = {};

    originalHostList.forEach((item) => {
        diffMap[item.hostId] = 'remove';
    });
    
    lastHostList.forEach((item) => {
        if (diffMap[item.hostId]) {
            diffMap[item.hostId] = '';
        } else {
            diffMap[item.hostId] = 'new';
            if (item.ip) {
                if (lastHostIPMemo[item.ip]) {
                    diffMap[item.hostId] = 'repeat';
                } else {
                    lastHostIPMemo[item.ip] = true;
                }
            }
        }
    });

    invalidList.forEach((item) => {
        diffMap[item.hostId] = 'invalid';
    });
    return diffMap;
};

export const getNodeDiffMap = (lastNodeList, originalValue = {}, invalidList = []) => {
    const {
        nodeList: originalNodeList = [],
    } = originalValue;

    const diffMap = lastNodeList.reduce((result, item) => ({
        ...result,
        [genNodeKey(item)]: 'new',
    }), {});

    originalNodeList.forEach((item) => {
        const nodeKey = genNodeKey(item);
        if (diffMap[nodeKey]) {
            diffMap[nodeKey] = '';
        } else {
            diffMap[nodeKey] = 'remove';
        }
    });
    invalidList.forEach((item) => {
        diffMap[item.hostId] = 'invalid';
    });
    return diffMap;
};

export const getDynamicGroupDiffMap = (lastDynamicGroupList, originalValue = {}, invalidList = []) => {
    const {
        dynamicList: originalDynamicGroupList = [],
    } = originalValue;
    const diffMap = lastDynamicGroupList.reduce((result, item) => ({
        ...result,
        [item.id]: 'new',
    }), {});

    originalDynamicGroupList.forEach((item) => {
        if (diffMap[item.id]) {
            diffMap[item.id] = '';
        } else {
            diffMap[item.id] = 'remove';
        }
    });
    invalidList.forEach((item) => {
        diffMap[item.hostId] = 'invalid';
    });
    return diffMap;
};
