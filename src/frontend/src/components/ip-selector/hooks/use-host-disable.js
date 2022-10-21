import _ from 'lodash';

import useIpSelector from './use-ip-selector';

export default function () {
    const context = useIpSelector();

    return (hostData) => {
        const { disableHostMethod } = context;
        const options = {
            disabled: false,
            tooltips: {
                disabled: true,
                content: '',
            },
        };
        if (!disableHostMethod) {
            return options;
        }
        const checkValue = disableHostMethod(hostData);
        if (checkValue) {
            options.disabled = true;
            options.tooltips.disabled = false;
            options.tooltips.content = _.isString(checkValue) ? checkValue : '主机被禁用';
        }
        return options;
    };
}
