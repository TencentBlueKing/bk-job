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
                <thead id="bkIPSelectorHostTableHead">
                    <tr>
                        <th
                            v-if="slots['header-selection']"
                            class="columu-fixed"
                            data-width="60px"
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
                            data-width="60px"
                            :style="{
                                top: 0,
                                right: 0,
                                width: '60px',
                                right: showSetting ? '40px' : 0
                            }">
                            <slot name="action" />
                            操作
                        </th>
                        <th
                            v-if="showSetting"
                            class="columu-fixed-right"
                            data-width="40px"
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

    // 列的显示顺序
    export const columnKeySortList = shallowRef([]);
    // 默认需要显示的列
    export const columnKeyRenderList = shallowRef([
        'ip',
        'ipv6',
        'alive',
        'osName',
    ]);
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

    // eslint-disable-next-line no-underscore-dangle
    window.__bk_ip_selector__ = {
        columnKeySortList,
        columnKeyRenderList,
        columnKeyRenderMap,
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

    const CUSTOM_SETTINGS_MODULE = Manager.nameStyle(`ipSelectorHostList${Manager.config.version}`);

    const {
        hostTableRenderColumnList,
        hostTableCustomColumnList,
    } = Manager.config;
    // 配置的自定义列
    if (hostTableCustomColumnList) {
        const keySortList = Object.keys(tableColumnConfig);
        hostTableCustomColumnList.forEach((columnConfig) => {
            if (columnConfig.index) {
                keySortList.splice(columnConfig.index, 0, columnConfig.key);
            } else {
                keySortList.push(columnConfig.key);
            }
        });
        columnKeySortList.value = keySortList;
    }
    // 默认显示的列表
    if (hostTableRenderColumnList.length > 0) {
        columnKeyRenderList.value = [...hostTableRenderColumnList];
        const keyRenderMap = makeMap(hostTableRenderColumnList);
        const defaultSortList = columnKeySortList.value.reduce((result, columnKey) => {
            if (!keyRenderMap[columnKey]) {
                result.push(columnKey);
            }
            return result;
        }, []);
        columnKeySortList.value = [...hostTableRenderColumnList, ...defaultSortList];
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
        const {
            hostListColumn = [],
            hostListColumnSort = [],
        } = data[CUSTOM_SETTINGS_MODULE];

        if (hostListColumnSort.length === columnKeySortList.value.length) {
            columnKeySortList.value = hostListColumnSort;
            columnKeyRenderList.value = hostListColumn;
        }
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

    defineExpose({
        initalScroll,
    });
</script>
<style lang="postcss">
    @import "../../../../styles/table.mixin.css";
    @import "../../../../styles/scroller.mixin.css";

    .render-table-host-content {
        position: relative;

        .talbe-content {
            position: relative;
            overflow-x: auto;
            border-top: 1px solid #f0f1f5;

            @include scroller;

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
                position: sticky;
                top: 0;
                left: 0;
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
