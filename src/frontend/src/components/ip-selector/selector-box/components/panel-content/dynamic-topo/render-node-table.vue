<template>
    <div
        v-bkloading="{ isLoading }"
        style="min-height: 300px;">
        <template v-if="!isChildrenLazyLoading">
            <template v-if="props.node.children.length > 0">
                <bk-input
                    v-model="searchKey"
                    placeholder="请输入节点名称搜索"
                    style="margin: 12px 0;" />
                <render-node-table
                    :agent-static="nodeAgentStaticMap"
                    :data="renderTableData"
                    :height="renderTableHeight"
                    @row-click="handleRowClick">
                    <template #header-selection>
                        <table-page-check
                            :disabled="renderTableData.length < 1"
                            :value="tablePageCheckValue"
                            @change="handlePageCheck" />
                    </template>
                    <template #selection="{ row }">
                        <bk-checkbox :value="Boolean(checkedMap[genNodeKey(row.node)])" />
                    </template>
                </render-node-table>
                <bk-pagination
                    v-if="isShowPagination"
                    :show-limit="false"
                    v-bind="pagination"
                    @change="handlePaginationCurrentChange"
                    @limit-change="handlePaginationLimitChange" />
            </template>
            <div
                v-else-if="!isLoading"
                style="padding-top: 120px; text-align: center;">
                <img src="../../../../images/empty.svg">
                <div>没有子节点</div>
            </div>
        </template>
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
        ref,
        shallowRef,
        watch,
    } from 'vue';

    import RenderNodeTable from '../../../../common/render-table/node.vue';
    import useDialogSize from '../../../../hooks/use-dialog-size';
    import useLocalPagination from '../../../../hooks/use-local-pagination';
    import Manager from '../../../../manager';
    import {
        genNodeKey,
        getPaginationDefault,
    } from '../../../../utils';
    import TablePageCheck from '../../table-page-check.vue';

    const props = defineProps({
        node: {
            type: Object,
            required: true,
        },
        checkedMap: {
            type: Object,
            default: () => ({}),
        },
    });

    const emits = defineEmits(['check-change']);

    const isChildrenLazyLoading = ref(false);
    const isLoading = ref(false);
    const tablePageCheckValue = ref('');

    const tableData = shallowRef([]);
    const nodeAgentStaticMap = shallowRef({});

    const tableOffetTop = 155;
    const {
        contentHeight: dialogContentHeight,
    } = useDialogSize();
    const renderTableHeight = dialogContentHeight.value - tableOffetTop;

    // 本地分页
    const {
        isShowPagination,
        searchKey,
        data: renderTableData,
        pagination,
        handlePaginationCurrentChange,
        handlePaginationLimitChange,
     } = useLocalPagination(
        tableData,
        getPaginationDefault(renderTableHeight),
        (node, rule) => rule.test(node.namePath),
    );

    let isInnerChange = false;

    // 判断 page-check 的状态
    const syncPageCheckValue = () => {
        setTimeout(() => {
            if (tableData.value.length > 0) {
            tablePageCheckValue.value = 'page';
            tableData.value.forEach((nodeItem) => {
                if (!props.checkedMap[nodeItem.key]) {
                    tablePageCheckValue.value = '';
                }
            });
        } else {
            tablePageCheckValue.value = '';
        }
        });
    };

    // 获取分组路径、agent状态
    const fetchData = () => {
        isLoading.value = true;
        const params = {
            [Manager.nameStyle('nodeList')]: props.node.children.map((children) => {
                const childrenData = children.data.payload;
                return {
                    [Manager.nameStyle('objectId')]: childrenData.object_id,
                    [Manager.nameStyle('instanceId')]: childrenData.instance_id,
                    [Manager.nameStyle('meta')]: childrenData.meta,
                };
            }),
        };
        // 查询节点路径
        Manager.service.fetchNodesQueryPath(params)
            .then((data) => {
                tableData.value = data.reduce((result, nodeStack) => {
                    const namePath = nodeStack.map(nodeData => nodeData.instance_name).join('/');
                    const tailNode = _.last(nodeStack);
                    result.push({
                        key: genNodeKey(tailNode),
                        node: tailNode,
                        namePath,
                    });
                    return result;
                }, []);

                syncPageCheckValue();
            })
            .finally(() => {
                isLoading.value = false;
            });
        // 查询节点的 agent 状态
        Manager.service.fetchHostAgentStatisticsNodes(params)
            .then((data) => {
                nodeAgentStaticMap.value = data.reduce((result, item) => {
                    result[genNodeKey(item.node)] = item.agent_statistics;
                    return result;
                }, {});
            });
    };

    watch(() => [props.node, props.node.state.loading], ([node, lazyLoading]) => {
        isChildrenLazyLoading.value = lazyLoading;
        isLoading.value = lazyLoading;
        if (!lazyLoading) {
            fetchData();
        }
    }, {
        immediate: true,
    });
    
    watch(() => props.checkedMap, () => {
        if (isInnerChange) {
            isInnerChange = false;
            return;
        }
        syncPageCheckValue();
    }, {
        immediate: true,
    });

    // 本页全选、跨页全选
    const handlePageCheck = (checkValue) => {
        let newCheckedMap = { ...props.checkedMap };
        if (checkValue === 'page') {
            renderTableData.value.forEach((nodeItem) => {
                newCheckedMap[nodeItem.key] = nodeItem.node;
            });
        } else if (checkValue === 'pageCancle') {
            renderTableData.value.forEach((nodeItem) => {
                delete newCheckedMap[nodeItem.key];
            });
        } else if (checkValue === 'allCancle') {
            newCheckedMap = {};
        } else if (checkValue === 'all') {
            tableData.value.forEach((nodeItem) => {
                newCheckedMap[nodeItem.key] = nodeItem.node;
            });
        }
        tablePageCheckValue.value = checkValue;
        isInnerChange = true;
        emits('check-change', newCheckedMap);
    };

    // 选中节点
    const handleRowClick = (rowData) => {
        const nodeKey = genNodeKey(rowData.node);
        const newCheckedMap = { ...props.checkedMap };
        if (newCheckedMap[nodeKey]) {
            delete newCheckedMap[nodeKey];
        } else {
            newCheckedMap[nodeKey] = rowData.node;
        }
        isInnerChange = true;
        emits('check-change', newCheckedMap);
        syncPageCheckValue();
    };
</script>
