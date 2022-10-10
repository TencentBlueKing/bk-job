import Entry from './entry.vue';
import { merge } from './manager';

export default (options) => {
    merge(options);

    return Entry;
};
