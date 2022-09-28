import {
    computed,
    reactive,
    ref,
    watch,
} from 'vue';

import { encodeRegexp } from '../utils';

import useDebounceRef from './use-debounced-ref';

export default (
    originalData,
    paginationDefault = {},
    filterCallback = () => true,
) => {
    const pagination = reactive({
        count: 0,
        current: 1,
        limit: 10,
        small: true,
        showTotalCount: false,
        align: 'right',
        ...paginationDefault,
    });

    const searchKey = useDebounceRef('');

    const serachList = computed(() => {
        if (!searchKey.value) {
            return originalData.value;
        }
        const searchRule = new RegExp(encodeRegexp(searchKey.value), 'i');
        return originalData.value.reduce((result, item) => {
            if (filterCallback(item, searchRule)) {
                result.push(item);
            }
            return result;
        }, []);
    });

    const isShowPagination = ref(false);

    const data = computed(() => serachList.value.slice(
        (pagination.current - 1) * pagination.limit,
        pagination.limit * pagination.current,
    ));

    const handlePaginationCurrentChange = (current) => {
        pagination.current = current;
    };
    const handlePaginationLimitChange = (limit) => {
        pagination.limit = limit;
    };
    
    watch(searchKey, () => {
        pagination.current = 1;
    });
    watch(serachList, (list) => {
        pagination.count = list.length;
        isShowPagination.value = list.length > 0 && list.length > pagination.limit;
    }, {
        immediate: true,
    });

    return {
        data,
        searchKey,
        serachList,
        isShowPagination,
        pagination,
        handlePaginationCurrentChange,
        handlePaginationLimitChange,
    };
};
