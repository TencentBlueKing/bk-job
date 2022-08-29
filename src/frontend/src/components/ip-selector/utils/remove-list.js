import { genNodeKey } from './gen-node-key';

export const getRemoveHostList = (lastHostList, originalValue = {}) => {
    const {
        hostList: originalHostList = [],
    } = originalValue;

    const lastHostMap = lastHostList.reduce((result, item) => ({
        ...result,
        [item.hostId]: true,
    }), {});

    return originalHostList.reduce((result, item) => {
        if (!lastHostMap[item.hostId]) {
            result.push(item);
        }

        return result;
    }, []);
};

export const getRemoveNodeList = (lastNodeList, originalValue = {}) => {
    const {
        nodeList: originalNodeList = [],
    } = originalValue;

    const lastNodeMap = lastNodeList.reduce((result, item) => ({
        ...result,
        [genNodeKey(item)]: true,
    }), {});

    return originalNodeList.reduce((result, item) => {
        if (!lastNodeMap[genNodeKey(item)]) {
            result.push(item);
        }

        return result;
    }, []);
};

export const getRemoveDynamicGroupList = (lastDynamicGroupList, originalValue = {}) => {
    const {
        dynamicList: originalDynamicGroupList = [],
    } = originalValue;

    const lastDynamicGroupMap = lastDynamicGroupList.reduce((result, item) => ({
        ...result,
        [item.id]: true,
    }), {});

    return originalDynamicGroupList.reduce((result, item) => {
        if (!lastDynamicGroupMap[item.id]) {
            result.push(item);
        }

        return result;
    }, []);
};
