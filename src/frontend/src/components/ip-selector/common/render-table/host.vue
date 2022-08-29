<template>
    <div class="ip-selector-host-table">
        <div
            class="host-talbe-wrapper"
            :class="{ 'not-empty': data.length > 0 }"
            :style="styles">
            <table>
                <thead>
                    <tr>
                        <th
                            v-if="slots.selection"
                            style="width: 60px;">
                            <slot name="header-selection" />
                        </th>
                        <template v-for="columnName in sortColumnList">
                            <th
                                v-if="renderColumnMap[columnName]"
                                :key="columnName">
                                {{ columnConfig[columnName].name }}
                            </th>
                        </template>
                        
                        <!-- <th style="width: 120px;">IP</th>
                        <th style="width: 160px;">IPv6</th>
                        <th>主机名称</th>
                        <th style="width: 120px;">Agent 状态</th>
                        <th>云区域</th>
                        <th>系统</th> -->
                        <th style="width: 40px;">
                            <div
                                class="table-column-setting-btn"
                                @click="handleShowSetting">
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
                        <template v-for="(columnKey) in sortColumnList">
                            <td
                                v-if="renderColumnMap[columnKey]"
                                :key="columnKey">
                                <agent-status
                                    v-if="columnKey === 'alive'"
                                    :data="item.alive" />
                                <span v-else>
                                    {{ item[columnKey] || '--' }}
                                </span>
                            </td>
                        </template>
                        <td key="settting" />
                        <!-- <td>
                            <div class="cell">
                                {{ item.ip }}
                            </div>
                        </td>
                        <td>
                            <div class="cell">
                                {{ item.ipv6 || '--' }}
                            </div>
                        </td>
                        <td>
                            <div class="cell">
                                {{ item.hostName || '--' }}
                            </div>
                        </td>
                        <td>
                            <div class="cell">
                                <agent-status :data="item.alive" />
                            </div>
                        </td>
                        <td>
                            <div class="cell">
                                {{ item.cloudArea.name || '--' }}
                            </div>
                        </td>
                        <td colspan="2">
                            <div class="cell">
                                {{ item.osName || '--' }}
                            </div>
                        </td> -->
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
            <bk-checkbox-group v-model="renderColumnList">
                <vuedraggable
                    :list="columnSettingList"
                    class="column-list">
                    <div
                        v-for="item in columnSettingList"
                        class="column-item"
                        :key="item.name">
                        <bk-checkbox :value="item.key">
                            {{ item.name }}
                        </bk-checkbox>
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
    import vuedraggable from 'vuedraggable';
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
    });

    const emits = defineEmits([
        'row-click',
        'pagination-change',
    ]);

    // 表格列的完整配置
    const columnConfig = {
        ip: {
            name: 'IP',
            key: 'ip',
        },
        ipv6: {
            name: 'IPv6',
            key: 'ipv6',
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
        },
        agentId: {
            name: 'Agent ID',
            key: 'agentId',
        },
    };
    // 需要显示的列
    const renderColumnMap = shallowRef({
        ip: true,
        ipv6: true,
        osName: true,
    });
    // 列的显示顺序
    const sortColumnList = ref(Object.keys(columnConfig));
    // 表格列的配置列表
    const columnSettingList = ref(Object.values(columnConfig));
    // 需要显示的列 checkbox-group 的值
    const renderColumnList = ref([]);
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

    // 表格列显示设置
    const handleShowSetting = () => {
        isShowSetting.value = true;
        renderColumnList.value = Object.keys(renderColumnMap.value);
    };

    // 提交表格列设置
    const handleSubmitSetting = () => {
        renderColumnMap.value = makeMap(renderColumnList.value);
        isShowSetting.value = false;
        sortColumnList.value = columnSettingList.value.map(item => item.key);
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
