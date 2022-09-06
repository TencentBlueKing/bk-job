import { genNodeKey } from './gen-node-key';

export const groupHostList = (validHostList, diffMap) => {
    const originalList = [];
    const newList = [];
    validHostList.forEach((hostData) => {
        if (diffMap[hostData.host_id] === 'new') {
            newList.push(hostData);
        } else {
            originalList.push(hostData);
        }
    });

    return {
        newList,
        originalList,
    };
};

export const groupNodeList = (validNodeList, diffMap) => {
    const originalList = [];
    const newList = [];
    validNodeList.forEach((nodeData) => {
        if (diffMap[genNodeKey(nodeData)] === 'new') {
            newList.push(nodeData);
        } else {
            originalList.push(nodeData);
        }
    });

    return {
        newList,
        originalList,
    };
};

export const groupDynamicGroupList = (validDynamicGroupList, diffMap) => {
    const originalList = [];
    const newList = [];
    validDynamicGroupList.forEach((dynamicGroupData) => {
        if (diffMap[dynamicGroupData.id] === 'new') {
            newList.push(dynamicGroupData);
        } else {
            originalList.push(dynamicGroupData);
        }
    });

    return {
        newList,
        originalList,
    };
};
