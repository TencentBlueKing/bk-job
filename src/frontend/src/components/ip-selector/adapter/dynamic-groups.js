export const dynamicGroups = (data = []) => data.map(item => ({
    id: item.id,
    name: item.name,
    meta: item.meta,
    last_time: item.lastTime,
}));
