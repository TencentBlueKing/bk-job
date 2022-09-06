<template>
    <div class="ip-selector-host-table">
        <div
            class="host-talbe-wrapper"
            :class="{
                'not-empty': data.length > 0,
            }"
            :style="styles">
            <table v-if="!isLoadingCustom">
                <thead>
                    <tr>
                        <th
                            v-if="slots.selection"
                            style="width: 60px;">
                            <slot name="header-selection" />
                        </th>
                        <template v-for="columnKey in columnKeySortList">
                            <th
                                v-if="columnKeyRenderMap[columnKey]"
                                :key="columnKey"
                                :style="{
                                    width: columnWidthCallback ?
                                        columnWidthCallback(columnKeyRenderList.indexOf(columnKey))
                                        : tableColumnConfig[columnKey].width,
                                }">
                                <div class="cell">
                                    <div class="cell-text">
                                        {{ tableColumnConfig[columnKey].name }}
                                    </div>
                                    <render-filter
                                        v-if="tableColumnConfig[columnKey].filter"
                                        :data="tableColumnConfig[columnKey].filter" />
                                </div>
                            </th>
                        </template>
                        <th
                            v-if="slots.action"
                            style="width: 100px;" />
                        <th
                            v-if="showSetting"
                            style="width: 40px;"
                            @click="handleShowSetting">
                            <div class="table-column-setting-btn">
                                <i class="bk-ipselector-icon bk-ipselector-set-fill" />
                            </div>
                        </th>
                    </tr>
                </thead>
                <tbody>
                    <tr
                        v-for="(hostDataItem, index) in data"
                        :key="index"
                        @click="handleRowClick(hostDataItem, index, $event)">
                        <td v-if="slots.selection">
                            <slot
                                name="selection"
                                v-bind:row="hostDataItem" />
                        </td>
                        <template v-for="(columnKey) in columnKeySortList">
                            <td
                                v-if="columnKeyRenderMap[columnKey]"
                                :key="columnKey">
                                <template v-if="columnKey === 'alive'">
                                    <agent-status :data="hostDataItem.alive" />
                                </template>
                                <template v-else>
                                    <div class="cell">
                                        <div class="cell-text">
                                            {{ getObjectValueByPath(hostDataItem, tableColumnConfig[columnKey].field) || '--' }}
                                        </div>
                                        <div class="cell-append">
                                            <template v-if="columnKey === 'ip'">
                                                <slot
                                                    name="ip"
                                                    v-bind:row="hostDataItem" />
                                            </template>
                                            <template v-if="columnKey === 'ipv6'">
                                                <slot
                                                    name="ipv6"
                                                    v-bind:row="hostDataItem" />
                                            </template>
                                        </div>
                                    </div>
                                </template>
                            </td>
                        </template>
                        <td v-if="slots.action">
                            <slot
                                name="action"
                                v-bind:row="item" />
                        </td>
                        <td
                            v-if="showSetting"
                            key="settting" />
                    </tr>
                </tbody>
            </table>
            <div
                v-if="data.length < 1"
                class="table-empty">
                <slot name="empty">
                    <img src="../../../images/empty.svg">
                    <div>暂无数据</div>
                </slot>
            </div>
        </div>
        <bk-pagination
            v-if="isShowPagination"
            small
            show-total-count
            :show-limit="false"
            align="right"
            v-bind="pagination"
            @change="handlePaginationChange"
            @limit-change="handlePaginationLimitChange" />
        <column-setting
            v-if="isShowSetting"
            :selected-list="columnKeyRenderList"
            :sort-list="columnKeySortList"
            @change="handleSettingChange"
            @close="handleSettingClose" />
    </div>
