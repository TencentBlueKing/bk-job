<template>
    <div
        v-bkloading="{ isLoading }"
        class="render-host-table">
        <render-host-table
            :data="tableData"
            :height="renderTableHeight"
            :pagination="pagination"
            @pagination-change="handlePaginationChange" />
    </div>
</template>
<script>
    export default {
        inheritAttrs: false,
    };
</script>
<script setup>
    import {
        reactive,
        ref,
        shallowRef,
        watch,
    } from 'vue';

    import RenderHostTable from '../../../../common/render-table/host/index.vue';
    import useDialogSize from '../../../../hooks/use-dialog-size';
    import Manager from '../../../../manager';
    import { getPaginationDefault } from '../../../../utils';

    const props = defineProps({
        node: {
            type: Object,
            required: true,
        },
    });

    const tableOffetTop = 70;
    const {
        contentHeight: dialogContentHeight,
    } = useDialogSize();
    const renderTableHeight = dialogContentHeight.value - tableOffetTop;

    const pagination = reactive(getPaginationDefault(renderTableHeight));

    const isLoading = ref(false);

    const tableData = shallowRef([]);

    // 获取节点的主机列表
    const fetchNodeHostList = () => {
        isLoading.value = true;
        const nodeData = props.node.data.payload;
        Manager.service.fetchTopologyHostsNodes({
            [Manager.nameStyle('nodeList')]: [
                {
                    [Manager.nameStyle('objectId')]: nodeData.object_id,
                    [Manager.nameStyle('instanceId')]: nodeData.instance_id,
                    [Manager.nameStyle('meta')]: nodeData.meta,
                },
            ],
            [Manager.nameStyle('pageSize')]: pagination.limit,
            [Manager.nameStyle('start')]: (pagination.current - 1) * pagination.limit,
            [Manager.nameStyle('searchContent')]: '',
        }).then((data) => {
            tableData.value = data.data;
            pagination.count = data.total;
        })
        .finally(() => {
            isLoading.value = false;
        });
    };

    // 拓扑树选中节点
    watch(() => props.node, () => {
        pagination.current = 1;
        fetchNodeHostList();
    }, {
        immediate: true,
    });

    // 分页
    const handlePaginationChange = (currentPagination) => {
        pagination.current = currentPagination.current;
        pagination.limit = currentPagination.limit;
        fetchNodeHostList();
    };

</script>
<style lang="postcss" scoped>
    .render-host-table {
        padding-top: 12px;
    }
</style>
