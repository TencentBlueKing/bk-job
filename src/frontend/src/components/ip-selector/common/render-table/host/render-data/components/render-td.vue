<template>
    <td>
        <template v-if="columnKey === 'alive'">
            <agent-status :data="data.alive" />
        </template>
        <template v-else>
            <div class="cell">
                <div class="cell-text">
                    <RenderTdCell
                        :config="columnConfig"
                        :data="data" />
                </div>
                <div class="cell-append">
                    <slot />
                </div>
            </div>
        </template>
    </td>
</template>
<script setup>
    import { computed } from 'vue';

    import Manager from '../../../../../manager';
    import AgentStatus from '../../../../agent-status.vue';
    import tableColumnConfig from '../../column-config';

    import RenderTdCell from './render-td-cell.js';

    const props = defineProps({
        columnKey: {
            type: String,
        },
        data: {
            type: Object,
        },
    });

    const { hostTableCustomColumnList } = Manager.config;
    const tableCustomColumnConfig = hostTableCustomColumnList.reduce((result, item) => ({
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
</script>
