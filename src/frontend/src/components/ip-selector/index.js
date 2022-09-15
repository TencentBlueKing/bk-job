import Entry from './entry.vue';
import { merge } from './manager';

console.log('from ip selecg = ', Entry);

export default (options) => {
    merge(options);

    return Entry;
};