</template>
<script setup>
    import {
        ref,
        useSlots,
        computed,
        shallowRef,
    } from 'vue';
    import Manager from '../../../manager';
    import useHostRenderKey from '../../../hooks/use-host-render-key';
    import {
        makeMap,
        getObjectValueByPath,
     } from '../../../utils';
    import AgentStatus from '../../agent-status.vue';
    import ColumnSetting from './column-setting.vue';
    import tableColumnConfig from './column-config';
    import RenderFilter from './render-filter.vue';

    const slots = useSlots();

    const props = defineProps({
        data: {
            type: Array,
            required: true,
        },
        pagination: {
            type: Object,
        },
        height: {
            type: Number,
        },
        showSetting: {
            type: Boolean,
            default: true,
        },
        columnWidthCallback: {
            type: Function,
        },
    });

    const emits = defineEmits([
        'row-click',
        'pagination-change',
    ]);

    const CUSTOM_SETTINGS_MODULE = Manager.nameStyle('ipSelectorHostList');

    const isLoadingCustom = ref(true);
    // 列的显示顺序
    const columnKeySortList = shallowRef(Object.keys(tableColumnConfig));
    // 需要显示的列
    const columnKeyRenderList = shallowRef(['ip', 'ipv6', 'alive', 'osName']);
    // 需要显示的列
    const columnKeyRenderMap = computed(() => makeMap(columnKeyRenderList.value));
    const isShowSetting = ref(false);

    const styles = computed(() => {
        const styles = {};
        if (props.height > 0) {
            styles['max-height'] = `${props.height - 56}px`;
        }
        return styles;
    });

    const isShowPagination = computed(() => {
        if (!props.pagination) {
            return false;
        }
        if (props.pagination.count <= props.pagination.limit) {
            return false;
        }
        return true;
    });

    const { setKey } = useHostRenderKey();

    const setHostListRenderPrimaryKey = () => {
        // eslint-disable-next-line no-plusplus
        for (let i = 0; i < columnKeySortList.value.length; i++) {
            const curColumnKey = columnKeySortList.value[i];
            if (['ip', 'ipv6'].includes(curColumnKey)
                & columnKeyRenderList.value.includes(curColumnKey)) {
                setKey(curColumnKey);
                return;
            }
        }
        setKey('ip');
    };

    isLoadingCustom.value = true;
    // 获取用户自定义配置
    Manager.service.fetchCustomSettings({
        [Manager.nameStyle('moduleList')]: [CUSTOM_SETTINGS_MODULE],
    })
        .then((data) => {
            if (!data[CUSTOM_SETTINGS_MODULE]) {
                return;
            }
            const {
                hostListColumn = [],
                hostListColumnSort = [],
            } = data[CUSTOM_SETTINGS_MODULE];
            columnKeySortList.value = hostListColumnSort;
            columnKeyRenderList.value = hostListColumn;
            setHostListRenderPrimaryKey();
        })
        .finally(() => {
            isLoadingCustom.value = false;
        });
    
    // 显示设置弹框
    const handleShowSetting = () => {
        isShowSetting.value = true;
    };

    // 提交表格列表配置
    const handleSettingChange = (selectedList, sortList) => {
        columnKeyRenderList.value = selectedList;
        columnKeySortList.value = sortList;
        Manager.service.updateCustomSettings({
            [Manager.nameStyle('settingsMap')]: {
                [CUSTOM_SETTINGS_MODULE]: {
                    hostListColumn: columnKeyRenderList.value,
                    hostListColumnSort: columnKeySortList.value,
                },
            },
        });
        isShowSetting.value = false;
    };

    // 取消表格列配置
    const handleSettingClose = () => {
        isShowSetting.value = false;
    };

    // 选中行数
    const handleRowClick = (rowData, rowIndex, event) => {
        emits('row-click', rowData, rowIndex, event);
    };

    // 页码切换
    const handlePaginationChange = (current) => {
        emits('pagination-change', {
            ...props.pagination,
            current,
        });
    };

    // 每页条数切换
    const handlePaginationLimitChange = (limit) => {
        emits('pagination-change', {
            ...props.pagination,
            limit,
        });
    };
</script>
<style lang="postcss">
    @import "../../../styles/table.mixin.css";

    .ip-selector-host-table {
        position: relative;

        .host-talbe-wrapper {
            /* overflow-y: auto; */
            border-top: 1px solid #f0f1f5;

            &.not-empty {
                border-bottom: 1px solid #dcdee5;
            }

            table {
                tr:last-child {
                    td {
                        border-bottom: none;
                    }
                }
            }

            .table-empty {
                padding-top: 75px;
                padding-bottom: 25px;
                font-size: 12px;
                line-height: 20px;
                color: #63656e;
                text-align: center;
            }

            .table-column-setting-btn {
                display: flex;
                color: #c4c6cc;
                align-items: center;
                justify-content: center;
                cursor: pointer;
            }
        }

        @include table;
    }
</style>
