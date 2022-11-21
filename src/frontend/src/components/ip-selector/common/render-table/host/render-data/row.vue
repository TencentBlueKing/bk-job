<template>
    <tbody>
        <tr
            v-for="(hostDataItem, rowIndex) in data"
            :key="rowIndex"
            @click="handleRowClick(hostDataItem, rowIndex, $event)">
            <td
                v-if="slots.selection"
                class="columu-fixed">
                <slot
                    name="selection"
                    :row="hostDataItem" />
            </td>
            <template
                v-for="(columnKey, index) in columnKeySortList">
                <RenderTd
                    v-if="columnKeyRenderMap[columnKey]"
                    :key="`${columnKey}_${index}`"
                    :class="{
                        'columu-fixed': columnKey === firstRenderColumnKey,
                    }"
                    :column-key="columnKey"
                    :data="hostDataItem"
                    :style="{
                        left: `${slots.selection ? 60: 0}px`,
                    }">
                    <slot
                        v-if="columnKey === 'ip'"
                        name="ip"
                        :row="hostDataItem" />
                    <slot
                        v-if="columnKey === 'ipv6'"
                        name="ipv6"
                        :row="hostDataItem" />
                </RenderTd>
            </template>
            <td
                v-if="slots.action"
                class="columu-fixed-right"
                style="top: 0; right: 0;">
                <slot
                    name="action"
                    :row="hostDataItem" />
            </td>
            <td
                v-if="showSetting"
                key="settting" />
        </tr>
    </tbody>
</template>
<script setup>
    import { useSlots } from 'vue';

    import RenderTd from './components/render-td.vue';
    import {
        columnKeyRenderMap,
        columnKeySortList,
        firstRenderColumnKey,
     } from './index.vue';

    defineProps({
        data: {
            type: Array,
        },
        showSetting: {
            type: Boolean,
        },
    });

    const emits = defineEmits([
        'row-click',
    ]);

    const slots = useSlots();

    const handleRowClick = (rowData, rowIndex, event) => {
        emits('row-click', rowData, rowIndex, event);
    };
</script>
