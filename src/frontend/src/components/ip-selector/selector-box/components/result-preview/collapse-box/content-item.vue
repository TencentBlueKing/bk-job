<template>
    <div class="content-item">
        <div class="content-text">
            <slot />
        </div>
        <div
            class="item-remove"
            @click="handleRemove">
            <i type="close" />
        </div>
    </div>
</template>
<script lang="ts" setup>
  const emits = defineEmits(['remove']);

  const handleRemove = () => {
    emits('remove');
  };
</script>
<style lang="postcss" scoped>
    .content-item {
        display: flex;
        height: 32px;
        padding-left: 12px;
        overflow: hidden;
        font-size: 12px;
        color: #63656e;
        background: #fff;
        border-radius: 2px;
        opacity: 100%;
        transition: all 0.15s;
        align-items: center;

        &:nth-child(n+2) {
            margin-top: 2px;
        }

        &:hover {
            box-shadow: 0 1px 2px 0 rgb(0 0 0 / 6%);

            .content-text {
                max-width: calc(100% - 56px);
            }

            .item-remove {
                width: 32px;
                opacity: 100%;
            }
        }

        .content-text {
            margin-right: 12px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            direction: rtl;
            flex: 0 1 auto;
            align-items: center;
        }

        .item-remove {
            display: flex;
            width: 0;
            height: 100%;
            margin-left: auto;
            overflow: hidden;
            cursor: pointer;
            opacity: 0%;
            transition: all 0.15s;
            align-items: center;
            justify-content: center;

            &:hover {
                color: #3a84ff;
            }
        }
    }
</style>
