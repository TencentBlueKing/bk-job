<template>
    <div
        v-bkloading="{ isLoading }"
        class="ip-selector-view-dynamic-group">
        <collapse-box>
            <template #title>
                <span style="font-weight: bold;">【动态分组】</span>
                <span>
                    - 共
                    <span class="bk-ip-selector-number">{{ validDynamicGroupList.length }}</span>
                    个
                </span>
                <span v-if="newDynamicGroupNum">
                    ，新增
                    <span class="bk-ip-selector-number-success">{{ newDynamicGroupNum }}</span>
                    个
                </span>
                <span v-if="removedDynamicGroupList.length">
                    ，删除
                    <span class="bk-ip-selector-number-error">{{ removedDynamicGroupList.length }}</span>
                    个
                </span>
            </template>
            <template
                v-if="!context.readonly"
                #action>
                <extend-action>
                    <div @click="handleRemoveAll">
                        清除所有
                    </div>
                </extend-action>
            </template>
            <table>
                <tr
                    v-for="row in renderData"
                    :key="row.id"
                    :class="diffMap[row.id]">
                    <td style="width: 30%;">
                        <div class="cell">
                            <div class="cell-text">
                                {{ row.name || `#${row.id}` }}
                            </div>
                            <diff-tag :value="diffMap[row.id]" />
                        </div>
                    </td>
                    <td>
                        <render-agent-statistics
                            :data="agentStaticMap[row.id]"
                            :loading="isAgentStatisticsLoading"
                            @select="handleShowHostList(row)" />
                    </td>
                    <td
                        v-if="!context.readonly"
                        style="width: 60px;">
                        <bk-button
                            v-if="diffMap[row.id] !== 'remove'"
                            text
                            theme="primary"
                            @click="handleRemove(row)">
                            删除
                        </bk-button>
                    </td>
                </tr>
            </table>
            <div
                v-if="isShowPagination"
                style="padding: 0 10px 8px 0;">
                <bk-pagination
                    style="margin-top: 8px;"
                    v-bind="pagination"
                    @change="handlePaginationCurrentChange"
                    @limit-change="handlePaginationLimitChange" />
            </div>
        </collapse-box>
        <bk-dialog
            v-model="isShowHostList"
            :draggable="false"
            header-position="left"
            :title="`动态拓扑主机预览`"
            :width="dialogWidth">
            <host-list
                v-if="selectedDynamicGroup"
                :dynamic-group="selectedDynamicGroup" />
            <template #footer>
                <bk-button
                    theme="primary"
                    @click="handleHideHostList">
                    关闭
                </bk-button>
            </template>
        </bk-dialog>
    </div>
</template>
<script setup>
    import {
        ref,
        shallowRef,
        watch,
    } from 'vue';

    import DiffTag from '../common/diff-tag.vue';
    import ExtendAction from '../common/extend-action.vue';
    import useDialogSize from '../hooks/use-dialog-size';
    import useIpSelector from '../hooks/use-ip-selector';
    import useLocalPagination from '../hooks/use-local-pagination';
    import Manager from '../manager';
    import {
        getDynamicGroupDiffMap,
        getInvalidDynamicGroupList,
        getRemoveDynamicGroupList,
        groupDynamicGroupList,
    } from '../utils';

    import RenderAgentStatistics from './components/agent-statistics.vue';
    import CollapseBox from './components/collapse-box/index.vue';
    import HostList from './components/dynamic-group-host-list.vue';

    const props = defineProps({
        data: {
            type: Array,
            required: true,
        },
    });
    const emits = defineEmits(['change']);

    const isLoading = ref(false);
    const isAgentStatisticsLoading = ref(false);
    const tableData = shallowRef([]);
    const agentStaticMap = shallowRef({});
    const validDynamicGroupList = shallowRef([]);
    const removedDynamicGroupList = shallowRef([]);
    const invalidDynamicGroupList = shallowRef([]);
    const newDynamicGroupNum = ref(0);

    const resultList = shallowRef([]);

    const isShowHostList = ref(false);
    const selectedDynamicGroup = shallowRef();

    let isInnerChange = false;

    const context = useIpSelector();
    const diffMap = shallowRef({});

    const {
        isShowPagination,
        pagination,
        data: renderData,
        handlePaginationCurrentChange,
        handlePaginationLimitChange,
    } = useLocalPagination(tableData);

    const {
        width: dialogWidth,
    } = useDialogSize();

    // 根据 ID 获取组件详情
    const fetchData = () => {
        isLoading.value = true;
        const params = {
            [Manager.nameStyle('dynamicGroupList')]: props.data.map(item => ({
                [Manager.nameStyle('id')]: item.id,
                [Manager.nameStyle('meta')]: item.meta,
            })),
        };
        Manager.service.fetchDynamicGroups(params)
            .then((data) => {
                validDynamicGroupList.value = data;
            })
            .finally(() => {
                isLoading.value = false;
            });
        isAgentStatisticsLoading.value = true;
        Manager.service.fetchHostAgentStatisticsDynamicGroups(params)
            .then((data) => {
                agentStaticMap.value = data.reduce((result, item) => {
                    result[item.dynamic_group.id] = item.agent_statistics;
                    return result;
                }, {});
            })
            .finally(() => {
            isAgentStatisticsLoading.value = false;
            });
    };

    watch(() => props.data, () => {
        if (isInnerChange) {
            isInnerChange = false;
            return;
        }
        if (props.data.length > 0) {
            fetchData();
        } else {
            tableData.value = [];
        }
    }, {
        immediate: true,
    });

    watch([validDynamicGroupList, resultList], () => {
        invalidDynamicGroupList.value = getInvalidDynamicGroupList(props.data, validDynamicGroupList.value);

        removedDynamicGroupList.value = getRemoveDynamicGroupList(props.data, context.originalValue);
        diffMap.value = getDynamicGroupDiffMap(props.data, context.originalValue, invalidDynamicGroupList.value);

        const {
            newList,
            originalList,
        } = groupDynamicGroupList(validDynamicGroupList.value, diffMap.value);

        newDynamicGroupNum.value = newList.length;

        tableData.value = [
            ...invalidDynamicGroupList.value,
            ...newList,
            ...removedDynamicGroupList.value,
            ...originalList,
        ];
        pagination.count = tableData.value.length;
    });

    const triggerChange = () => {
        isInnerChange = true;
        emits('change', 'dynamicGroupList', resultList.value);
    };

    // 删除指定动态分组
    const handleRemove = (dynamicGroupData) => {
        resultList.value = props.data.reduce((result, item) => {
            if (item.id !== dynamicGroupData.id) {
                result.push(item);
            }
            return result;
        }, []);
        if (diffMap.value[dynamicGroupData.id] !== 'invalid') {
            validDynamicGroupList.value = validDynamicGroupList.value.reduce((result, item) => {
                if (item.id !== dynamicGroupData.id) {
                    result.push(item);
                }
                return result;
            }, []);
        }
        triggerChange();
    };
    
    // 移除所有
    const handleRemoveAll = () => {
        resultList.value = [];
        validDynamicGroupList.value = [];
        triggerChange();
    };

    // 查看分组的主机列表
    const handleShowHostList = (group) => {
        isShowHostList.value = true;
        selectedDynamicGroup.value = group;
    };

    const handleHideHostList = () => {
        isShowHostList.value = false;
    };

    defineExpose({
        refresh () {
            fetchData();
        },
    });
</script>
<style lang="postcss">
    @import "../styles/table.mixin.css";

    .ip-selector-view-dynamic-group {
        @include table;
    }
</style>
