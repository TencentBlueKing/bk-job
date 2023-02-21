<template>
  <div
    class="dropdown-menu-item"
    v-on="listeners">
    <slot />
  </div>
</template>
<script setup>
  import { useListeners } from 'vue';

  const listeners = useListeners();

</script>
<style lang="postcss" scoped>
  .dropdown-menu-item {
    display: flex;
    height: 32px;
    padding: 0 10px;
    font-size: 12px;
    color: #63656e;
    align-items: center;
    cursor: pointer;

    &:hover {
      color: #3a84ff;
      background: #f5f6fa;
    }
  }
</style>
