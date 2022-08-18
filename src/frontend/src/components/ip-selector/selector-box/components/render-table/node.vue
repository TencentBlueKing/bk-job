<template>
    <div class="ip-selector-node-table">
        <table>
            <thead>
                <tr>
                    <th
                        v-if="slots.selection"
                        style="width: 40px;">
                        <slot name="header-selection" />
                    </th>
                    <th>节点名称</th>
                    <th style="width: 180px;">Agent 状态</th>
                </tr>
            </thead>
            <tbody>
                <tr
                    v-for="(row, index) in data"
                    :key="index"
                    @click="handleRowClick(row, index, $event)">
                    <td v-if="slots.selection">
                        <slot name="selection" v-bind:row="row" />
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
                                    v-if="agentStatic[row.key].abnormalNum"
                                    class="agent-statistics-box">
                                    <span class="agent-desc">异常:</span>
                                    <span style="color: #ea3636;">{{ agentStatic[row.key].abnormalNum }}</span>
                                </span>
                                <span class="agent-statistics-box">
                                    <span class="agent-desc">总数:</span>
                                    <span style="color: #3a84ff;">{{ agentStatic[row.key].abnormalNum + agentStatic[row.key].normalNum }}</span>
                                </span>
                            </span>
                            <span v-else>--</span>
                        </div>
                    </td>
                </tr>
            </tbody>
        </table>
        <bk-pagination
            small
            v-bind="pagination" />
    </div>
</template>
<script setup>
    import { useSlots } from 'vue';

    const slots = useSlots();

    console.dir(slots);
    
    defineProps({
        data: {
            type: Array,
            required: true,
        },
        agentStatic: {
            type: Object,
            default: () => ({}),
        },
        pagination: {
            type: Object,
            required: true,
        },
    });

    const emits = defineEmits([
        'row-click',
    ]);

    const handleRowClick = (rowData, rowIndex, event) => {
        emits('row-click', rowData, rowIndex, event);
    };
</script>
<style lang="postcss">
    .ip-selector-node-table {
        table {
            width: 100%;
            font-size: 12px;
            text-align: left;
            table-layout: fixed;

            th,
            td {
                height: 40px;
                padding: 0 10px;
                border-bottom: 1px solid #dcdee5;
            }

            th {
                font-weight: normal;
                color: #313238;
                background: #f0f1f5;
                user-select: none;

                &:hover {
                    background: #eaebf0;
                }
            }

            tbody {
                tr {
                    &:hover {
                        td {
                            background-color: #f5f7fa;
                        }
                    }
                }
            }

            td {
                cursor: pointer;

                .cell {
                    height: 20px;
                    overflow: hidden;
                    line-height: 20px;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                }
            }
        }

        .agent-statistics-box {
            display: inline-flex;
            height: 24px;
            padding: 0 4px;
            align-items: center;

            &:hover {
                background: #eaebf0;
            }

            .agent-desc {
                padding-right: 4px;
            }
        }
    }
</style>
