<template>
    <div class="ip-selector-host-table">
        <div
            class="host-talbe-wrapper"
            :class="{ 'not-empty': data.length > 0 }"
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
                                    width: columnWidthCallback ? columnWidthCallback(columnKeyRenderList.indexOf(columnKey)) : tableColumnConfig[columnKey].width,
                                }">
                                {{ tableColumnConfig[columnKey].name }}
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
                        v-for="(item, index) in data"
                        :key="index"
                        @click="handleRowClick(item, index, $event)">
                        <td v-if="slots.selection">
                            <slot name="selection" v-bind:row="item" />
                        </td>
                        <template v-for="(columnKey) in columnKeySortList">
                            <td
                                v-if="columnKeyRenderMap[columnKey]"
                                :key="columnKey">
                                <template v-if="columnKey === 'alive'">
                                    <agent-status :data="item.alive" />
                                </template>
                                <template v-else>
                                    <div class="cell">
                                        {{ item[columnKey] || '--' }}
                                        <template v-if="columnKey === 'ip'">
                                            <slot name="ip" v-bind:row="item" />
                                        </template>
                                        <template v-if="columnKey === 'ipv6'">
                                            <slot name="ipv6" v-bind:row="item" />
                                        </template>
                                    </div>
                                </template>
                            </td>
                        </template>
                        <td v-if="slots.action">
                            <slot name="action" v-bind:row="item" />
                        </td>
                        <td
                            v-if="showSetting"
                            key="settting" />
                    </tr>
                </tbody>
            </table>
            <div
                v-if="slots.empty"
                class="table-empty">
                <slot name="empty" />
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
        <div
            v-if="isShowSetting"
            class="table-column-setting">
            <div class="setting-header">
                表格设置
            </div>
            <bk-checkbox-group v-model="columnKeyRenderList">
                <vuedraggable
                    :list="columnConfigList"
                    class="column-list">
                    <div
                        v-for="item in columnConfigList"
                        class="column-item"
                        :key="item.name">
                        <template v-if="item.key === 'ip'">
                            <bk-checkbox
                                :value="item.key"
                                :disabled="!columnKeyRenderList.includes('ipv6')">
                                {{ item.name }}
                            </bk-checkbox>
                        </template>
                        <template v-else-if="item.key === 'ipv6'">
                            <bk-checkbox
                                :value="item.key"
                                :disabled="!columnKeyRenderList.includes('ip')">
                                {{ item.name }}
                            </bk-checkbox>
                        </template>
                        <template v-else>
                            <bk-checkbox :value="item.key">
                                {{ item.name }}
                            </bk-checkbox>
                        </template>
                        <div class="column-item-drag">
                            <i class="bk-ipselector-icon bk-ipselector-ketuodong" />
                        </div>
                    </div>
                </vuedraggable>
            </bk-checkbox-group>
            <div class="setting-footer">
                <bk-button
                    theme="primary"
                    @click="handleSubmitSetting"
                    style="margin-right: 8px;">
                    确定
                </bk-button>
                <bk-button @click="handleHideSetting">
                    取消
                </bk-button>
            </div>
        </div>
    </div>
