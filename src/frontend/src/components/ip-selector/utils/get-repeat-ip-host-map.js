export const getRepeatIpHostMap = (hostList) => {
    const hostIdMemo = {};
    const repeatMap = {};
    hostList.forEach((hostData) => {
        if (hostIdMemo[hostData.host_id]) {
            repeatMap[hostData.host_id] = true;
        }
        hostIdMemo[hostData.host_id] = true;
    });

    return repeatMap;
};
