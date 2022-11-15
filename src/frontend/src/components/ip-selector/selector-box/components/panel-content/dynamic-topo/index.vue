<template>
    <div class="ip-selector-dynamic-topo">
        <div
            v-if="isNotSupport"
            class="not-support-dynamic-topo">
            <img src="../../../../images/empty.svg">
            <p>当前业务（集）暂不支持使用动态-拓扑选择</p>
        </div>
        <template v-else>
            <resize-layout
                v-if="topoTreeData.length > 0"
                :default-width="265"
                flex-direction="left">
                <div class="tree-box">
                    <bk-input
                        v-model="filterKey"
                        clearable
                        placeholder="搜索拓扑节点"
                        style="margin-bottom: 12px;" />
                    <bk-big-tree
                        ref="treeRef"
                        :check-strictly="false"
                        :data="topoTreeData"
                        expand-on-click
                        :filter-method="filterMethod"
                        :lazy-disabled="lazyDisabledCallbak"
                        :lazy-method="lazyMethodCallback"
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
                                        :key="`filter_${isHidedEmptyNode}`"
                                        v-bk-tooltips="`${isHidedEmptyNode ? '显示没有主机的节点' : '隐藏没有主机的节点'}`"
                                        class="topo-node-filter"
                                        :style="{
                                            display: isHidedEmptyNode ? 'block' : 'none',
                                        }"
                                        @click.stop="handleToggleFilterWithCount">
                                        <ip-selector-icon :type="`${isHidedEmptyNode ? 'invisible1' : 'visible1'}`" />
                                    </div>
                                </template>
                                <div
                                    v-if="calcShowExpanded(nodeItem)"
                                    :key="`expanded_${nodeItem.expanded}`"
                                    v-bk-tooltips="`${nodeItem.expanded ? '收起所有节点' : '展开所有节点'}`"
                                    class="topo-node-expand"
                                    @click.stop="handleToggleTopoTreeExpanded(nodeItem)">
                                    <ip-selector-icon :type="`${nodeItem.expanded ? 'shangxiachengkai-2' : 'shangxiachengkai'}`" />
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
                        <template v-if="selectedTopoNode.id">
                            <table-tab
                                :model-value="renderTableType"
                                @change="handleTableTypeChange">
                                <table-tab-item name="node">
                                    {{ selectedTopoNode.name }} ({{ selectedTopoNode.children.length }})
                                </table-tab-item>
                                <table-tab-item name="host">
                                    主机 ({{ selectedTopoNode.data.payload.count }})
                                </table-tab-item>
                            </table-tab>
                            <div :key="selectedTopoNode.id">
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
                class="create-dynamic-topo">
                <span>无数据，</span>
                <a
                    :href="config.bk_cmdb_static_topo_url"
                    target="_blank">
                    去创建
                </a>
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
    import {
        computed,
        nextTick,
        ref,
        shallowRef,
        watch,
    } from 'vue';

    import IpSelectorIcon from '../../../../common/ip-selector-icon';
    import useDebounceRef from '../../../../hooks/use-debounced-ref';
    import useFetchConfig from '../../../../hooks/use-fetch-config';
    import useTreeExpanded from '../../../../hooks/use-tree-expanded';
    import useTreeFilter from '../../../../hooks/use-tree-filter';
    import useTreeLazy from '../../../../hooks/use-tree-lazy';
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

    const checkNotSupport = () => {
        if (props.topoTreeData.length === 1
            && props.topoTreeData[0].object_id === 'biz_set') {
            return true;
        }
        return false;
    };

    const tableComMap = {
        node: RenderNodeTable,
        host: RenderHostTable,
    };

    const treeRef = ref();
    const topoTreeSearch = useDebounceRef('');
    const renderTableType = ref('node');

    const isNotSupport = ref(false);

    const nodeCheckedMap = shallowRef({});

    const renderNodeList = shallowRef([]);
    const selectedTopoNode = shallowRef({});

    let isInnerChange = false;

    const renderTableCom = computed(() => tableComMap[renderTableType.value]);

    const {
        filterKey,
        filterMethod,
        filterWithCount: isHidedEmptyNode,
        toggleFilterWithCount: handleToggleFilterWithCount,
    } = useTreeFilter(treeRef);

    const {
        calcShowExpanded,
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

    const {
        lazyDisabledCallbak,
        lazyMethodCallback,
    } = useTreeLazy(() => {
        syncTopoTreeNodeCheckStatus();
    });

    // 同步拓扑树数据
    watch(() => props.topoTreeData, () => {
        isNotSupport.value = checkNotSupport();
        // 业务集不支持动态拓扑
        if (isNotSupport.value) {
            return;
        }
        nextTick(() => {
            if (props.topoTreeData.legnth < 1) {
                return;
            }
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
        if (isNotSupport.value) {
            return;
        }
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
        if (isNotSupport.value) {
            return;
        }
        treeRef.value.filter(topoTreeSearch.value);
    });

    const triggerChange = () => {
        isInnerChange = true;
        emits('change', 'nodeList', Object.values(nodeCheckedMap.value));
    };

    // 选择节点，查看节点的子节点和主机列表
    const handleNodeSelect = (node) => {
        selectedTopoNode.value = node;
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

        .create-dynamic-topo,
        .not-support-dynamic-topo {
            width: 100%;
            padding-top: 120px;
            text-align: center;
        }
    }
</style>
