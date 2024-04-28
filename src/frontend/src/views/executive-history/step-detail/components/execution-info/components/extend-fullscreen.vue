<template>
  <div class="extend-item">
    <div
      v-if="!value"
      v-bk-tooltips="$t('history.全屏')"
      @click="handleFullscreen">
      <icon type="full-screen" />
    </div>
    <div
      v-if="value"
      v-bk-tooltips="$t('history.还原')"
      @click="handleExitFullscreen">
      <icon type="un-full-screen" />
    </div>
  </div>
</template>
<script setup>
  import {
    onBeforeUnmount,
    onMounted,
  } from 'vue';

  defineProps({
    value: {
      type: Boolean,
      default: false,
    },
  });

  const emits = defineEmits([
    'change',
    'input',
    'fullscreen',
    'exit-fullscreen',
  ]);

  const handleFullscreen = () => {
    emits('input', true);
    emits('fullscreen');
  };
  /**
   * @desc 退出日志全屏
   */
  const handleExitFullscreen = () => {
    emits('input', false);
    emits('exit-fullscreen');
  };
  /**
   * @desc esc键退出日志全屏
   */
  const handleExitByESC = (event) => {
    if (event.keyCode === 27) {
      handleExitFullscreen();
    }
  };

  onMounted(() => {
    window.addEventListener('keyup', handleExitByESC);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('keyup', handleExitByESC);
  });
</script>
