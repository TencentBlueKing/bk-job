<template>
  <div class="system-permission">
    <bk-exception type="403">
      <span>无该应用的访问权限</span>
      <div class="text-subtitle">
        请联系 {{ message }} 开通
      </div>
    </bk-exception>
  </div>
</template>
<script setup>
  defineProps({
    message: String,
  });
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