</template>
<script setup>
    import {
        ref,
        useSlots,
        computed,
        shallowRef,
    } from 'vue';
    import CustomSettingsService from '@service/custom-settings';
    import vuedraggable from 'vuedraggable';
    import useHostRenderKey from '../../hooks/use-host-render-key';
    import { makeMap } from '../../utils';
    import AgentStatus from '../agent-status.vue';

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

    const CUSTOM_SETTINGS_MODULE = 'ip_selector';

    // 表格列的完整配置
    const tableColumnConfig = {
        ip: {
            name: 'IP',
            key: 'ip',
            width: '120px',
        },
        ipv6: {
            name: 'IPv6',
            key: 'ipv6',
            width: '180px',
        },
        coludArea: {
            name: '云区域',
            key: 'coludArea',
        },
        alive: {
            name: 'Agent 状态',
            key: 'alive',
        },
        hostName: {
            name: '主机名称',
            key: 'hostName',
        },
        osName: {
            name: 'OS 名称',
            key: 'osName',
        },
        coludName: {
            name: '所属云厂商',
            key: 'coludName',
        },
        osType: {
            name: 'OS 类型',
            key: 'osType',
        },
        hostId: {
            name: 'Host ID',
            key: 'hostId',
            width: '100px',
        },
        agentId: {
            name: 'Agent ID',
            key: 'agentId',
        },
    };
    
    const isLoadingCustom = ref(true);
    // 表格列配置(数组格式，交互上支持拖拽排序)
    const columnConfigList = ref(Object.values(tableColumnConfig));
    // 列的显示顺序
    const columnKeySortList = ref(Object.keys(tableColumnConfig));
    // 需要显示的列 checkbox-group 的值
    const columnKeyRenderList = ref(['ip', 'ipv6', 'alive', 'osName']);
    // 需要显示的列
    const columnKeyRenderMap = shallowRef(makeMap(columnKeyRenderList.value));
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
    CustomSettingsService.fetchAll({
        moduleList: [CUSTOM_SETTINGS_MODULE],
    })
        .then((data) => {
            if (!data[CUSTOM_SETTINGS_MODULE]) {
                return;
            }
            const {
                hostListColumn = [],
                hostListColumnSort = [],
            } = data[CUSTOM_SETTINGS_MODULE];
            columnConfigList.value = hostListColumnSort.reduce((result, columnKey) => {
                result.push(tableColumnConfig[columnKey]);
                return result;
            }, []);
            columnKeySortList.value = hostListColumnSort;
            columnKeyRenderList.value = hostListColumn;
            columnKeyRenderMap.value = makeMap(hostListColumn);
            setHostListRenderPrimaryKey();
        })
        .finally(() => {
            isLoadingCustom.value = false;
        });
    
    // 显示设置弹框
    const handleShowSetting = () => {
        isShowSetting.value = true;
    };

    // 提交表格列设置
    const handleSubmitSetting = () => {
        isShowSetting.value = false;
        columnKeySortList.value = columnConfigList.value.map(item => item.key);
        columnKeyRenderMap.value = makeMap(columnKeyRenderList.value);
        setHostListRenderPrimaryKey();
        CustomSettingsService.update({
            settingsMap: {
                [CUSTOM_SETTINGS_MODULE]: {
                    hostListColumn: columnKeyRenderList.value,
                    hostListColumnSort: columnKeySortList.value,
                },
            },
        });
    };

    // 取消表格列设置
    const handleHideSetting = () => {
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
    @import "../../styles/table.mixin.css";

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
                margin-top: 75px;
                font-size: 12px;
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

        .table-column-setting {
            position: absolute;
            top: 40px;
            right: 0;
            z-index: 9;
            width: 545px;
            padding-top: 24px;
            background: #fff;
            border: 1px solid #dcdee5;
            border-radius: 2px;
            transform: translateX(25%);
            box-shadow: 0 2px 6px 0 rgb(0 0 0 / 10%);

            .setting-header {
                padding: 0 24px;
                font-size: 20px;
                line-height: 20px;
                color: #313238;
            }

            .column-list {
                display: flex;
                flex-wrap: wrap;
                padding: 0 24px 36px;
            }

            .column-item {
                display: flex;
                align-items: center;
                width: 120px;
                height: 32px;
                padding: 0 8px;
                margin-top: 16px;
                border-radius: 2px;

                &:hover {
                    background: #f5f7fa;

                    .column-item-drag {
                        display: flex;
                    }
                }

                .bk-checkbox-text {
                    font-size: 12px;
                }
            }

            .column-item-drag {
                display: none;
                width: 16px;
                height: 16px;
                margin-left: auto;
                font-size: 12px;
                color: #979ba5;
                justify-content: center;
                align-items: center;
            }

            .setting-footer {
                display: flex;
                height: 50px;
                padding-right: 24px;
                background: #fafbfd;
                border-top: 1px solid #dcdee5;
                justify-content: flex-end;
                align-items: center;
            }
        }

        @include table;
    }
</style>
