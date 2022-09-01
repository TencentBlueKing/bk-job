<template>
    <div
        :style="styles"
        v-bkloading="{ isLoading }">
        <div style="padding: 8px 0;">
            <bk-button
                style="margin-right: 8px;"
                :loading="isCopyFaildLoading"
                @click="handeCopeAllFailedIP">
                复制异常 IP
            </bk-button>
            <bk-button
                :loading="isCopyAllLoading"
                @click="handleCopeAllIP">
                复制全部 IP
            </bk-button>
        </div>
        <render-host-table
            :data="tableData"
            :pagination="pagination"
            @pagination-change="handlePaginationChange" />
    </div>
</template>
<script setup>
    import {
        computed,
        reactive,
        ref,
        shallowRef,
        onMounted,
    } from 'vue';
    import Manager from '../../manager';
    import RenderHostTable from '../../common/render-table/host';
    import useDialogSize from '../../hooks/use-dialog-size';
    import useHostRenderKey from '../../hooks/use-host-render-key';
    import {
        getPaginationDefault,
        execCopy,
     } from '../../utils';

    const props = defineProps({
        dynamicGroup: {
            type: Object,
            required: true,
        },
    });

    const isLoading = ref(false);
    const isCopyFaildLoading = ref(false);
    const isCopyAllLoading = ref(false);
    const tableData = shallowRef([]);

     const {
        contentHeight,
    } = useDialogSize();

    const {
        key: hostRenderKey,
    } = useHostRenderKey();

    const pagination = reactive(getPaginationDefault(contentHeight.value));

    const styles = computed(() => ({
        height: `${contentHeight.value}px`,
    }));

    const requestHandler = (params = {}) => Manager.service.fetchHostsDynamicGroup({
            id: props.dynamicGroup.id,
            pageSize: pagination.limit,
            start: (pagination.current - 1) * pagination.limit,
            ...params,
        });

    const fetchDynamicGroupHostList = () => {
        isLoading.value = true;
        requestHandler()
        .then((data) => {
            tableData.value = data.data;
            pagination.count = data.total;
        })
        .finally(() => {
            isLoading.value = false;
        });
    };

    const handeCopeAllFailedIP = () => {
        isCopyFaildLoading.value = true;
        requestHandler({
            alive: 0,
        })
        .then((data) => {
            const IPList = data.data.map(item => item[hostRenderKey.value]);
            execCopy(IPList.join('\n'), `复制成功 ${IPList.length} 个 IP`);
        })
        .finally(() => {
            isCopyFaildLoading.value = false;
        });
    };

    const handleCopeAllIP = () => {
        isCopyAllLoading.value = true;
        requestHandler({
            alive: 0,
        })
        .then((data) => {
            const IPList = data.data.map(item => item[hostRenderKey.value]);
            execCopy(IPList.join('\n'), `复制成功 ${IPList.length} 个 IP`);
        })
        .finally(() => {
            isCopyAllLoading.value = false;
        });
    };

    const handlePaginationChange = (currentPagination) => {
        pagination.current = currentPagination.current;
        pagination.limit = currentPagination.limit;
        fetchDynamicGroupHostList();
    };

    onMounted(() => {
        fetchDynamicGroupHostList();
    });
</script>
