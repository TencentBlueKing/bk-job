<template>
    <div
        v-bkloading="{ isLoading: isDynamicGroupLoading }"
        class="ip-selector-dynamic-group">
        <resize-layout
            v-if="dynamicGroupList.length > 0"
            :default-width="265"
            flex-direction="left">
            <div class="tree-box">
                <bk-input
                    placeholder="搜索动态分组名称"
                    style="margin-bottom: 12px;" />
                <div class="dynamic-group-list">
                    <div
                        v-for="item in dynamicGroupList"
                        :key="item.id"
                        class="dynamic-group-item"
                        :class="{
                            active: selectGroupId === item.id
                        }"
                        @click="handleGroupSelect(item)">
                        <div @click.stop="">
                            <bk-checkbox
                                :checked="Boolean(dynamicGroupCheckedMap[item.id])"
                                :value="item.id"
                                @change="value => handleGroupCheck(item, value)" />
                        </div>
                        <div class="dynamic-group-name">
                            {{ item.name }}
                        </div>
                        <a
                            v-bk-tooltips="'跳转 CMDB 查看详情'"
                            class="dynamic-group-detail-link"
                            :href="`${config.bk_cmdb_dynamic_group_url}?page=1&filter=${item.name}`"
                            target="_blank">
                            <ip-selector-icon type="jump-link" />
                        </a>
                        <div
                            v-if="checkLastStatus(item)"
                            class="dynamic-group-tag">
                            最近更新
                        </div>
                        <div class="dynamic-group-tag">
                            <div
                                v-if="isDynamicGroupHostCountLoading"
                                class="bk-ip-selector-rotate-loading">
                                <ip-selector-icon
                                    svg
                                    type="loading" />
                            </div>
                            <span v-else>
                                {{ dynamicGroupHostCountMap[item.id] }}
                            </span>
                        </div>
                    </div>
                </div>
            </div>
            <template #right>
                <div
                    v-bkloading="{ isLoading: isHostListLoading }"
                    class="table-box">
                    <bk-input
                        placeholder="请输入 IP/IPv6/主机名称 或 选择条件搜索"
                        style="margin-bottom: 12px;" />
                    <render-host-table
                        :data="hostTableData"
                        :height="renderTableHeight"
                        :pagination="pagination"
                        @pagination-change="handlePaginationChange" />
                </div>
            </template>
        </resize-layout>
        <div
            v-else-if="!isDynamicGroupLoading"
            v-bkloading="{ isLoading: isConfigLoading }"
            class="create-dynamic-group">
            <span>无数据，</span>
            <a
                :href="config.bk_cmdb_dynamic_group_url"
                target="_blank">去创建</a>
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
        reactive,
        ref,
        shallowRef,
        watch,
    } from 'vue';

    import IpSelectorIcon from '../../../common/ip-selector-icon';
    import RenderHostTable from '../../../common/render-table/host/index.vue';
    import useDialogSize from '../../../hooks/use-dialog-size';
    import useFetchConfig from '../../../hooks/use-fetch-config';
    import Manager from '../../../manager';
    import { getPaginationDefault } from '../../../utils';
    import ResizeLayout from '../resize-layout.vue';

    const props = defineProps({
        topoTreeData: {
            type: Array,
            required: true,
        },
        lastDynamicGroupList: {
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
    
    const {
        loading: isConfigLoading,
        config,
    } = useFetchConfig();

    const isDynamicGroupLoading = ref(false);
    const isDynamicGroupHostCountLoading = ref(false);
    const isHostListLoading = ref(false);

    const dynamicGroupList = shallowRef([]);
    const hostTableData = shallowRef([]);
    const dynamicGroupHostCountMap = shallowRef({});
    
    const dynamicGroupCheckedMap = shallowRef({});

    const selectGroupId = ref();

    // 同步外部值
    watch(() => props.lastDynamicGroupList, (lastDynamicGroupList) => {
        dynamicGroupCheckedMap.value = lastDynamicGroupList.reduce((result, dynamicGroupItem) => {
            result[dynamicGroupItem.id] = dynamicGroupItem;
            return result;
        }, {});
    }, {
        immediate: true,
    });
    
    // 获取分组列表
    const fetchDynamicGroups = () => {
        isDynamicGroupLoading.value = true;
        Manager.service.fetchDynamicGroups()
            .then((data) => {
                dynamicGroupList.value = Object.freeze(data);
                if (data.length > 0) {
                    handleGroupSelect(data[0]);
                    // 异步获取分组的主机数
                    isDynamicGroupHostCountLoading.value = true;
                    Manager.service.fetchHostAgentStatisticsDynamicGroups({
                        [Manager.nameStyle('dynamicGroupList')]: data.map(item => ({
                            [Manager.nameStyle('id')]: item.id,
                            [Manager.nameStyle('meta')]: item.meta,
                        })),
                    })
                    .then((agentStatisticsData) => {
                        dynamicGroupHostCountMap.value = agentStatisticsData.reduce((result, item) => ({
                            ...result,
                            [item.dynamic_group.id]: item.agent_statistics.total_count,
                        }), {});
                    })
                    .finally(() => {
                        isDynamicGroupHostCountLoading.value = false;
                    });
                }
            })
            .finally(() => {
                isDynamicGroupLoading.value = false;
            });
    };

    fetchDynamicGroups();

    // 获取选中分组的主机列表
    const fetchDynamicGroupHostList = () => {
        isHostListLoading.value = true;
        Manager.service.fetchHostsDynamicGroup({
            [Manager.nameStyle('id')]: selectGroupId.value,
            [Manager.nameStyle('pageSize')]: pagination.limit,
            [Manager.nameStyle('start')]: (pagination.current - 1) * pagination.limit,
        })
        .then((data) => {
            hostTableData.value = data.data;
            pagination.count = data.total;
        })
        .finally(() => {
            isHostListLoading.value = false;
        });
    };

    const checkLastStatus = dynamicGroupData => new Date(dynamicGroupData.last_time).getTime() >= Date.now() - 86400000;

    // 查看分组的主机列表
    const handleGroupSelect = (group) => {
        selectGroupId.value = group.id;
        fetchDynamicGroupHostList();
    };

    // 选中分组
    const handleGroupCheck = (groupData, checked) => {
        const checkedMap = { ...dynamicGroupCheckedMap.value };
        if (checked) {
            checkedMap[groupData.id] = groupData;
        } else {
            delete checkedMap[groupData.id];
        }

        dynamicGroupCheckedMap.value = checkedMap;

        emits('change', 'dynamicGroupList', Object.values(dynamicGroupCheckedMap.value));
    };

    // 分页
    const handlePaginationChange = (currentPagination) => {
        pagination.current = currentPagination.current;
        pagination.limit = currentPagination.limit;
        fetchDynamicGroupHostList();
    };

</script>
<style lang="postcss">
    .ip-selector-dynamic-group {
        display: flex;
        height: 100%;

        .tree-box {
            height: 100%;
            padding-right: 16px;
            padding-left: 16px;
            overflow: auto;

            .dynamic-group-item {
                display: flex;
                height: 32px;
                padding-left: 4px;
                cursor: pointer;
                align-items: center;

                &.active {
                    color: #3a84ff;
                    background: #e1ecff;
                }

                &:hover {
                    background: #f0f1f5;

                    .dynamic-group-name {
                        margin-right: 5px;
                    }

                    .dynamic-group-detail-link {
                        display: block;
                    }

                    .dynamic-group-tag {
                        color: #fff;
                        background: #a3c5fd;
                    }
                }

                .dynamic-group-name {
                    height: 18px;
                    padding-left: 8px;
                    margin-right: auto;
                    overflow: hidden;
                    font-size: 12px;
                    line-height: 18px;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                    flex: 0 1 auto;
                }

                .dynamic-group-detail-link {
                    display: none;
                    margin-right: auto;
                    color: #3a84ff;
                }

                .dynamic-group-tag {
                    height: 18px;
                    padding: 0 6px;
                    margin-left: 5px;
                    font-size: 12px;
                    line-height: 18px;
                    color: #979ba5;
                    text-align: left;
                    word-break: keep-all;
                    background: #f0f1f5;
                    border-radius: 2px;
                }
            }
        }

        .table-box {
            flex: 1;
            padding-left: 24px;
        }

        .create-dynamic-group {
            width: 100%;
            padding-top: 120px;
            text-align: center;
        }
    }
</style>
