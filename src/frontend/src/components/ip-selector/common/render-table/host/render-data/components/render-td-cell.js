import _ from 'lodash';

import { getObjectValueByPath } from '../../../../../utils';

export default {
    props: {
        config: {
            type: Object,
            required: true,
        },
        data: {
            type: Object,
            required: true,
        },
    },
    render (h) {
        return _.isFunction(this.config.renderCell)
            ? this.config.renderCell(h)
            : h('span', {}, getObjectValueByPath(this.data, this.config.field) || '--');
    },
};
