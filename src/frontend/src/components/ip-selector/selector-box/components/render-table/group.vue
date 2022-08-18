<template>
    <div class="ip-selector-host-table">
        <table>
            <thead>
                <tr>
                    <th style="width: 120px;">IP</th>
                    <th>IPv6</th>
                    <th>主机名称</th>
                    <th style="width: 120px;">Agent 状态</th>
                    <th>云区域</th>
                    <th>系统</th>
                </tr>
            </thead>
            <tbody>
                <tr
                    v-for="(item, index) in data"
                    :key="index"
                    @click="handleRowClick(item, index, $event)">
                    
                    <td>
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
                            {{ item.ipDesc || '--' }}
                        </div>
                    </td>
                    <td>
                        <div class="cell">
                            {{ item.agentStatus || '--' }}
                        </div>
                    </td>
                    <td>
                        <div class="cell">
                            {{ item.cloudArea.name || '--' }}
                        </div>
                    </td>
                    <td>
                        <div class="cell">
                            {{ item.osName || '--' }}
                        </div>
                    </td>
                </tr>
            </tbody>
        </table>
        <bk-pagination
            v-if="pagination"
            small
            v-bind="pagination"
            @change="handlePaginationChange"
            @limit-change="handlePaginationLimitChange" />
    </div>
</template>
<script setup>
    import { useSlots } from 'vue';

    const slots = useSlots();

    console.dir(slots);
    
    const props = defineProps({
        data: {
            type: Array,
            required: true,
        },
        pagination: {
            type: Object,
        },
    });

    const emits = defineEmits([
        'row-click',
        'pagination-change',
    ]);

    const handleRowClick = (rowData, rowIndex, event) => {
        emits('row-click', rowData, rowIndex, event);
    };

    const handlePaginationChange = (current) => {
        emits('pagination-change', {
            ...props.pagination,
            current,
        });
    };
    const handlePaginationLimitChange = (limit) => {
        emits('pagination-change', {
            ...props.pagination,
            limit,
        });
    };
</script>
<style lang="postcss">
    .ip-selector-host-table {
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

            td {
                color: #63656e;
                cursor: pointer;

                .cell {
                    height: 20px;
                    overflow: hidden;
                    line-height: 20px;
                    text-overflow: ellipsis;
                    white-space: nowrap;
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
        }
    }
</style>
