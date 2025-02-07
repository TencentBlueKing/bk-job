<template>
  <bk-form-item
    class="form-item-content"
    :field="field"
    :label="$t('解释器')">
    <bk-checkbox v-model="isCustom">
      <span
        v-bk-tooltips="$t('使用目标机器指定路径下的解释器运行本脚本（近对Windows有效）')"
        class="tips">
        {{ $t('自定义window解释器路径') }}
      </span>
    </bk-checkbox>
    <div
      v-if="isCustom"
      style="margin-top: 8px">
      <bk-input
        class="form-item-content"
        :placeholder="$t('输入目标机器上的自定义解释器软件路径，如：/usr/local/python-2/bin/python')"
        :value="formData[field]"
        @change="handleChange" />
    </div>
  </bk-form-item>
</template>
<script setup>
  import { ref, watch } from 'vue';

  const props = defineProps({
    field: {
      type: String,
      required: true,
    },
    formData: {
      type: Object,
      required: true,
    },
  });

  const emits = defineEmits(['on-change']);

  const isCustom = ref(false);

  watch(() => props.formData, () => {
    if (props.formData[props.field]) {
      isCustom.value = true;
    }
  }, {
    immediate: true,
  });

  const handleChange = (value) => {
    emits('on-change', props.field, value);
  };
</script>

