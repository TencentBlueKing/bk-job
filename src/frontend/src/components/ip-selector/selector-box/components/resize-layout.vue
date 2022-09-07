<template>
    <div class="ip-selector-resize-layout">
        <div class="layout-left">
            <slot />
        </div>
        <div
            class="layout-right"
            :style="layoutRightStyles">
            <slot name="right" />
        </div>
        <div
            v-if="flexDirection === 'left'"
            class="resize-btn-left">
            &lt;
        </div>
        <div
            v-if="flexDirection === 'right'"
            class="resize-btn-right">
            &gt;
        </div>
    </div>
</template>
<script setup>
    import { computed } from 'vue';
    
    const props = defineProps({
        flexDirection: {
            type: String,
            default: 'left',
        },
        defaultWidth: {
            type: Number,
        },
    });

    const layoutRightStyles = computed(() => ({
        width: `${props.defaultWidth}px`,
    }));
</script>
<style lang="postcss">
    .ip-selector-resize-layout {
        position: relative;
        display: flex;

        .layout-right {
            display: block;
        }

        .resize-btn-left,
        .resize-btn-right {
            position: absolute;
            top: 50%;
            display: flex;
            width: 16px;
            height: 64px;
            background: #dcdee5;
            transform: translateY(-50%);
            align-items: center;
            justify-content: center;
        }

        .resize-btn-right {
            position: absolute;
        }
    }
</style>
