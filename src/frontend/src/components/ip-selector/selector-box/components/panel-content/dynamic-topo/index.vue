<template>
    <div class="ip-selector-dynamic-topo">
        <div class="tree-box">
            <bk-input
                placeholder="搜索拓扑节点"
                style="margin-bottom: 12px;" />
            <bk-big-tree
                ref="treeRef"
                :data="topoTreeData"
                show-link-line
                show-checkbox
                selectable
                :expand-on-click="false"
                @select-change="handleNodeSelect"
                @check-change="handleCheckChange">
                <template #default="{ node: nodeItem, data }">
                    <div class="topo-node-box">
                        <div class="topo-node-name">{{ data.name }}</div>
                        <div
                            v-if="nodeItem.level === 0"
                            class="topo-node-filter"
                            @click="handleFilterEmptyToggle">
                            <Icon
                                v-if="isRenderEmptyTopoNode"
                                type="eye-slash-shape" />
                            <Icon
                                v-else
                                type="eye-shape" />
                        </div>
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
                            @check-change="handleTableCheckChange" />
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
    } from 'vue';

    import TableTab from '../../table-tab';
    import TableTabItem from '../../table-tab/item.vue';
    import RenderNodeTable from './render-node-table.vue';
    import RenderHostTable from './render-host-table.vue';

    import { getDirectChildrenNodesByNodeIds } from '../../../utils';

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

    const getNodeKey = node => `#${node.objectId}#${node.instanceId}`;
    
    const treeRef = ref();
    const renderTableType = ref('node');
    const isRenderEmptyTopoNode = ref(false);
    
    const nodeCheckedMap = shallowRef({});
    const allNodeList = shallowRef([]);

    const renderNodeList = shallowRef([]);
    const selectedTopoNode = shallowRef({});

    let isInnerChange = false;

    const renderTableCom = computed(() => tableComMap[renderTableType.value]);

    watch(() => props.lastNodeList, (lastNodeList) => {
        if (isInnerChange) {
            isInnerChange = false;
            return;
        }
        nodeCheckedMap.value = lastNodeList.reduce((result, item) => {
            result[getNodeKey(item)] = item;
            return result;
        }, {});
    }, {
        immediate: true,
    });

    const triggerChange = () => {
        // const values = Object.values(nodeCheckedMap.value).map(item => ({
        //     objectId: item.objectId,
        //     instanceId: item.instanceId,
        // }));
        isInnerChange = true;
        emits('change', 'node', Object.values(nodeCheckedMap.value));
    };

    const handleFilterEmptyToggle = () => {
        isRenderEmptyTopoNode.value = !isRenderEmptyTopoNode.value;
    };

    const handleNodeSelect = (node) => {
        renderTableType.value = 'node';
        selectedTopoNode.value = node.data.payload;
    };

    const handleCheckChange = (allCheckNodeId) => {
        // console.log('from handleCheckChangehandleCheckChange', allCheckNodeId);
        const allChildrenList = getDirectChildrenNodesByNodeIds(props.topoTreeData, allCheckNodeId || []);
        allNodeList.value = allChildrenList;
        // console.log('from handleCheckChange = ', allChildrenList);
    };

    const handleTableTypeChange = (type) => {
        renderTableType.value = type;
    };

    const handleTableCheckChange = (checkedMap) => {
        console.log('frm handleTableCheckChangehandleTableCheckChange = ', checkedMap);
        nodeCheckedMap.value = checkedMap;
        triggerChange();
    };
</script>
<style lang="postcss">
    .ip-selector-dynamic-topo {
        display: flex;
        height: 100%;

        .tree-box {
            width: 265px;
            height: 100%;
            padding-right: 15px;
            overflow: auto;
            border-right: 1px solid #dcdee5;

            .topo-node-box {
                display: flex;
                padding-right: 3px;
                font-size: 12px;
                align-items: center;
            }

            .topo-node-name {
                display: block;
            }

            .topo-node-filter {
                padding-left: 20px;
                color: #979ba5;
            }

            .topo-node-count {
                height: 16px;
                padding: 0 6px;
                margin-left: auto;
                line-height: 16px;
                color: #979ba5;
                background: #f0f1f5;
                border-radius: 2px;
            }
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
