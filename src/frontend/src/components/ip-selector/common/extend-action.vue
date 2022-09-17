<template>
    <div class="ip-selector-extend-action">
        <ip-selector-icon type="more" />
        <div class="action-menu">
            <slot />
        </div>
    </div>
</template>
<script setup>
  import IpSelectorIcon from './ip-selector-icon';
</script>
<style lang="postcss">
    .ip-selector-extend-action {
        position: relative;
        display: inline-flex;
        width: 20px;
        height: 20px;
        font-size: 12px;
        cursor: pointer;
        border-radius: 2px;
        justify-content: center;
        align-items: center;

        &:hover {
            background: #e1ecff;

            .action-menu {
                display: block;
            }
        }

        .action-menu {
            position: absolute;
            top: 20px;
            left: 0;
            display: none;
            font-size: 12px;
            line-height: 32px;
            color: #63656e;
            background: #fff;
            border: 1px solid #dcdee5;
            border-radius: 2px;
            transform: translateX(-50%);
            box-shadow: 0 2px 6px 0 rgb(0 0 0 / 10%);

            & > * {
                padding: 0 12px;
                word-break: keep-all;
                white-space: nowrap;

                &:hover {
                    color: #3a84ff;
                    background: #f5f6fa;
                }
            }
        }
    }
</style>
