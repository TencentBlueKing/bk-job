<template>
    <div class="ip-selector-view-node">
        <collapse-box>
            <template #title>
                <span style="font-weight: bold;">【动态拓扑】</span>
                <span>
                    - 共
                    <span class="bk-ip-selector-number">131231</span>
                    个，新增
                    <span class="bk-ip-selector-number-success">131231</span>
                    个，删除
                    <span class="bk-ip-selector-number-error">131231</span>
                    个
                </span>
            </template>
            <table>
                <tr
                    v-for="row in tableData"
                    :key="row.id">
                    <td>{{ row.namePath }}</td>
                    <td>
                        <render-agent-statistics :data="nodeAgentStaticMap[genNodeKey(row.node)]" />
                    </td>
                </tr>
            </table>
        </collapse-box>
    </div>
</template>
<script setup>
    import CollapseBox from './components/collapse-box/index.vue';
    import _ from 'lodash';
    import {
        ref,
        shallowRef,
        watch,
        reactive,
    } from 'vue';
    import AppManageService from '@service/app-manage';
    import { genNodeKey } from '../utils';
    import RenderAgentStatistics from './components/agent-statistics';

    const props = defineProps({
        data: {
            type: Array,
            required: true,
        },
    });
    // const emits = defineEmits(['change']);

    const isLoading = ref(false);
    const tableData = shallowRef([]);
    const nodeAgentStaticMap = shallowRef({});

    const pagination = reactive({
        count: 0,
        current: 1,
        limit: 10,
    });
    console.log('print pagination = ', pagination);

    const fetchData = () => {
        isLoading.value = true;
        AppManageService.fetchNodePath(props.data)
            .then((data) => {
                tableData.value = data.reduce((result, item) => {
                    const namePath = item.map(({ instanceName }) => instanceName).join('/');
                    const tailNode = _.last(item);
                    result.push({
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
            nodeList: props.data,
        })
            .then((data) => {
                const staticMap = {};
                data.forEach((item) => {
                    staticMap[genNodeKey(item.node)] = item.agentStatistics;
                });
                nodeAgentStaticMap.value = staticMap;
            });
    };

    watch(() => props.data, () => {
        if (props.data.length > 0) {
            fetchData();
        } else {
            tableData.value = [];
        }
    }, {
        immediate: true,
    });

    // const handleRemove = (removeTarget) => {
    //     const result = props.data.reduce((result, item) => {
    //         if (removeTarget !== item) {
    //         result.push(item);
    //         }
    //         return result;
    //     }, []);

    //     emits('change', 'node', result);
    // };

    // const handlRemoveAll = () => {
    //     emits('change', 'node', []);
    // };
</script>
<style lang="postcss">
    @import "../styles/table-mixin.css";

    .ip-selector-view-node {
        @include table;
    }
</style>
