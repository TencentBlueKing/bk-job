export default {
    ip: {
        name: 'IP',
        field: 'ip',
        width: '120px',
    },
    ipv6: {
        name: 'IPv6',
        field: 'ipv6',
        width: '180px',
    },
    coludArea: {
        name: '云区域',
        field: 'cloud_area.name',
        width: '150px',
    },
    alive: {
        name: 'Agent 状态',
        field: 'alive',
        width: '100px',
        // filter: [
        //     {
        //         name: '正常',
        //         value: 1,
        //     },
        //     {
        //         name: '异常',
        //         value: 0,
        //     },
        // ],
    },
    hostName: {
        name: '主机名称',
        field: 'host_name',
        width: '200px',
    },
    osName: {
        name: 'OS 名称',
        field: 'os_name',
        width: '150px',
        // filter: [
        //     {
        //         name: 'Windows',
        //         value: 'windows',
        //     },
        //     {
        //         name: 'Linux',
        //         value: 'linux',
        //     },
        // ],
    },
    coludVerdor: {
        name: '所属云厂商',
        field: 'cloud_vendor',
        width: '200px',
    },
    osType: {
        name: 'OS 类型',
        field: 'os_type',
        width: '120px',
    },
    hostId: {
        name: 'Host ID',
        field: 'host_id',
        width: '100px',
    },
    agentId: {
        name: 'Agent ID',
        field: 'agent_id',
        width: '100px',
    },
};
