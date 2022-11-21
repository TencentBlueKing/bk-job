<template>
    <th
        :class="{
            [`host-column-${columnKey}`]: true,
        }"
        :style="styles"
        @mousedown="handleMouseDown"
        @mousemove="handleMouseMove">
        <div class="cell">
            <div class="cell-text">
                <RenderThCell :config="columnConfig" />
            </div>
            <filter-select
                v-if="columnConfig.filter"
                :data="columnConfig.filter" />
        </div>
    </th>
</template>
<script setup>
    import { computed } from 'vue';

    import Manager from '../../../../../manager';
    import tableColumnConfig from '../../column-config';

    import FilterSelect from './filter-select.vue';
    import RenderThCell from './render-th-cell.js';

    const props = defineProps({
        columnKey: {
            type: String,
            required: true,
        },
        columnWidthCallback: {
            type: Function,
        },
        index: {
            type: Number,
            required: true,
        },
    });
    const emits = defineEmits([
        'mousedown',
        'mousemove',
    ]);

    const { hostTableColumns } = Manager.config;

    const tableCustomColumnConfig = hostTableColumns.reduce((result, item) => ({
        ...result,
        [item.key]: item,
    }), {});

    const columnConfig = computed(() => {
        if (tableColumnConfig[props.columnKey]) {
            return tableColumnConfig[props.columnKey];
        } else if (tableCustomColumnConfig[props.columnKey]) {
            return tableCustomColumnConfig[props.columnKey];
        }
        return null;
    });

    const styles = {
        width: props.columnWidthCallback
                ? props.columnWidthCallback(props.index)
                : columnConfig.value.width,
    };

    const handleMouseDown = (event) => {
        emits('mousedown', event);
    };

    const handleMouseMove = (event) => {
        emits('mousemove', event);
    };
</script>
