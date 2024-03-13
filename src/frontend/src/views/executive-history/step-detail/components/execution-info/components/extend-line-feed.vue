<template>
  <div
    v-if="activePanel === 'scriptLog'"
    class="extend-item"
    style="padding-left: 16px; border-left: 1px solid #262626;">
    <bk-switcher
      size="small"
      theme="primary"
      :value="value"
      @change="handleChange" />
    <span style="padding-left: 7px; font-size: 12px; color: #979ba5;">{{ $t('history.自动换行') }}</span>
  </div>
</template>
<script setup>
  import { onMounted } from 'vue';
  const props = defineProps({
    value: {
      type: Boolean,
      default: false,
    },
    activePanel: {
      type: String,
      required: true,
    },
  });

  const emits = defineEmits([
    'change',
    'input',
  ]);

  const SCRIPT_LOG_AUTO_LINE_FEED = 'script_log_line_feed';

  const handleChange = () => {
    const result  = !props.value;
    if (result) {
      localStorage.setItem(SCRIPT_LOG_AUTO_LINE_FEED, true);
    } else {
      localStorage.removeItem(SCRIPT_LOG_AUTO_LINE_FEED);
    }

    emits('input', result);
    emits('change', result);
  };

  onMounted(() => {
    emits('input', Boolean(localStorage.getItem(SCRIPT_LOG_AUTO_LINE_FEED)));
  });
</script>

