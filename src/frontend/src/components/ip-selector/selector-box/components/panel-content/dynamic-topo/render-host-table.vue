<template>
    <div
        v-bkloading="{ isLoading }"
        class="render-host-table">
        <div class="search-box">
            <bk-select
                style="width: 264px; margin-right: 8px;"
                @change="handleChildNodeChange">
                <bk-option
                    v-for="item in node.child"
                    :id="item.instance_id"
                    :key="item.instance_id"
                    :name="item.instance_name" />
            </bk-select>
            <bk-input
                v-model="nodeHostListSearch"
                @keyup="handleEnterKeyUp" />
        </div>
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
    import _ from 'lodash';
    import {
        reactive,
        ref,
        shallowRef,
        watch,
    } from 'vue';

    import RenderHostTable from '../../../../common/render-table/host/index.vue';
    import useDialogSize from '../../../../hooks/use-dialog-size';
    import useInputEnter from '../../../../hooks/use-input-enter';
    import Manager from '../../../../manager';
    import { getPaginationDefault } from '../../../../utils';

    const props = defineProps({
        node: {
            type: Object,
            required: true,
        },
    });

    let memoNodeData;

    const tableOffetTop = 115;
    const {
        contentHeight: dialogContentHeight,
    } = useDialogSize();
    const renderTableHeight = dialogContentHeight.value - tableOffetTop;

    const pagination = reactive(getPaginationDefault(renderTableHeight));

    const isLoading = ref(false);
    const nodeHostListSearch = ref('');

    const tableData = shallowRef([]);

    // 获取节点的主机列表
    const fetchNodeHostList = () => {
        isLoading.value = true;
        Manager.service.fetchTopologyHostsNodes({
            [Manager.nameStyle('nodeList')]: [
                {
                    [Manager.nameStyle('objectId')]: memoNodeData.object_id,
                    [Manager.nameStyle('instanceId')]: memoNodeData.instance_id,
                    [Manager.nameStyle('meta')]: memoNodeData.meta,
                },
            ],
            [Manager.nameStyle('pageSize')]: pagination.limit,
            [Manager.nameStyle('start')]: (pagination.current - 1) * pagination.limit,
            [Manager.nameStyle('searchContent')]: nodeHostListSearch.value,
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
        memoNodeData = props.node.data.payload;
        fetchNodeHostList();
    }, {
        immediate: true,
    });

    const handleEnterKeyUp = useInputEnter(() => {
        pagination.current = 1;
        fetchNodeHostList();
    });

    // 切换子节点
    const handleChildNodeChange = (instanceId) => {
        const selectNodeData = _.find(props.node.child, _ => _.instance_id === instanceId);
        memoNodeData = selectNodeData || props.node;
        pagination.current = 1;
        fetchNodeHostList();
    };

    // 分页
    const handlePaginationChange = (currentPagination) => {
        pagination.current = currentPagination.current;
        pagination.limit = currentPagination.limit;
        fetchNodeHostList();
    };

</script>
<style lang="postcss" scoped>
    .render-host-table {
        .search-box {
            display: flex;
            margin: 12px 0;
        }
    }
</style>
