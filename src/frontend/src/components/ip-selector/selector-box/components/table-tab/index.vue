<template>
    <div class="ip-selector-table-tab">
        <slot />
    </div>
</template>
<script>
    import { provide } from 'vue';
    export const tabKey = Symbol('tab');
</script>
<script setup>
    const props = defineProps({
        modelValue: {
            type: String,
        },
    });
    const emits = defineEmits(['change', 'update:modelValue']);

    let tabItemList = [];

    const registerItem = (itemContext) => {
        tabItemList.push(itemContext);
    };

    const unRegisterItem = (itemContext) => {
        tabItemList = tabItemList.reduce((result, item) => {
            if (item !== itemContext) {
                result.push(item);
            }
            return result;
        }, []);
    };

    provide(tabKey, {
        props,
        registerItem,
        unRegisterItem,
        change (name) {
            emits('change', name);
            emits('update:modelValue', name);
        },
    });
</script>
<style lang="postcss">
    .ip-selector-table-tab {
        display: flex;
        padding: 0 8px;
        border-bottom: 1px solid #dcdee5;
    }
</style>
