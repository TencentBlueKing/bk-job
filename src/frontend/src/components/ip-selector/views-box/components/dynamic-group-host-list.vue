<template>
    <div
        v-bkloading="{ isLoading }"
        style="min-height: 500px;">
        <div style="padding: 8px 0;">
            <bk-button
                :loading="isCopyFaildLoading"
                style="margin-right: 8px;"
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
        reactive,
        ref,
        shallowRef,
        watch,
    } from 'vue';

    import RenderHostTable from '../../common/render-table/host';
    import useDialogSize from '../../hooks/use-dialog-size';
    import useHostRenderKey from '../../hooks/use-host-render-key';
    import Manager from '../../manager';
    import {
        execCopy,
        getPaginationDefault,
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

    const requestHandler = (params = {}) => Manager.service.fetchHostsDynamicGroup({
            [Manager.nameStyle('id')]: props.dynamicGroup.id,
            [Manager.nameStyle('pageSize')]: pagination.limit,
            [Manager.nameStyle('start')]: (pagination.current - 1) * pagination.limit,
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

    watch(() => props.dynamicGroup, () => {
        if (props.dynamicGroup.id) {
            fetchDynamicGroupHostList();
        }
    });

    const handeCopeAllFailedIP = () => {
        isCopyFaildLoading.value = true;
        requestHandler({
            [Manager.nameStyle('alive')]: 0,
            [Manager.nameStyle('pageSize')]: -1,
            [Manager.nameStyle('start')]: 0,
        })
        .then((data) => {
            const ipList = data.data.map(item => item[hostRenderKey.value]);
            execCopy(ipList.join('\n'), `复制成功 ${ipList.length} 个 IP`);
        })
        .finally(() => {
            isCopyFaildLoading.value = false;
        });
    };

    const handleCopeAllIP = () => {
        isCopyAllLoading.value = true;
        requestHandler({
            [Manager.nameStyle('pageSize')]: -1,
            [Manager.nameStyle('start')]: 0,
        })
        .then((data) => {
            const ipList = data.data.map(item => item[hostRenderKey.value]);
            execCopy(ipList.join('\n'), `复制成功 ${ipList.length} 个 IP`);
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
</script>
