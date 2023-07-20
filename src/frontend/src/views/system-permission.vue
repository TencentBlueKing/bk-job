<template>
  <div class="system-permission">
    <bk-exception type="403">
      <div style="font-size: 24px; line-height: 32px; color: #63656E;">
        {{ message }}
      </div>
      <div class="text-subtitle">
        {{ data }}
      </div>
    </bk-exception>
  </div>
</template>
<script setup>

  defineProps({
    message: String,
    data: String,
  });

</script>
<style lang="postcss">
.system-permission{
  display: flex;
  height: 100vh;
  padding-top: 150px;
  justify-content: center;
  background: #fff;

  .text-subtitle{
    margin-top: 16px;
    font-size: 14px;
    line-height: 22px;
    color: #979ba5;
    text-align: center;
  }
}
</style>

