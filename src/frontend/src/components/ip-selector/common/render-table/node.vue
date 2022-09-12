<template>
    <div class="ip-selector-node-table">
        <div
            class="node-talbe-wrapper"
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
                        <th>节点名称</th>
                        <th style="width: 180px;">
                            Agent 状态
                        </th>
                    </tr>
                </thead>
                <tbody>
                    <tr
                        v-for="(row, index) in data"
                        :key="index"
                        @click="handleRowClick(row, index, $event)">
                        <td v-if="slots.selection">
                            <slot
                                name="selection"
                                :row="row" />
                        </td>
                        
                        <td>
                            <div class="cell">
                                {{ row.namePath || '--' }}
                            </div>
                        </td>
                        <td>
                            <div class="cell">
                                <span v-if="agentStatic[row.key]">
                                    <span
                                        v-if="agentStatic[row.key].not_alive_count"
                                        class="agent-statistics-box"
                                        @click.stop="handleChooseAbnormal">
                                        <span class="agent-desc">异常:</span>
                                        <span style="color: #ea3636;">{{ agentStatic[row.key].not_alive_count }}</span>
                                    </span>
                                    <span
                                        class="agent-statistics-box"
                                        @click.stop="handleChooseAll">
                                        <span class="agent-desc">总数:</span>
                                        <span style="color: #3a84ff;">{{ agentStatic[row.key].total_count }}</span>
                                    </span>
                                </span>
                                <div
                                    v-else
                                    class="bk-ip-selector-rotate-loading">
                                    <svg style="width: 1em; height: 1em; vertical-align: middle; fill: currentcolor;">
                                        <use xlink:href="#bk-ipselector-loading" />
                                    </svg>
                                </div>
                            </div>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
</template>
<script setup>
    import {
        computed,
        useSlots,
    } from 'vue';

    const props = defineProps({
        data: {
            type: Array,
            required: true,
        },
        agentStatic: {
            type: Object,
            default: () => ({}),
        },
        height: {
            type: Number,
        },
    });

    const emits = defineEmits([
        'row-click',
    ]);

    const slots = useSlots();

    const styles = computed(() => {
        const styles = {};
        if (props.height > 0) {
            styles['max-height'] = `${props.height - 96}px`;
        }
        return styles;
    });

    const handleRowClick = (rowData, rowIndex, event) => {
        emits('row-click', rowData, rowIndex, event);
    };

    const handleChooseAbnormal = () => {
        
    };

    const handleChooseAll = () => {
        
    };
</script>
<style lang="postcss">
    @import "../../styles/table.mixin.css";

    .ip-selector-node-table {
        .node-talbe-wrapper {
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
        }

        .agent-statistics-box {
            display: inline-flex;
            height: 24px;
            padding: 0 4px;
            align-items: center;

            &:hover {
                /* background: #eaebf0; */
            }

            .agent-desc {
                padding-right: 4px;
            }
        }

        @include table;
    }
</style>
