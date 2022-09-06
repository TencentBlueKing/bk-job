import _ from 'lodash';

const typeServeiceMap = {
    staticTopo: [
        'fetchTopologyHostCount',
        'fetchTopologyHostsNodes',
        'fetchTopologyHostIdsNodes',
        'fetchHostsDetails',
        'fetchHostCheck',
    ],
    dynamicTopo: [
        'fetchTopologyHostCount',
        'fetchTopologyHostsNodes',
        'fetchNodesQueryPath',
        'fetchHostAgentStatisticsNodes',
    ],
    dynamicGroup: [
        'fetchDynamicGroups',
        'fetchHostsDynamicGroup',
        'fetchHostAgentStatisticsDynamicGroups',
    ],
    manualInput: [
        'fetchHostCheck',
        'fetchHostsDetails',
    ],
    common: [
        'fetchCustomSettings',
        'updateCustomSettings',
        'fetchConfig',
    ],
};

const baseConfigNameList = [
    'panelList',
    'unqiuePanelValue',
];

const config = {
    panelList: ['staticTopo', 'manualInput'], // 'staticTopo' | 'dynamicTopo' | 'dynamicGroup' | 'manualInput'
    unqiuePanelValue: false,
    nameStyle: 'camelCase', // 'camelCase' | 'kebabCase'
};

const service = {};

const getServiceListByPanelList
= panelList => _.uniq(panelList.reduce(
    (result, panelName) => result.concat(typeServeiceMap[panelName])
    , [...typeServeiceMap.common],
));

export const merge = (options) => {
    baseConfigNameList.forEach((name) => {
        config[name] = options[name];
    });

    getServiceListByPanelList(config.panelList).forEach((serviceName) => {
        if (!_.isFunction(options[serviceName])) {
            console.error(`使用 IP 选择器需要配置 *${serviceName}*`);
        }
        service[serviceName] = options[serviceName];
    });
};

export default {
    config,
    service,
    nameStyle: (name) => {
        if (config.nameStyle === 'camelCase') {
            return name;
        }
        return name.replace(/([A-Z])/g, '_$1').toLowerCase();
    },
};
