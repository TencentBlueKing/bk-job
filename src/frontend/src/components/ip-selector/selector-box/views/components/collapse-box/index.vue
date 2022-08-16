<template>
    <div class="ip-selector-view-collsapn">
        <div
            class="title"
            @click="handleToggle">
            <i
                class="arrow-flag"
                :class="{ 'is-show': isShow }"
                type="angle-line-down" />
            <slot name="title" />
        </div>
        <div
            v-if="isShow"
            class="content">
            <slot />
        </div>
        <div class="action">
            <slot name="action" />
        </div>
    </div>
</template>
<script setup>
  import { ref } from 'vue';

  const isShow = ref(true);

  const handleToggle = () => {
    isShow.value = !isShow.value;
  };
</script>
<style lang="postcss" scoped>
    .ip-selector-view-collsapn {
        position: relative;

        .title {
            display: flex;
            height: 42px;
            padding: 0 16px;
            font-size: 12px;
            color: #63656e;
            cursor: pointer;
            align-items: center;
            background: #eff1f5;
            user-select: none;

            .arrow-flag {
                margin-right: 4px;
                transform: rotateZ(-90deg);
                transition: all 0.15s;

                &.is-show {
                    transform: rotateZ(0);
                }
            }
        }

        .content {
            max-height: 476px;
            padding: 0 24px;
            overflow-y: auto;
            background: #fff;
        }

        .action {
            position: absolute;
            top: 4px;
            right: 0;
            margin-left: auto;
            cursor: pointer;
        }
    }
</style>
