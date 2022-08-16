<template>
    <div class="preview-result-collsapn">
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
.preview-result-collsapn {
    position: relative;
    user-select: none;

    .title {
        display: flex;
        height: 34px;
        padding: 0 24px;
        font-size: 12px;
        color: #63656e;
        cursor: pointer;
        align-items: center;

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
