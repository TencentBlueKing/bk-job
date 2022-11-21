import _ from 'lodash';

export default {
    props: {
        config: {
            type: Object,
            required: true,
        },
    },
    render (h) {
        return _.isFunction(this.config.renderHead)
            ? this.config.renderHead(h)
            : h('span', {}, this.config.label);
    },
};
