import { genNodeKey } from './gen-node-key';

export const getRemoveHostList = (lastHostList, originalValue) => {
    if (!originalValue) {
        return [];
    }
    const {
        host_list: originalHostList = [],
    } = originalValue;

    const lastHostMap = lastHostList.reduce((result, item) => ({
        ...result,
        [item.host_id]: true,
    }), {});

    return originalHostList.reduce((result, item) => {
        if (!lastHostMap[item.host_id]) {
            result.push(item);
        }

        return result;
    }, []);
};

export const getRemoveNodeList = (lastNodeList, originalValue) => {
    if (!originalValue) {
        return [];
    }
    const {
        node_list: originalNodeList = [],
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

export const getRemoveDynamicGroupList = (lastDynamicGroupList, originalValue) => {
    if (!originalValue) {
        return [];
    }
    const {
        dynamic_group_list: originalDynamicGroupList = [],
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
