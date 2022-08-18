<template>
    <div class="ip-selector-dynamic-group">
        <div class="tree-box">
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
                    <bk-checkbox
                        :value="item.id"
                        @change="value => handleGroupCheck(item, value)" />
                    <span style="padding-left: 8px;">{{ item.name }}</span>
                </div>
            </div>
        </div>
        <div
            class="ip-table"
            v-bkloading="{ isLoading: isHostListLoading }">
            <bk-input
                placeholder="请输入 IP/IPv6/主机名称 或 选择条件搜索"
                style="margin-bottom: 12px;" />
            <render-host-table
                :data="hostTableData" />
        </div>
    </div>
</template>
<script setup>
    import {
        ref,
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

    const isLoading = ref(false);
    const isHostListLoading = ref(false);

    const groupList = shallowRef([]);
    const hostTableData = shallowRef([]);
    
    const groupCheckedMap = shallowRef({});

    const selectGroupId = ref();

    const fetchDynamicGroup = () => {
        isLoading.value = true;
        AppManageService.fetchDynamicGroup()
            .then((data) => {
                groupList.value = Object.freeze(data);
            })
            .finally(() => {
                isLoading.value = false;
            });
    };

    fetchDynamicGroup();

    const handleGroupSelect = (group) => {
        selectGroupId.value = group.id;
        isHostListLoading.value = true;
        AppManageService.fetchHostOfDynamicGroup({
            id: group.id,
        })
        .then((data) => {
            hostTableData.value = data[0].ipListStatus;
            console.log('from host lsit = ', data);
        })
        .finally(() => {
            isHostListLoading.value = false;
        });
    };

    const handleGroupCheck = (groupData, checked) => {
        console.log('from check = ', groupData, checked);
        const checkedMap = { ...groupCheckedMap.value };
        if (checked) {
            checkedMap[groupData.id] = groupData;
        } else {
            delete checkedMap[groupData.id];
        }

        groupCheckedMap.value = checkedMap;

        emits('change', 'group', Object.values(groupCheckedMap.value));
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

        .ip-table {
            flex: 1;
            padding-left: 24px;
        }
    }
</style>
