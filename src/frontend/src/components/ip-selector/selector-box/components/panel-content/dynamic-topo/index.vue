<template>
    <div class="ip-selector-dynamic-topo">
        <div class="tree-box">
            <bk-input
                v-model="filterKey"
                placeholder="搜索拓扑节点"
                style="margin-bottom: 12px;" />
            <bk-big-tree
                ref="treeRef"
                :data="topoTreeData"
                show-link-line
                show-checkbox
                selectable
                :filter-method="filterMethod"
                :check-strictly="false"
                :expand-on-click="false"
                @select-change="handleNodeSelect"
                @check-change="handleNodeCheckChange">
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
        <div class="ip-table">
            <template v-if="selectedTopoNode.instanceName">
                <table-tab
                    :model-value="renderTableType"
                    @change="handleTableTypeChange">
                    <table-tab-item name="node">
                        {{ selectedTopoNode.instanceName }} ({{ selectedTopoNode.child.length }})
                    </table-tab-item>
                    <table-tab-item name="host">
                        主机 ({{ selectedTopoNode.count }})
                    </table-tab-item>
                </table-tab>
                <div :key="`${selectedTopoNode.objectId}${selectedTopoNode.instanceId}`">
                    <keep-alive>
                        <component
                            :is="renderTableCom"
                            :node="selectedTopoNode"
                            :data="renderNodeList"
                            :checked-map="nodeCheckedMap"
                            @check-change="handleTableNodeCheckChange" />
                    </keep-alive>
                </div>
            </template>
            <div v-else>
                请在右侧选择节点
            </div>
        </div>
    </div>
</template>
<script setup>
    import {
        computed,
        ref,
        shallowRef,
        watch,
        nextTick,
    } from 'vue';
    import useDebounceRef from '../../../../hooks/use-debounced-ref';
    import useTreeExpanded from '../../../../hooks/use-tree-expanded';
    import useTreeFilter from '../../../../hooks/use-tree-filter';
    import { genNodeKey } from '../../../../utils';
    import TableTab from '../../table-tab';
    import TableTabItem from '../../table-tab/item.vue';
    import RenderNodeTable from './render-node-table.vue';
    import RenderHostTable from './render-host-table.vue';
    
    const props = defineProps({
        topoTreeData: {
            type: Array,
            required: true,
        },
        lastNodeList: {
            type: Array,
            default: () => [],
        },
    });
    
    const emits = defineEmits([
        'change',
    ]);

    const tableComMap = {
        node: RenderNodeTable,
        host: RenderHostTable,
    };

    const treeRef = ref();
    const topoTreeSearch = useDebounceRef('');
    const renderTableType = ref('node');
    
    const nodeCheckedMap = shallowRef({});

    const renderNodeList = shallowRef([]);
    const selectedTopoNode = shallowRef({});

    let isInnerChange = false;

    const renderTableCom = computed(() => tableComMap[renderTableType.value]);

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

    // 同步拓扑树节点的选中状态
    const syncTopoTreeNodeCheckStatus = () => {
        treeRef.value.removeChecked({
            emitEvent: false,
        });
        treeRef.value.setChecked(Object.keys(nodeCheckedMap.value), {
            emitEvent: false,
            checked: true,
        });
    };

    // 同步拓扑树数据
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

    // 同步节点的选中值
    watch(() => props.lastNodeList, (lastNodeList) => {
        if (isInnerChange) {
            isInnerChange = false;
            return;
        }
        nodeCheckedMap.value = lastNodeList.reduce((result, item) => {
            result[genNodeKey(item)] = item;
            return result;
        }, {});
        nextTick(() => {
            syncTopoTreeNodeCheckStatus();
        });
    }, {
        immediate: true,
    });

    // 拓扑树搜索
    watch(topoTreeSearch, () => {
        treeRef.value.filter(topoTreeSearch.value);
    });

    const triggerChange = () => {
        isInnerChange = true;
        emits('change', 'nodeList', Object.values(nodeCheckedMap.value));
    };

    // 选择节点，查看节点的子节点和主机列表
    const handleNodeSelect = (node) => {
        renderTableType.value = 'node';
        selectedTopoNode.value = node.data.payload;
    };

    // 在拓扑树中选中节点
    const handleNodeCheckChange = (allCheckNodeId, checkedNode) => {
        const checkedMap = { ...nodeCheckedMap.value };
        const nodeKey = genNodeKey(checkedNode.data.payload);
        if (checkedNode.checked) {
            checkedMap[nodeKey] = checkedNode.data.payload;
        } else {
            delete checkedMap[nodeKey];
        }
        
        nodeCheckedMap.value = checkedMap;
        triggerChange();
    };

    // 切换显示列表
    const handleTableTypeChange = (type) => {
        renderTableType.value = type;
    };

    // 在子节点列表中选中节点
    const handleTableNodeCheckChange = (checkedMap) => {
        nodeCheckedMap.value = checkedMap;
        syncTopoTreeNodeCheckStatus();
        triggerChange();
    };
</script>
<style lang="postcss">
    @import "../../../../styles/tree.mixin.css";

    .ip-selector-dynamic-topo {
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

        .ip-table {
            flex: 1;
            padding-left: 24px;

            .table-type-tab {
                display: flex;
                height: 40px;
                align-items: center;
                font-size: 14px;
                color: #63656e;

                .tab-item {
                    padding: 0 10px;
                }
            }
        }
    }
</style>
