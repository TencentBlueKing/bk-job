import Component from './index.vue';
import { merge } from './manager';

export default (options) => {
    merge(options);

    return Component;
};
