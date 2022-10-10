import {
    reactive,
    ref,
} from 'vue';

import Manager from '../manager';

const config = reactive({
    bk_cmdb_dynamic_group_url: '',
    bk_cmdb_static_topo_url: '',
});
const loading = ref(true);

export default () => {
    if (!config.bk_cmdb_dynamic_group_url) {
        Promise.resolve()
            .then(() => Manager.service.fetchConfig())
            .then((data) => {
                config.bk_cmdb_dynamic_group_url = data.bk_cmdb_dynamic_group_url;
                loading.value = false;
            });
    }
    return {
        loading,
        config,
    };
};
