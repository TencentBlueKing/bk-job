<template>
  <div class="execution-info-panel-tab">
    <div
      v-if="!isFile"
      class="item"
      :class="{ active: value === 'scriptLog' }"
      @click="handleTogglePanel('scriptLog')">
      {{ $t('history.执行日志') }}
    </div>
    <template v-if="isFile">
      <div
        class="item"
        :class="{ active: value === 'download' }"
        @click="handleTogglePanel('download')">
        {{ $t('history.下载信息') }}
      </div>
      <div
        class="item"
        :class="{ active: value === 'upload' }"
        @click="handleTogglePanel('upload')">
        {{ $t('history.上传源信息') }}
      </div>
    </template>
    <div
      v-if="isTask && !isFile"
      class="item"
      :class="{ active: value === 'variable' }"
      @click="handleTogglePanel('variable')">
      {{ $t('history.变量明细') }}
    </div>
  </div>
</template>
<script setup>
  defineProps({
    value: {
      type: String,
    },
    isTask: {
      type: Boolean,
    },
    isFile: {
      type: Boolean,
    },
  });

  const emits = defineEmits(['change', 'input']);

  const handleTogglePanel = (value) => {
    emits('change', value);
    emits('input', value);
  };
</script>
<style lang="postcss">
  .execution-info-panel-tab{
    display: flex;

    .item {
      position: relative;
      height: 42px;
      padding: 0 20px;
      cursor: pointer;
      user-select: none;

      &.active {
        z-index: 1;
        color: #fff;
        background: #212124;

        &::before {
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 2px;
          background: #3a84ff;
          content: "";
        }
      }
    }
  }
</style>
