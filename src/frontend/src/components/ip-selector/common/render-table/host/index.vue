<template>
    <div class="ip-selector-host-table">
        <div class="table-wrapper">
            <div
                ref="tableContentRef"
                class="talbe-content"
                :class="{
                    'not-empty': data.length > 0,
                }"
                :style="styles">
                <table v-if="!isLoadingCustom">
                    <thead>
                        <tr>
                            <th
                                v-if="slots.selection"
                                class="columu-fixed"
                                style="width: 60px;">
                                <slot name="header-selection" />
                            </th>
                            <template
                                v-for="(columnKey) in columnKeySortList">
                                <th
                                    v-if="columnKeyRenderMap[columnKey]"
                                    :key="columnKey"
                                    :class="{
                                        'columu-fixed': columnKey === firstRenderColumnKey,
                                    }"
                                    :style="{
                                        width: columnWidthCallback ?
                                            columnWidthCallback(columnKeyRenderList.indexOf(columnKey))
                                            : tableColumnConfig[columnKey].width,
                                        left: `${slots.selection ? 60: 0}px`,
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
                                style="width: 60px;" />
                            <th
                                v-if="showSetting"
                                style="width: 40px;">
                                <column-setting
                                    :selected-list="columnKeyRenderList"
                                    :sort-list="columnKeySortList"
                                    @change="handleSettingChange">
                                    <div class="table-column-setting-btn">
                                        <ip-selector-icon type="set-fill" />
                                    </div>
                                </column-setting>
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr
                            v-for="(hostDataItem, rowIndex) in data"
                            :key="rowIndex"
                            @click="handleRowClick(hostDataItem, rowIndex, $event)">
                            <td
                                v-if="slots.selection"
                                class="columu-fixed">
                                <slot
                                    name="selection"
                                    :row="hostDataItem" />
                            </td>
                            <template
                                v-for="(columnKey) in columnKeySortList">
                                <td
                                    v-if="columnKeyRenderMap[columnKey]"
                                    :key="columnKey"
                                    :class="{
                                        'columu-fixed': columnKey === firstRenderColumnKey,
                                    }"
                                    :style="{
                                        left: `${slots.selection ? 60: 0}px`,
                                    }">
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
                                                        :row="hostDataItem" />
                                                </template>
                                                <template v-if="columnKey === 'ipv6'">
                                                    <slot
                                                        name="ipv6"
                                                        :row="hostDataItem" />
                                                </template>
                                            </div>
                                        </div>
                                    </template>
                                </td>
                            </template>
                            <td v-if="slots.action">
                                <slot
                                    name="action"
                                    :row="hostDataItem" />
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
            <div
                class="table-fixed"
                :style="fixedStyles" />
        </div>
        <bk-pagination
            v-if="isShowPagination"
            align="right"
            :show-limit="false"
            show-total-count
            small
            v-bind="pagination"
            @change="handlePaginationChange"
            @limit-change="handlePaginationLimitChange" />
    </div>
</template>
<script>
    import _ from 'lodash';
    import {
        computed,
        onBeforeUnmount,
        onMounted,
        ref,
        shallowRef,
        useSlots,
    } from 'vue';

    import IpSelectorIcon from '../../../common/ip-selector-icon';
    import useHostRenderKey from '../../../hooks/use-host-render-key';
    import Manager from '../../../manager';
    import {
        getObjectValueByPath,
        makeMap,
     } from '../../../utils';
    import AgentStatus from '../../agent-status.vue';

    import tableColumnConfig from './column-config';
    import ColumnSetting from './column-setting.vue';
    import RenderFilter from './render-filter.vue';

    const CUSTOM_SETTINGS_MODULE = Manager.nameStyle('ipSelectorHostList');

    // 列的显示顺序
    const columnKeySortList = shallowRef(Object.keys(tableColumnConfig));
    // 需要显示的列
    const columnKeyRenderList = shallowRef(['ip', 'ipv6', 'alive', 'osName']);
    // 需要显示的列
    const columnKeyRenderMap = computed(() => makeMap(columnKeyRenderList.value));

    export default {
        inheritAttrs: false,
    };
</script>
<script setup>
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

    const slots = useSlots();

    const tableContentRef = ref();
    const isLoadingCustom = ref(true);

    const styles = computed(() => {
        const styles = {};
        if (props.height > 0) {
            styles['max-height'] = `${props.height - 56}px`;
        }
        return styles;
    });

    const fixedStyles = shallowRef({});

    const firstRenderColumnKey = computed(() => {
        // eslint-disable-next-line no-plusplus
        for (let i = 0; i < columnKeySortList.value.length; i++) {
            const columnKey = columnKeySortList.value[i];
            if (columnKeyRenderMap.value[columnKey]) {
                return columnKey;
            }
        }
        return '';
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
                && columnKeyRenderList.value.includes(curColumnKey)) {
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
        setHostListRenderPrimaryKey();
        handleHorizontalScroll();
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

    const handleHorizontalScroll = _.throttle(() => {
        const { scrollLeft } = tableContentRef.value;
        if (scrollLeft === 0) {
            fixedStyles.value = {
                display: 'none',
            };
            return;
        }
        const fixedColumns = tableContentRef.value.querySelectorAll('th.columu-fixed');
        const fixedWidth = Array.from(fixedColumns).reduce((result, itemEl) => result + itemEl.getBoundingClientRect().width, 0);
        fixedStyles.value = {
            width: `${fixedWidth}px`,
        };
    }, 30);

    onMounted(() => {
        tableContentRef.value.addEventListener('scroll', handleHorizontalScroll);
        onBeforeUnmount(() => {
            tableContentRef.value.removeEventListener('scroll', handleHorizontalScroll);
        });
    });
</script>
<style lang="postcss">
    @import "../../../styles/table.mixin.css";

    .ip-selector-host-table {
        position: relative;
        overflow: hidden;

        .table-wrapper {
            position: relative;
        }

        .talbe-content {
            position: relative;
            overflow-x: auto;
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

                th,
                td {
                    &:first-child {
                        position: sticky;
                        left: 0;
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

        .table-fixed {
            position: absolute;
            top: 0;
            bottom: 0;
            left: 0;
            pointer-events: none;
            box-shadow: 0 0 10px rgb(0 0 0 / 12%);
        }

        @include table;
    }
</style>
