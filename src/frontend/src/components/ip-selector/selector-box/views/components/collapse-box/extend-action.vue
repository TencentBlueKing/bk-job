<template>
    <bk-popover
        placement="bottom"
        theme="light"
        trigger="click">
        <div class="collspan-box-extend-btn">
            <i type="more" />
        </div>
        <template #content>
            <div class="collspan-box-extend-action">
                <slot />
            </div>
        </template>
    </bk-popover>
</template>
<script setup>

</script>
<style lang="postcss">
    .collspan-box-extend-btn {
        display: flex;
        width: 24px;
        height: 24px;
        cursor: pointer;
        border-radius: 50%;
        align-items: center;
        justify-content: center;
        transition: all 0.15s;

        &:hover {
            background: #dcdee5;
        }
    }

    .collspan-box-extend-action {
        margin: 0 -14px;

        & > * {
            height: 32px;
            padding: 0 12px;
            line-height: 32px;
            cursor: pointer;

            &:hover {
                color: #3a84ff;
                background: #f5f6fa;
            }
        }
    }
</style>
