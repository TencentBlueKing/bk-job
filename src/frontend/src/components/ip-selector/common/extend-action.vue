<template>
    <div class="ip-selector-extend-action">
        <div
            ref="handleRef"
            class="handler-btn">
            <ip-selector-icon type="more" />
        </div>
        <div
            ref="popRef"
            class="ip-selector-extend-action-popover">
            <slot />
        </div>
    </div>
</template>
<script setup>
    import tippy from 'tippy.js';
    import {
        onBeforeUnmount,
        onMounted,
        ref,
    } from 'vue';

    import IpSelectorIcon from './ip-selector-icon';

    const handleRef = ref();
    const popRef = ref();

    let tippyInstance;

    onMounted(() => {
        tippyInstance = tippy(handleRef.value, {
            content: popRef.value,
            placement: 'bottom-end',
            appendTo: () => document.body,
            theme: 'light',
            maxWidth: 'none',
            trigger: 'click',
            interactive: true,
            arrow: true,
            offset: [8, 8],
            zIndex: 999999,
            hideOnClick: true,
        });
    });

    onBeforeUnmount(() => {
        if (tippyInstance) {
            tippyInstance.hide();
            tippyInstance.destroy();
            tippyInstance = null;
        }
    });
</script>
<style lang="postcss">
    .ip-selector-extend-action {
        .handler-btn {
            display: flex;
            width: 20px;
            height: 20px;
            font-size: 12px;
            cursor: pointer;
            border-radius: 2px;
            justify-content: center;
            align-items: center;

            &:hover {
                color: #3a84ff;
                background: #e1ecff;
            }
        }
    }

    .ip-selector-extend-action-popover {
        margin: -5px -9px;
        font-size: 12px;
        line-height: 32px;
        color: #63656e;
        user-select: none;

        & > * {
            padding: 0 12px;
            word-break: keep-all;
            white-space: nowrap;
            cursor: pointer;

            &:hover {
                color: #3a84ff;
                background: #f5f6fa;
            }
        }
    }
</style>
