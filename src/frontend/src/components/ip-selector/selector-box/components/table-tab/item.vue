<template>
    <div
        class="ip-selector-table-tab-item"
        :class="classes"
        @click="handleClick">
        <slot />
    </div>
</template>
<script setup>
    import {
        computed,
        getCurrentInstance,
        inject,
        onBeforeUnmount,
    } from 'vue';

    import { tabKey } from './index.vue';

    const props = defineProps({
        name: {
            type: String,
            required: true,
        },
    });

    const currentInstance = getCurrentInstance();

    const tabContext = inject(tabKey);

    const classes = computed(() => ({
        active: tabContext.props.modelValue === props.name,
    }));

    tabContext.registerItem(currentInstance);

    const handleClick = () => {
       tabContext.change(props.name);
    };

    onBeforeUnmount(() => {
        tabContext.unRegisterItem(currentInstance);
    });
</script>
<style lang="postcss">
    .ip-selector-table-tab-item {
        position: relative;
        display: flex;
        height: 40px;
        padding: 0 8px;
        cursor: pointer;
        align-items: center;

        &.active {
            color: #3a84ff;
            cursor: default;

            &::after {
                position: absolute;
                right: 0;
                bottom: -1px;
                left: 0;
                border-bottom: 2px solid #3a84ff;
                content: "";
            }
        }

        & ~ & {
            margin-left: 16px;
        }
    }
</style>
