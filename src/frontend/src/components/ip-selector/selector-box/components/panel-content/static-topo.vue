<template>
    <div class="ip-selector-static-topo">
        <div class="tree-box">
            <bk-input
                v-model="filterKey"
                placeholder="搜索拓扑节点"
                style="margin-bottom: 12px;" />
            <bk-big-tree
                ref="treeRef"
                :data="topoTreeData"
                :filter-method="filterMethod"
                show-link-line
                selectable
                :expand-on-click="false"
                @select-change="handleNodeSelect">
                <template #default="{ node: nodeItem, data }">
                    <div class="topo-node-box">
                        <div class="topo-node-name">{{ data.name }}</div>
                        <template v-if="nodeItem.level === 0">
                            <div
                                class="topo-node-filter"
                                @click="handleToggleFilterWithCount">
                                <i
                                    v-if="isShowEmptyNode"
                                    class="bk-ipselector-icon bk-ipselector-invisible1" />
                                <i
                                    v-else
                                    class="bk-ipselector-icon bk-ipselector-visible1" />
                            </div>
                            <div
                                class="topo-node-expand"
                                @click="handleToggleTopoTreeExpanded">
                                <i
                                    v-if="isTopoTreeExpanded"
                                    class="bk-ipselector-icon bk-ipselector-shangxiachengkai" />
                                <i
                                    v-else
                                    class="bk-ipselector-icon bk-ipselector-shangxiachengkai-2" />
                            </div>
                        </template>
                        <div class="topo-node-count">
                            {{ data.payload.count }}
                        </div>
                    </div>
                </template>
            </bk-big-tree>
        </div>
        <div
            class="host-table"
            v-bkloading="{ isLoading: isHostLoading }">
            <bk-input
                v-model="nodeHostListSearch"
                placeholder="请输入 IP/IPv6/主机名称 或 选择条件搜索"
                style="margin-bottom: 12px;"
                @keyup="handleEnterKeyUp" />
            <render-host-table
                :data="hostTableData"
                :pagination="pagination"
                :height="renderTableHeight"
                @pagination-change="handlePaginationChange"
                @row-click="handleRowClick">
                <template #header-selection>
                    <page-check
                        :value="pageCheckValue"
                        @change="handlePageCheck" />
                </template>
                <template #selection="{ row }">
                    <bk-checkbox :value="Boolean(hostCheckedMap[row.hostId])" />
                </template>
            </render-host-table>
        </div>
    </div>
