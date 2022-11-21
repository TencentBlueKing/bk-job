<template>
    <div class="ip-selector-host-table">
        <RenderData
            :column-width-callback="columnWidthCallback"
            :data="data"
            :show-setting="showSetting">
            <template
                v-if="slots.selection"
                #header-selection>
                <slot name="header-selection" />
            </template>
            <template
                v-if="slots.action"
                #action>
                <span />
            </template>
            <RenderDataRow
                :data="data"
                :show-setting="showSetting"
                @row-click="handleRowClick">
                <template
                    v-if="slots.selection"
                    #selection="{ row }">
                    <slot
                        name="selection"
                        :row="row" />
                </template>
                <template
                    v-if="slots.action"
                    #action="{row}">
                    <slot
                        name="action"
                        :row="row" />
                </template>
            </RenderDataRow>
        </RenderData>
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
    import {
        computed,
        useSlots,
    } from 'vue';

    import RenderData from './render-data/index.vue';
    import RenderDataRow from './render-data/row.vue';

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

    const isShowPagination = computed(() => {
        if (!props.pagination) {
            return false;
        }
        if (props.pagination.count <= props.pagination.limit) {
            return false;
        }
        return true;
    });

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
    .ip-selector-host-table {
        position: relative;
        overflow: hidden;
    }
</style>
