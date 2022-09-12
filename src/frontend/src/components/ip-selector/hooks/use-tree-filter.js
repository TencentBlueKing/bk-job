import {
    ref,
    watch,
} from 'vue';

import {
    encodeRegexp,
} from '../utils';

import useDebounceRef from './use-debounced-ref';

export default (treeRef) => {
    const filterKey = useDebounceRef('');
    const filterWithCount = ref(false);

    const filterMethod = (keyword, node) => {
        const [search] = keyword.split('-');
        const rule = new RegExp(`${encodeRegexp(search)}`, 'i');
        const nameFilterResult = rule.test(node.name);
        
        let countFilterResult = true;
        if (filterWithCount.value) {
            countFilterResult = node.data.payload.count > 0;
        }
       
        return nameFilterResult && countFilterResult;
    };

    const toggleFilterWithCount = () => {
        filterWithCount.value = !filterWithCount.value;
    };

    watch([filterKey, filterWithCount], () => {
        treeRef.value.filter(`${filterKey.value}-${filterWithCount.value}`);
    });

    return {
        filterKey,
        filterWithCount,
        filterMethod,
        toggleFilterWithCount,
    };
};
