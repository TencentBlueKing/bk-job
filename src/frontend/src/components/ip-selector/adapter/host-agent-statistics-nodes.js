export const hostAgentStatisticsNodes = (data = []) => data.map((item) => {
    const {
        notAliveCount,
        aliveCount,
        totalCount,
    } = item.agentStatistics;
    const {
        instanceId,
        objectId,
    } = item.node;

    return {
        agent_statistics: {
            not_alive_count: notAliveCount,
            alive_count: aliveCount,
            total_count: totalCount,
        },
        node: {
            instance_id: instanceId,
            object_id: objectId,
        },
    };
});
