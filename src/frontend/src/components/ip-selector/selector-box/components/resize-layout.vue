<template>
    <div class="ip-selector-resize-layout">
        <div class="layout-left">
            <slot />
        </div>
        <div class="layout-right">
            <slot name="right" />
        </div>
        <div
            v-if="resize === 'left'"
            class="resize-btn-left">
            &lt;
        </div>
        <div
            v-if="resize === 'right'"
            class="resize-btn-right">
            >
        </div>
    </div>
</template>
<script setup lang="ts">
    defineProps({
        resize: {
            type: String,
            default: 'left',
        },
    });
</script>
<style lang="postcss">
    .ip-selector-resize-layout {
        position: relative;
        display: flex;

        .layout-left {
            border-left: 1px solid #dcdee5;
        }

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
