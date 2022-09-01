const serviceMethodNameList = [
    'fetchTopologyHostCount',
    'fetchTopologyHostsNodes',
    'fetchTopologyHostIdsNodes',
    'fetchHostsDetails',
    'fetchHostCheck',
    'fetchNodesQueryPath',
    'fetchHostAgentStatisticsNodes',
    'fetchDynamicGroups',
    'fetchHostsDynamicGroup',
    'fetchHostAgentStatisticsDynamicGroups',
    'fetchCustomSettings',
    'updateCustomSettings',
];
const baseConfigNameList = [
    'panelList',
    'unqiuePanelValue',
];

const config = {
    panelList: ['staticTopo', 'customInput'],
    unqiuePanelValue: false,
};

const service = {};

export const merge = (options) => {
    baseConfigNameList.forEach((name) => {
        config[name] = options[name];
    });
    
    serviceMethodNameList.forEach((name) => {
        service[name] = options[name];
    });
};

export default {
    config,
    service,
};
