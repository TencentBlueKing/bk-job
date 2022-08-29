<template>
    <div class="ip-selector-dynamic-group">
        <div
            v-bkloading="{ isLoading: isDynamicGroupLoading }"
            class="tree-box">
            <bk-input
                placeholder="搜索拓扑节点"
                style="margin-bottom: 12px;" />
            <div class="dynamic-group-list">
                <div
                    v-for="item in groupList"
                    :key="item.id"
                    class="dynamic-group-item"
                    :class="{ active: selectGroupId === item.id }"
                    @click="handleGroupSelect(item)">
                    <span @click.stop="">
                        <bk-checkbox
                            :value="item.id"
                            @change="value => handleGroupCheck(item, value)" />
                    </span>
                    <span style="padding-left: 8px;">{{ item.name }}</span>
                </div>
            </div>
        </div>
        <div
            class="table-box"
            v-bkloading="{ isLoading: isHostListLoading }">
            <bk-input
                placeholder="请输入 IP/IPv6/主机名称 或 选择条件搜索"
                style="margin-bottom: 12px;" />
            <render-host-table
                :data="hostTableData"
                :pagination="pagination"
                :height="renderTableHeight"
                @pagination-change="handlePaginationChange" />
        </div>
    </div>
</template>
<script setup>
    import {
        watch,
        ref,
        shallowRef,
        reactive,
    } from 'vue';

    import AppManageService from '@service/app-manage';
    import useDialogSize from '../../../hooks/use-dialog-size';
    import { getPaginationDefault } from '../../../utils';
    import RenderHostTable from '../../../common/render-table/host.vue';

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

    const isDynamicGroupLoading = ref(false);
    const isHostListLoading = ref(true);

    const groupList = shallowRef([]);
    const hostTableData = shallowRef([]);
    
    const groupCheckedMap = shallowRef({});

    const selectGroupId = ref();

    // 同步外部值
    watch(() => props.lastDynamicGroupList, (lastDynamicGroupList) => {
        groupCheckedMap.value = lastDynamicGroupList.reduce((result, dynamicGroupItem) => {
            result[dynamicGroupItem.id] = dynamicGroupItem;
            return result;
        }, {});
    }, {
        immediate: true,
    });
    
    // 获取分组列表
    const fetchDynamicGroup = () => {
        isDynamicGroupLoading.value = true;
        AppManageService.fetchDynamicGroup()
            .then((data) => {
                groupList.value = Object.freeze(data);
                if (data.length > 0) {
                    handleGroupSelect(data[0]);
                }
            })
            .finally(() => {
                isDynamicGroupLoading.value = false;
            });
    };

    fetchDynamicGroup();

    // 获取选中分组的主机列表
    const fetchDynamicGroupHostList = () => {
        isHostListLoading.value = true;
        AppManageService.fetchDynamicGroupHost({
            id: selectGroupId.value,
            pageSize: pagination.limit,
            start: (pagination.current - 1) * pagination.limit,
        })
        .then((data) => {
            hostTableData.value = data.data;
            pagination.count = data.total;
        })
        .finally(() => {
            isHostListLoading.value = false;
        });
    };

    // 查看分组的主机列表
    const handleGroupSelect = (group) => {
        selectGroupId.value = group.id;
        fetchDynamicGroupHostList();
    };

    // 选中分组
    const handleGroupCheck = (groupData, checked) => {
        const checkedMap = { ...groupCheckedMap.value };
        if (checked) {
            checkedMap[groupData.id] = groupData;
        } else {
            delete checkedMap[groupData.id];
        }

        groupCheckedMap.value = checkedMap;

        emits('change', 'dynamicGroupList', Object.values(groupCheckedMap.value));
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
            width: 265px;
            height: 100%;
            padding-right: 15px;
            overflow: auto;
            border-right: 1px solid #dcdee5;

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
                }
            }
        }

        .table-box {
            flex: 1;
            padding-left: 24px;
        }
    }
</style>