</template>
<script setup>
    import {
        ref,
        reactive,
        shallowRef,
        watch,
        nextTick,
    } from 'vue';

    import Manager from '../../../manager';
    import { getPaginationDefault } from '../../../utils';
    import RenderHostTable from '../../../common/render-table/host.vue';
    import useDialogSize from '../../../hooks/use-dialog-size';
    import useDebounceRef from '../../../hooks/use-debounced-ref';
    import useInputEnter from '../../../hooks/use-input-enter';
    import useTreeExpanded from '../../../hooks/use-tree-expanded';
    import useTreeFilter from '../../../hooks/use-tree-filter';
    import PageCheck from '../table-page-check.vue';

    const props = defineProps({
        topoTreeData: {
            type: Array,
            required: true,
        },
        lastHostList: {
            type: Array,
            required: true,
        },
    });

    const emits = defineEmits([
        'change',
    ]);

    const tableOffetTop = 60;
    const {
        contentHeight: dialogContentHeight,
    } = useDialogSize();
    const renderTableHeight = dialogContentHeight.value - tableOffetTop;

    const pagination = reactive(getPaginationDefault(renderTableHeight));

    const treeRef = ref();
    const topoTreeSearch = useDebounceRef('');

    const isHostLoading = ref(false);
    const hostTableData = shallowRef([]);
    
    const pageCheckValue = ref('');
    const hostCheckedMap = shallowRef({});
    const nodeHostListSearch = ref('');

    let selectedTopoNode;
    let isInnerChange = false;

    const {
        filterKey,
        filterMethod,
        filterWithCount: isShowEmptyNode,
        toggleFilterWithCount: handleToggleFilterWithCount,
    } = useTreeFilter(treeRef);

    const {
        expanded: isTopoTreeExpanded,
        toggleExpanded: handleToggleTopoTreeExpanded,
    } = useTreeExpanded(treeRef);

    // 判断 page-check 的状态
    const syncPageCheckValue = () => {
        setTimeout(() => {
            if (hostTableData.value.length > 0) {
                pageCheckValue.value = 'page';
                hostTableData.value.forEach((hostItem) => {
                    if (!hostCheckedMap.value[hostItem.hostId]) {
                        pageCheckValue.value = '';
                    }
                });
            } else {
                pageCheckValue.value = '';
            }
        });
    };

    // 同步拓扑树的值
    watch(() => props.topoTreeData, () => {
        if (props.topoTreeData.legnth < 1) {
            return;
        }
        nextTick(() => {
            const [rootFirstNode] = props.topoTreeData;
            treeRef.value.setSelected(rootFirstNode.id, {
                emitEvent: true,
            });
            treeRef.value.setExpanded(rootFirstNode.id);
        });
    }, {
        immediate: true,
    });

    // 同步主机的选中状态
    watch(() => props.lastHostList, (lastHostList) => {
        if (isInnerChange) {
            isInnerChange = false;
            return;
        }
        hostCheckedMap.value = lastHostList.reduce((result, hostItem) => {
            result[hostItem.hostId] = hostItem;
            return result;
        }, {});
        syncPageCheckValue();
    }, {
        immediate: true,
    });

    // 拓扑树搜索
    watch(topoTreeSearch, () => {
        treeRef.value.filter(topoTreeSearch.value);
    });

    const triggerChange = () => {
        isInnerChange = true;
        emits('change', 'hostList', Object.values(hostCheckedMap.value));
    };

    // 获取选中节点的主机列表
    const fetchNodeHostList = () => {
        if (!selectedTopoNode) {
            return;
        }
        isHostLoading.value = true;
        Manager.service.fetchTopologyHostsNodes({
            nodeList: [
                {
                    objectId: selectedTopoNode.objectId,
                    instanceId: selectedTopoNode.instanceId,
                },
            ],
            pageSize: pagination.limit,
            start: (pagination.current - 1) * pagination.limit,
            searchContent: nodeHostListSearch.value,
        }).then((data) => {
            hostTableData.value = data.data;
            pagination.count = data.total;
            syncPageCheckValue();
        })
        .finally(() => {
            isHostLoading.value = false;
        });
    };

    const handleEnterKeyUp = useInputEnter(() => {
        fetchNodeHostList();
    });

    // 获取选中节点的完整主机列表
    const fetchNodeAllHostId = () => Manager.service.fetchTopologyHostIdsNodes({
            nodeList: [
                {
                    objectId: selectedTopoNode.objectId,
                    instanceId: selectedTopoNode.instanceId,
                },
            ],
            searchContent: nodeHostListSearch.value,
        }).then(data => data.data);

    // 选中节点
    const handleNodeSelect = (node) => {
        selectedTopoNode = node.data.payload;
        fetchNodeHostList();
    };

    // 本页全选、跨页全选
    const handlePageCheck = (checkValue) => {
        const checkedMap = { ...hostCheckedMap.value };
        Promise.resolve()
            .then(() => {
                if (checkValue === 'page') {
                    hostTableData.value.forEach((hostItem) => {
                        checkedMap[hostItem.hostId] = hostItem;
                    });
                } else if (checkValue === 'pageCancle') {
                    hostTableData.value.forEach((hostItem) => {
                        delete checkedMap[hostItem.hostId];
                    });
                } else if (checkValue === 'allCancle') {
                    return fetchNodeAllHostId()
                        .then((data) => {
                            data.forEach((hostData) => {
                                delete checkedMap[hostData.hostId];
                            });
                        });
                } else if (checkValue === 'all') {
                    return fetchNodeAllHostId()
                        .then((data) => {
                            data.forEach((hostData) => {
                                checkedMap[hostData.hostId] = hostData;
                            });
                        });
                }
            })
            .then(() => {
                hostCheckedMap.value = checkedMap;
                pageCheckValue.value = checkValue;
                triggerChange();
            });
    };

    // 选中指定主机
    const handleRowClick = (data) => {
        const checkedMap = { ...hostCheckedMap.value };
        if (checkedMap[data.hostId]) {
            delete checkedMap[data.hostId];
        } else {
            checkedMap[data.hostId] = data;
        }
        hostCheckedMap.value = checkedMap;
        syncPageCheckValue();
        triggerChange();
    };

    // 分页
    const handlePaginationChange = (currentPagination) => {
        pagination.current = currentPagination.current;
        pagination.limit = currentPagination.limit;
        fetchNodeHostList();
    };
</script>
<style lang="postcss">
    @import "../../../styles/tree.mixin.css";

    .ip-selector-static-topo {
        display: flex;
        height: 100%;

        .tree-box {
            width: 265px;
            height: 100%;
            padding-right: 15px;
            overflow: auto;
            border-right: 1px solid #dcdee5;

            @include tree;
        }

        .host-table {
            flex: 1;
            padding-left: 24px;
        }
    }
</style>
