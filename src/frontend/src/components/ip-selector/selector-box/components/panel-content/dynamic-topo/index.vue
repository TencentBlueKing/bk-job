<template>
    <div class="ip-selector-dynamic-topo">
        <resize-layout
            v-if="topoTreeData.length > 0"
            :default-width="265"
            flex-direction="left">
            <div class="tree-box">
                <bk-input
                    v-model="filterKey"
                    placeholder="搜索拓扑节点"
                    style="margin-bottom: 12px;" />
                <bk-big-tree
                    ref="treeRef"
                    :check-strictly="false"
                    :data="topoTreeData"
                    :expand-on-click="false"
                    :filter-method="filterMethod"
                    selectable
                    show-checkbox
                    show-link-line
                    @check-change="handleNodeCheckChange"
                    @select-change="handleNodeSelect">
                    <template #default="{ node: nodeItem, data }">
                        <div class="topo-node-box">
                            <div class="topo-node-name">
                                {{ data.name }}
                            </div>
                            <template v-if="nodeItem.level === 0">
                                <div
                                    v-bk-tooltips="'隐藏没有主机的节点'"
                                    class="topo-node-filter"
                                    :style="{
                                        display: isShowEmptyNode ? 'block' : 'none',
                                    }"
                                    @click.stop="handleToggleFilterWithCount">
                                    <i
                                        class="bk-ipselector-icon"
                                        :class="{
                                            'bk-ipselector-invisible1': isShowEmptyNode,
                                            'bk-ipselector-visible1': !isShowEmptyNode,
                                        }" />
                                </div>
                            </template>
                            <div
                                v-if="!nodeItem.isLeaf"
                                v-bk-tooltips="'展开所有节点'"
                                class="topo-node-expand"
                                @click.stop="handleToggleTopoTreeExpanded(nodeItem)">
                                <i class="bk-ipselector-icon bk-ipselector-shangxiachengkai" />
                            </div>
                            <div class="topo-node-count">
                                {{ data.payload.count }}
                            </div>
                        </div>
                    </template>
                </bk-big-tree>
            </div>
            <template #right>
                <div class="table-box">
                    <template v-if="selectedTopoNode.instance_name">
                        <table-tab
                            :model-value="renderTableType"
                            @change="handleTableTypeChange">
                            <table-tab-item name="node">
                                {{ selectedTopoNode.instance_name }} ({{ selectedTopoNode.child.length }})
                            </table-tab-item>
                            <table-tab-item name="host">
                                主机 ({{ selectedTopoNode.count }})
                            </table-tab-item>
                        </table-tab>
                        <div :key="`${selectedTopoNode.object_id}${selectedTopoNode.instance_id}`">
                            <keep-alive>
                                <component
                                    :is="renderTableCom"
                                    :checked-map="nodeCheckedMap"
                                    :data="renderNodeList"
                                    :node="selectedTopoNode"
                                    style="min-height: 200px;"
                                    @check-change="handleTableNodeCheckChange" />
                            </keep-alive>
                        </div>
                    </template>
                    <div v-else>
                        请在右侧选择节点
                    </div>
                </div>
            </template>
        </resize-layout>
        <div
            v-else
            v-bkloading="{ isLoading: isConfigLoading }"
            class="create-static-topo">
            <span>无数据，</span>
            <a
                :href="config.bk_cmdb_static_topo_url"
                target="_blank">{{ $t('去创建') }}</a>
        </div>
    </div>
</template>
<script>
    export default {
        inheritAttrs: false,
    };
</script>
<script setup>
    import {
        computed,
        nextTick,
        ref,
        shallowRef,
        watch,
    } from 'vue';

    import useDebounceRef from '../../../../hooks/use-debounced-ref';
    import useFetchConfig from '../../../../hooks/use-fetch-config';
    import useTreeExpanded from '../../../../hooks/use-tree-expanded';
    import useTreeFilter from '../../../../hooks/use-tree-filter';
    import { genNodeKey } from '../../../../utils';
    import ResizeLayout from '../../resize-layout.vue';
    import TableTab from '../../table-tab';
    import TableTabItem from '../../table-tab/item.vue';

    import RenderHostTable from './render-host-table.vue';
    import RenderNodeTable from './render-node-table.vue';
    
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
        toggleExpanded: handleToggleTopoTreeExpanded,
    } = useTreeExpanded(treeRef);

    const {
        loading: isConfigLoading,
        config,
    } = useFetchConfig();

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
            height: 100%;
            padding-right: 16px;
            padding-left: 16px;
            overflow: auto;

            @include tree;
        }

        .table-box {
            flex: 1;
            padding-left: 24px;
        }

        .create-static-topo {
            width: 100%;
            padding-top: 120px;
            text-align: center;
        }
    }
</style>
