import { genNodeKey } from './gen-node-key';

export const getInvalidHostList = (lastHostList, validHostList) => {
    const validHostMap = validHostList.reduce((result, item) => ({
        ...result,
        [item.host_id]: true,
    }), {});

    return lastHostList.reduce((result, item) => {
        if (!validHostMap[item.host_id]) {
            result.push(item);
        }

        return result;
    }, []);
};

export const getInvalidNodeList = (lastNodeList, validNodeList) => {
    const validNodeMap = validNodeList.reduce((result, item) => ({
        ...result,
        [genNodeKey(item)]: true,
    }), {});

    return lastNodeList.reduce((result, item) => {
        if (!validNodeMap[genNodeKey(item)]) {
            result.push(item);
        }

        return result;
    }, []);
};

export const getInvalidDynamicGroupList = (lastDynamicGroupList, validDynamicGroupList) => {
    const validDynamicGroupMap = validDynamicGroupList.reduce((result, item) => ({
        ...result,
        [item.id]: true,
    }), {});

    return lastDynamicGroupList.reduce((result, item) => {
        if (!validDynamicGroupMap[item.id]) {
            result.push(item);
        }

        return result;
    }, []);
};
