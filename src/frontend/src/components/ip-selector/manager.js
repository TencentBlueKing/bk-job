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

let lastVersion = '';

const config = {
    panelList: ['staticTopo', 'manualInput'], // 'staticTopo' | 'dynamicTopo' | 'dynamicGroup' | 'manualInput'
    unqiuePanelValue: false,
    nameStyle: 'kebabCase', // 'camelCase' | 'kebabCase'
    hostTableCustomColumnList: [],
    hostTableRenderColumnList: [],
};

const service = {};

const getServiceListByPanelList
= panelList => _.uniq(panelList.reduce(
    (result, panelName) => result.concat(typeServeiceMap[panelName])
    , [...typeServeiceMap.common],
));

export const merge = (options) => {
    if (options.version) {
        lastVersion = options.version;
    }
    Object.keys(config).forEach((key) => {
        if (options[key]) {
            config[key] = options[key];
        }
    });

    console.log('from merge = ', config);

    getServiceListByPanelList(config.panelList).forEach((serviceName) => {
        if (!_.isFunction(options[serviceName])) {
            console.error(`使用 IP 选择器需要配置 *${serviceName}*`);
        }
        service[serviceName] = options[serviceName];
    });
};

let localService = {};

export const mergeLocalService = (service) => {
    localService = service;
};

let localConfig = {};
export const mergeLocalConfig = (config) => {
    localConfig = config;
};

export default {
    config: new Proxy({}, {
        get (target, propKey) {
            if (propKey === 'version') {
                return `${lastVersion}`;
            }
            if (localConfig[propKey]) {
                return localConfig[propKey];
            }
            return config[propKey];
        },
    }),
    service: new Proxy({}, {
        get (target, propKey) {
            if (_.isFunction(localService[propKey])) {
                return localService[propKey];
            }
            if (!service[propKey]) {
                return Promise.reject(new Error(`请配置 *${propKey}* 方法`));
            }
            return service[propKey];
        },
    }),
    nameStyle: (name) => {
        if (config.nameStyle === 'camelCase') {
            return name;
        }
        return name.replace(/([A-Z])/g, '_$1').toLowerCase();
    },
};
