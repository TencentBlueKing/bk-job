<template>
    <div class="ip-selector-static-topo">
        <div class="tree-box">
            <bk-input
                placeholder="搜索拓扑节点"
                style="margin-bottom: 12px;" />
            <bk-big-tree
                ref="treeRef"
                :data="topoTreeData"
                show-link-line
                selectable
                :expand-on-click="false"
                @select-change="handleNodeSelect">
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
            <bk-input
                placeholder="请输入 IP/IPv6/主机名称 或 选择条件搜索"
                style="margin-bottom: 12px;" />
            <render-host-table
                :data="hostTableData"
                :pagination="pagination"
                @row-click="handleRowClick">
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
    } from 'vue';

    import AppManageService from '@service/app-manage';

    import RenderHostTable from '../render-table/host.vue';

    defineProps({
        topoTreeData: {
            type: Array,
            required: true,
        },
    });
    const emits = defineEmits([
        'change',
    ]);
    const treeRef = ref();
    const isRenderEmptyTopoNode = ref(false);
    const hostTableData = shallowRef([]);
    const pagination = reactive({
        count: 0,
        current: 1,
        limit: 10,
    });
    const hostCheckedMap = shallowRef({});

    let selectedTopoNode;

    const triggerChange = () => {
        emits('change', 'host', Object.values(hostCheckedMap.value));
    };

    const fetchNodeHostList = () => {
        AppManageService.fetchTopologyHost({
            appTopoNodeList: [{
                objectId: selectedTopoNode.objectId,
                instanceId: selectedTopoNode.instanceId,
            },
            ],
            pageSize: pagination.limit,
            start: (pagination.current - 1) * pagination.limit,
        }).then((data) => {
            hostTableData.value = data.data;
            pagination.count = data.total;
        });
    };

    const handleFilterEmptyToggle = () => {
        isRenderEmptyTopoNode.value = !isRenderEmptyTopoNode.value;
    };

    const handleNodeSelect = (node) => {
        selectedTopoNode = node.data.payload;
        fetchNodeHostList();
    };

    const handleRowClick = (data) => {
        const checkedMap = { ...hostCheckedMap.value };
        if (checkedMap[data.hostId]) {
            delete checkedMap[data.hostId];
        } else {
            checkedMap[data.hostId] = {
                hostId: data.hostId,
            };
        }
        hostCheckedMap.value = checkedMap;
        triggerChange();
    };
</script>
<style lang="postcss">
    .ip-selector-static-topo {
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
        }
    }
</style>
