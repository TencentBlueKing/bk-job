export const topolopyHostIdsNodes = (data = {}) => ({
    page_size: data.pageSize,
    start: data.start,
    total: data.total,
    data: data.data.map(item => ({
        host_id: item.hostId,
        meta: item.meta,
    })),
});
