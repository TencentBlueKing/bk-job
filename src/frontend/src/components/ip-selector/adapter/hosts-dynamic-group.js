export const hostsDynamicGroup = (data = {}) => ({
    page_size: data.pageSize,
    start: data.start,
    total: data.total,
    data: data.data.map(item => ({
        alive: item.alive,
        cloud_area: item.cloudArea,
        host_id: item.hostId,
        host_name: item.hostName,
        ip: item.ip,
        ipv6: item.ipv6,
        os_name: item.osName,
        os_type: item.osType,
        cloud_vendor: item.coludVerdor,
        agent_id: item.agentId,
    })),
});
