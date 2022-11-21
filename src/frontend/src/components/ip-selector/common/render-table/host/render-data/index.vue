<template>
    <div class="render-table-host-content">
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
                            v-if="slots['header-selection']"
                            class="columu-fixed"
                            style="width: 60px;">
                            <slot name="header-selection" />
                        </th>
                        <template v-for="(columnKey) in columnKeySortList">
                            <RenderHead
                                v-if="columnKeyRenderMap[columnKey]"
                                :key="columnKey"
                                :class="{
                                    'columu-fixed': columnKey === firstRenderColumnKey,
                                    [`host-column-first-key`]: columnKey === firstRenderColumnKey
                                }"
                                :column-key="columnKey"
                                :column-width-callback="columnWidthCallback"
                                :index="columnKeyRenderList.indexOf(columnKey)"
                                :style="{
                                    left: `${slots['header-selection'] ? 60: 0}px`,
                                }"
                                @mousedown="(event) => handleMouseDown(event, columnKey)"
                                @mousemove="handleMouseMove" />
                        </template>
                        <th
                            v-if="slots.action"
                            class="columu-fixed-right"
                            style="top: 0; right: 0; width: 60px;">
                            <slot name="action" />
                        </th>
                        <th
                            v-if="showSetting"
                            class="columu-fixed-right"
                            style="top: 0; right: 0; width: 40px; padding: 0;">
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
                <slot />
            </table>
            <div
                v-if="data.length < 1"
                class="table-empty">
                <slot name="empty">
                    <img src="../../../../images/empty.svg">
                    <div>暂无数据</div>
                </slot>
            </div>
        </div>
        <div
            class="table-fixed-left"
            :style="leftFixedStyles" />
        <div
            class="table-fixed-right"
            :style="rightFixedStyles" />
        <div
            ref="tableColumnResizeRef"
            class="table-column-resize" />
    </div>
</template>
<script>
    import {
        computed,
        nextTick,
        onMounted,
        ref,
        shallowRef,
        useSlots,
    } from 'vue';

    import useHostRenderKey from '../../../../hooks/use-host-render-key';
    import Manager from '../../../../manager';
    import { makeMap } from '../../../../utils';
    import IpSelectorIcon from '../../../ip-selector-icon';
    import tableColumnConfig from '../column-config';

    import ColumnSetting from './components/column-setting.vue';
    import RenderHead from './components/render-th.vue';
    import useResizeEvent from './hooks/use-resize-event.js';
    import useScroll from './hooks/use-scroll';

    export const CUSTOM_SETTINGS_MODULE = Manager.nameStyle('ipSelectorHostList');

    // 列的显示顺序
    export const columnKeySortList = shallowRef([]);
    // 默认需要显示的列
    export const columnKeyRenderList = shallowRef(['ip', 'ipv6', 'alive', 'osName']);
    // 默认需要显示的列
    export const columnKeyRenderMap = computed(() => makeMap(columnKeyRenderList.value));

    export const firstRenderColumnKey = computed(() => {
        // eslint-disable-next-line no-plusplus
        for (let i = 0; i < columnKeySortList.value.length; i++) {
            const columnKey = columnKeySortList.value[i];
            if (columnKeyRenderMap.value[columnKey]) {
                return columnKey;
            }
        }
        return '';
    });

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
        height: {
            type: Number,
        },
        columnWidthCallback: {
            type: Function,
        },
        showSetting: {
            type: Boolean,
        },
    });

    const {
        hostTableColumnSortList,
        hostTableColumns,
    } = Manager.config;
    if (hostTableColumns) {
        const keySortList = Object.keys(tableColumnConfig);
        hostTableColumns.forEach((columnConfig) => {
            if (columnConfig.index) {
                keySortList.splice(columnConfig.index, 0, columnConfig.key);
            } else {
                keySortList.push(columnConfig.key);
            }
        });
        columnKeySortList.value = keySortList;
    }
    if (hostTableColumnSortList.length > 0) {
        columnKeyRenderList.value = [...hostTableColumnSortList];
    }

    const slots = useSlots();

    const { setKey } = useHostRenderKey();

    const isLoadingCustom = ref(true);

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

    const styles = computed(() => {
        const styles = {};
        if (props.height > 0) {
            styles['max-height'] = `${props.height - 56}px`;
        }
        return styles;
    });

    const tableContentRef = ref();
    const tableColumnResizeRef = ref();

    const {
        leftFixedStyles,
        rightFixedStyles,
        initalScroll,
    } = useScroll(tableContentRef);

    const {
        initColumnWidth,
        handleMouseMove,
        handleMouseDown,
     } = useResizeEvent(tableContentRef, tableColumnResizeRef);

     // 获取用户自定义配置
    Manager.service.fetchCustomSettings({
        [Manager.nameStyle('moduleList')]: [CUSTOM_SETTINGS_MODULE],
    }).then((data) => {
        if (!data[CUSTOM_SETTINGS_MODULE]) {
            return;
        }
        // const {
        //     hostListColumn = [],
        //     hostListColumnSort = [],
        // } = data[CUSTOM_SETTINGS_MODULE];

        // columnKeySortList.value = hostListColumnSort;
        // columnKeyRenderList.value = hostListColumn;
        setHostListRenderPrimaryKey();
    })
    .finally(() => {
        isLoadingCustom.value = false;
        nextTick(() => {
            initColumnWidth();
            setTimeout(() => {
                initalScroll();
            });
        });
    });

    const handleSettingChange = (renderList, sortList) => {
        columnKeyRenderList.value = renderList;
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
        initalScroll();
     };

    onMounted(() => {
        initColumnWidth();
    });

    defineExpose({
        initalScroll,
    });
</script>
<style lang="postcss">
    @import url("../../../../styles/table.mixin.css");

    .render-table-host-content {
        position: relative;

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

        .table-fixed-left {
            position: absolute;
            top: 0;
            bottom: 0;
            left: 0;
            pointer-events: none;
            box-shadow: 0 0 10px rgb(0 0 0 / 12%);
        }

        .table-fixed-right {
            position: absolute;
            top: 0;
            right: 0;
            bottom: 0;
            pointer-events: none;
            box-shadow: 0 0 10px rgb(0 0 0 / 12%);
        }

        .table-column-resize {
            position: absolute;
            top: 0;
            bottom: 0;
            display: none;
            width: 1px;
            background: #dfe0e5;
        }

        @include table;
    }
</style>
