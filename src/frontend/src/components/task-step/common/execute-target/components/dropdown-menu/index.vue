<template>
  <div class="host-dropdown-menu">
    <div
      ref="handleRef"
      @click="handleShow"
      @mouseenter="handleShow">
      <slot />
    </div>
    <div
      ref="popRef"
      style="min-width: 98px; padding: 5px 0; margin: -0.3rem -0.6rem; word-break: keep-all;">
      <slot name="menu" />
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

  onMounted(() => {
    tippyInstance = Tippy(handleRef.value, {
      arrow: false,
      allowHTML: false,
      content: popRef.value,
      placement: 'bottom-start',
      trigger: 'manual',
      theme: 'light',
      hideOnClick: true,
      animateFill: false,
      interactive: true,
      boundary: 'window',
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
<style lang="postcss" scoped>
  .host-dropdown-menu {
    display: block;
  }
</style>
