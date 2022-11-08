export const hostAgentStatisticsDynamicGroups = (data = []) => data.map((item) => {
    const {
        notAliveCount,
        aliveCount,
        totalCount,
    } = item.agentStatistics;
    const {
        id,
        name,
        meta,
    } = item.dynamicGroup;

    return {
        agent_statistics: {
            not_alive_count: notAliveCount,
            alive_count: aliveCount,
            total_count: totalCount,
        },
        dynamic_group: {
            id,
            name,
            meta,
        },
    };
});
