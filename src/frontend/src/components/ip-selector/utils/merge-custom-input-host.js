export const mergeCustomInputHost = (staticTopoHostList, customInputHostList) => {
    const staticTopoHostMap = staticTopoHostList.reduce((result, item) => ({
        ...result,
        [item.hostId]: true,
    }), {});
    
    const stack = [...staticTopoHostList];
    customInputHostList.forEach((hostData) => {
        if (!staticTopoHostMap[hostData.hostId]) {
            stack.push(hostData);
        }
    });

    return stack;
};
