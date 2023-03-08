<template>
  <div>
    <div
      ref="handleRef"
      class="dropdown-menu-group"
      :class="{
        active: isActive
      }">
      <slot />
      <i class="toggle-flag bk-icon icon-angle-right" />
    </div>
    <div
      ref="popRef"
      style="padding: 5px 0; margin: -0.3rem -0.6rem;"
      @click="handleClick">
      <slot name="action" />
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

  const handleClick = () => {
    tippyInstance.hide();
  };

  onMounted(() => {
    tippyInstance = Tippy(handleRef.value, {
      arrow: false,
      allowHTML: false,
      content: popRef.value,
      placement: 'right-start',
      trigger: 'mouseenter',
      theme: 'light',
      hideOnClick: false,
      ignoreAttributes: true,
      animateFill: false,
      boundary: 'window',
      interactive: true,
      distance: 6,
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
  .dropdown-menu-group {
    display: flex;
    height: 32px;
    padding: 0 10px;
    font-size: 12px;
    color: #63656e;
    align-items: center;
    cursor: pointer;

    &.active {
      color: #3a84ff;
      background: #f5f6fa;
    }

    .toggle-flag {
      margin-left: auto;
      font-size: 16px;
    }
  }
</style>
