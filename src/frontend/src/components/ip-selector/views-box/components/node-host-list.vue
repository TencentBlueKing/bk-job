<template>
    <div
        :style="styles"
        v-bkloading="{ isLoading }">
        <div style="padding: 8px 0;">
            <bk-input />
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
    import AppManageService from '@service/app-manage';
    import useDialogSize from '../../hooks/use-dialog-size';
    import RenderHostTable from '../../common/render-table/host';
    import { getPaginationDefault } from '../../utils';

    const props = defineProps({
        node: {
            type: Object,
            required: true,
        },
    });

     const {
        contentHeight,
    } = useDialogSize();

    const pagination = reactive(getPaginationDefault(contentHeight.value));
    
    const isLoading = ref(false);
    const tableData = shallowRef([]);

    const styles = computed(() => ({
        height: `${contentHeight.value}px`,
    }));

    const fetchNodeHostList = () => {
        isLoading.value = true;
        AppManageService.fetchTopologyHost({
            appTopoNodeList: [{
                objectId: props.node.objectId,
                instanceId: props.node.instanceId,
            },
            ],
            pageSize: pagination.limit,
            start: (pagination.current - 1) * pagination.limit,
        }).then((data) => {
            tableData.value = data.data;
            pagination.count = data.total;
        })
        .finally(() => {
            isLoading.value = false;
        });
    };

    const handlePaginationChange = (currentPagination) => {
        pagination.current = currentPagination.current;
        pagination.limit = currentPagination.limit;
        fetchNodeHostList();
    };

    onMounted(() => {
        fetchNodeHostList();
    });
</script>
