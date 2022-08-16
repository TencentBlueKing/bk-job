<template>
    <div v-bkloading="{ isLoading }">
        <bk-input
            placeholder="请输入 IP/IPv6/主机名称 或 选择条件搜索"
            style="margin: 12px 0;" />
        <render-table
            :data="tableData"
            :pagination="pagination"
            :agent-static="nodeAgentStaticMap"
            @row-click="handleRowClick">
            <template #selection="{ row }">
                <bk-checkbox :value="Boolean(checkedMap[getNodeKey(row)])" />
            </template>
        </render-table>
    </div>
</template>
<script setup>
    import {
        ref,
        reactive,
        shallowRef,
        watch,
    } from 'vue';
    import _ from 'lodash';
    import AppManageService from '@service/app-manage';
    import RenderTable from '../../render-table/node.vue';

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

    const getNodeKey = node => `#${node.objectId}#${node.instanceId}`;

    const isLoading = ref(false);
    const pagination = reactive({
        count: 0,
        current: 1,
        limit: 10,
    });

    const tableData = shallowRef([]);
    const nodeAgentStaticMap = shallowRef({});

    const fetchData = () => {
        isLoading.value = true;
        const params = props.node.child.map(item => ({
            objectId: item.objectId,
            instanceId: item.instanceId,
        }));
        AppManageService.fetchNodePath(params)
            .then((data) => {
                tableData.value = data.reduce((result, item) => {
                    const namePath = item.map(({ instanceName }) => instanceName).join('/');
                    const tailNode = _.last(item);
                    result.push({
                        key: getNodeKey(tailNode),
                        node: tailNode,
                        namePath,
                    });
                    return result;
                }, []);
            })
            .finally(() => {
                isLoading.value = false;
            });
        AppManageService.fetchBatchNodeAgentStatistics({
            nodeList: params,
        })
            .then((data) => {
                const staticMap = {};
                data.forEach((item) => {
                    staticMap[getNodeKey(item.node)] = item.agentStatistics;
                });
                nodeAgentStaticMap.value = staticMap;
            });
    };

    watch(() => props.data, () => {
        fetchData();
    }, {
        immediate: true,
    });

    const handleRowClick = (nodeData) => {
        const nodeKey = getNodeKey(nodeData);
        const checkedMap = { ...props.checkedMap };
        if (checkedMap[nodeKey]) {
            delete checkedMap[nodeKey];
        } else {
            checkedMap[nodeKey] = nodeData;
        }
        console.log('row click == =', nodeData, props.checkedMap, checkedMap);

        emits('check-change', checkedMap);
    };
</script>
