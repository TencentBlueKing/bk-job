export const getRepeatIpHostMap = (hostList) => {
    const hostIdMemo = {};
    const repeatMap = {};
    hostList.forEach((hostData) => {
        if (hostIdMemo[hostData.hostId]) {
            repeatMap[hostData.hostId] = true;
        }
        hostIdMemo[hostData.hostId] = true;
    });

    return repeatMap;
};
