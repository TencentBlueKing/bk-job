<template>
    <div
        v-bkloading="{ isLoading }"
        class="render-host-table">
        <div class="search-box">
            <bk-select
                @change="handleChildNodeChange"
                style="width: 264px; margin-right: 8px;">
                <bk-option
                    v-for="item in node.child"
                    :name="item.instanceName"
                    :id="item.instanceId"
                    :key="item.instanceId" />
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
<script setup>
    import {
        shallowRef,
        reactive,
        watch,
        ref,
    } from 'vue';
    import _ from 'lodash';
    import AppManageService from '@service/app-manage';
    import useDialogSize from '../../../../hooks/use-dialog-size';
    import useInputEnter from '../../../../hooks/use-input-enter';
    import RenderHostTable from '../../../../common/render-table/host.vue';
    import { getPaginationDefault } from '../../../../utils';

    const props = defineProps({
        node: {
            type: Object,
            required: true,
        },
    });

    let memoNode;

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
        AppManageService.fetchTopologyHost({
            appTopoNodeList: [{
                objectId: memoNode.objectId,
                instanceId: memoNode.instanceId,
            },
            ],
            pageSize: pagination.limit,
            start: (pagination.current - 1) * pagination.limit,
            searchContent: nodeHostListSearch.value,
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
        memoNode = props.node;
        fetchNodeHostList();
    }, {
        immediate: true,
    });

    const handleEnterKeyUp = useInputEnter(() => {
        fetchNodeHostList();
    });

    // 切换子节点
    const handleChildNodeChange = (instanceId) => {
        const selectNode = _.find(props.node.child, _ => _.instanceId === instanceId);
        memoNode = selectNode || props.node;
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
