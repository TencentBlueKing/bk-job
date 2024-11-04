<template>
  <div class="step-detail-ip-list-copy-menu">
    <div
      ref="handleRef"
      class="handler-btn"
      :class="{
        active: isActive
      }"
      @click="handleShow"
      @mouseenter="handleShow">
      <span
        class="copy-ip-btn">
        <icon type="step-copy" />
      </span>
    </div>
    <div
      ref="popRef"
      class="step-detail-ip-list-copy-menu-popover"
      @click="handleClick">
      <slot />
    </div>
  </div>
</template>
<script setup>
  import Tippy from 'bk-magic-vue/lib/utils/tippy';
  import {
    onBeforeUnmount,
    onMounted,
    ref,
  } from 'vue';


  const handleRef = ref();
  const popRef = ref();
  const isActive = ref(false);

  let tippyInstance;

  const handleShow = () => {
    tippyInstance.show();
  };

  const handleClick = () => {
    tippyInstance.hide();
  };

  onMounted(() => {
    tippyInstance = Tippy(handleRef.value, {
      content: popRef.value,
      placement: 'bottom-start',
      theme: 'light bk-step-detail-ip-list-copy-menu',
      trigger: 'manual',
      interactive: true,
      arrow: true,
      zIndex: 999999,
      hideOnClick: true,
      onShow() {
        isActive.value = true;
      },
      onHide() {
        isActive.value = false;
      },
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
  .step-detail-ip-list-copy-menu {
    display: inline-flex;
    align-items: center;

    .handler-btn {
      display: flex;
      width: 20px;
      height: 20px;
      font-size: 12px;
      cursor: pointer;
      border-radius: 2px;
      justify-content: center;
      align-items: center;

      &.active,
      &:hover {
        color: #3a84ff;
      }
    }
  }

  .bk-step-detail-ip-list-copy-menu-theme {
    padding: 8px 0;
  }

  .step-detail-ip-list-copy-menu-popover {
    font-size: 12px;
    line-height: 32px;
    color: #63656e;
    user-select: none;

    & > * {
      padding: 0 12px;
      word-break: keep-all;
      white-space: nowrap;
      cursor: pointer;

      &.active,
      &:hover {
        color: #3a84ff;
        background: #f5f6fa;
      }
    }
  }
</style>
