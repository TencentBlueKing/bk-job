<template>
  <div
    v-if="activePanel === 'scriptLog'"
    class="extend-item">
    <div
      @mouseenter="handleShowSetFont"
      @mouseleave="handleHideSetFont">
      Aa
    </div>
    <div
      class="execution-info-script-log-font-setting"
      :class="{ active: isFontSet }"
      @mouseenter="handleShowSetFont"
      @mouseleave="handleHideSetFont">
      <div class="font-setting-wraper">
        <div
          class="font-item"
          :class="{ active: value === 12 }"
          style="font-size: 12px;"
          @click="handleFontChange(12)">
          Aa
        </div>
        <div class="line" />
        <div
          class="font-item"
          :class="{ active: value === 13 }"
          style="font-size: 13px;"
          @click="handleFontChange(13)">
          Aa
        </div>
        <div class="line" />
        <div
          class="font-item"
          :class="{ active: value === 14 }"
          style="font-size: 14px;"
          @click="handleFontChange(14)">
          Aa
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
  import {
    onMounted,
    ref,
  } from 'vue';

  const STEP_FONT_SIZE_KEY = 'step_execution_font_size';

  defineProps({
    activePanel: {
      type: String,
      required: true,
    },
    value: {
      type: Number,
      required: true,
    },
  });

  const emits = defineEmits(['change', 'input']);

  const isFontSet = ref(false);

  const handleShowSetFont = () => {
    isFontSet.value = true;
  };

  const handleHideSetFont = () => {
    isFontSet.value = false;
  };

  const handleFontChange = (fontSize) => {
    emits('input', fontSize);
    emits('change', fontSize);
    localStorage.setItem(STEP_FONT_SIZE_KEY, fontSize);
  };

  onMounted(() => {
    let fontSize = parseInt(localStorage.getItem(STEP_FONT_SIZE_KEY), 10);
    if (!fontSize || fontSize < 12) {
      fontSize = 12;
    } else if (fontSize > 14) {
      fontSize = 14;
    }
    handleFontChange(fontSize);
  });
</script>
<style lang="postcss">
  .execution-info-script-log-font-setting {
    position: absolute;
    top: 55px;
    right: 94px;
    z-index: 1;
    width: 160px;
    color: #979ba5;
    background: #2f3033;
    border: 1px solid;
    border-radius: 2px;
    opacity: 0%;
    visibility: hidden;
    transform: translateY(-15px);
    box-shadow: 0 2px 4px 0 rgb(0 0 0 / 50%);
    transition: all 0.15s;
    border-image-source: linear-gradient(#3b3c42, #292a2e);
    border-image-slice: 1;
    border-image-width: 1px;

    &.active {
      opacity: 100%;
      visibility: visible;
      transform: translateY(0);
    }

    .font-setting-wraper {
      position: relative;
      z-index: 1;
      display: flex;
      background: inherit;
      align-items: center;
    }

    &::before {
      position: absolute;
      top: -20px;
      right: 38px;
      width: 42px;
      height: 25px;
      cursor: pointer;
      content: "";
    }

    &::after {
      position: absolute;
      top: -5px;
      left: 50%;
      width: 11px;
      height: 11px;
      background: inherit;
      border: 1px solid #3b3c42;
      border-bottom: none;
      border-left: none;
      content: "";
      transform: translateX(-50%) rotateZ(-45deg);
    }

    .font-item {
      display: flex;
      height: 42px;
      cursor: pointer;
      transition: all 0.15s;
      align-items: center;
      flex: 1;
      justify-content: center;

      &:hover,
      &.active {
        color: #fafbfd;
      }
    }

    .line {
      width: 1px;
      height: 18px;
      background: #63656e;
    }
  }
</style>
