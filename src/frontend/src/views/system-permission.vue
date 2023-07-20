<template>
  <div class="system-permission">
    <bk-exception type="403">
      <span>{{ $t('无该应用的访问权限') }}</span>
      <div class="text-subtitle">
        {{ message }}
      </div>
    </bk-exception>
  </div>
</template>
<script setup>
  import { getCurrentInstance } from 'vue';

  defineProps({
    message: String,
  });

  console.log('getCurrentInstance = ', getCurrentInstance());
</script>
<style lang="postcss">
.system-permission{
  display: flex;
  justify-content: center;
  padding-top: 200px;

  .text-subtitle{
    margin-top: 14px;
    font-size: 14px;
    color: #979ba5;
    text-align: center;
  }
}
</style>

