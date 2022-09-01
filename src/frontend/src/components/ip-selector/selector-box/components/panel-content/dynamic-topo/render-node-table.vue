<template>
    <div v-bkloading="{ isLoading }">
        <bk-input
            v-model="searchKey"
            placeholder="请输入节点名称搜索"
            style="margin: 12px 0;" />
        <render-node-table
            :data="renderTableData"
            :agent-static="agentStaticMap"
            :height="renderTableHeight"
            @row-click="handleRowClick">
            <template #header-selection>
                <page-check
                    :value="pageCheckValue"
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
    </div>
</template>
<script setup>
    import {
        ref,
        shallowRef,
        watch,
    } from 'vue';
    import _ from 'lodash';
    import Manager from '../../../../manager';
    import {
        genNodeKey,
        getPaginationDefault,
    } from '../../../../utils';
    import useLocalPagination from '../../../../hooks/use-local-pagination';
    import useDialogSize from '../../../../hooks/use-dialog-size';
    import RenderNodeTable from '../../../../common/render-table/node.vue';
    import PageCheck from '../../table-page-check.vue';

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

    const isLoading = ref(true);
    const pageCheckValue = ref('');

    const tableData = shallowRef([]);
    const agentStaticMap = shallowRef({});

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
            pageCheckValue.value = 'page';
            tableData.value.forEach((nodeItem) => {
                if (!props.checkedMap[nodeItem.key]) {
                    pageCheckValue.value = '';
                }
            });
        } else {
            pageCheckValue.value = '';
        }
        });
    };

    // 获取分组路径、agent状态
    const fetchData = () => {
        isLoading.value = true;
        const nodeList = props.node.child.map(item => ({
            objectId: item.objectId,
            instanceId: item.instanceId,
        }));
        Manager.service.fetchNodesQueryPath({
            nodeList,
        })
            .then((data) => {
                tableData.value = data.reduce((result, item) => {
                    const namePath = item.map(({ instanceName }) => instanceName).join('/');
                    const tailNode = _.last(item);
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
        Manager.service.fetchHostAgentStatisticsNodes({
            nodeList,
        }).then((data) => {
            agentStaticMap.value = data.reduce((result, item) => {
                result[genNodeKey(item.node)] = item.agentStatistics;
                return result;
            }, {});
        });
    };

    watch(() => props.data, () => {
        fetchData();
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
