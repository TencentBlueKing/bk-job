<template>
  <bk-form-item
    v-show="isShow"
    class="form-item-content"
    error-display-type="normal"
    :label="t('解释器')"
    :property="field"
    :required="isCustom"
    :rules="rules">
    <bk-checkbox
      :value="isCustom"
      @change="handleCustomChange">
      <span
        v-bk-tooltips="t('使用目标机器指定路径下的解释器运行本脚本（仅对Windows有效）')"
        class="tips">
        {{ t('自定义windows解释器路径') }}
      </span>
    </bk-checkbox>
    <div
      v-if="isCustom"
      style="margin-top: 8px">
      <bk-input
        class="form-item-content"
        :placeholder="t('输入目标机器上的自定义解释器软件路径，如：D:\\Software\\python3\\python.exe。请勿指定命令行选项。')"
        :value="formData[field]"
        @change="handleChange" />
    </div>
  </bk-form-item>
</template>
<script setup>
  import { computed, ref, watch } from 'vue';

  import { useI18n } from '@/i18n';
  import {
    formatScriptTypeValue,
  } from '@/utils/assist';


  const props = defineProps({
    field: {
      type: String,
      required: true,
    },
    languageField: {
      type: String,
      required: true,
    },
    formData: {
      type: Object,
      required: true,
    },
  });

  const emits = defineEmits(['on-change']);

  const { t } = useI18n();

  const isCustom = ref(false);

  const isShow = computed(() => !['Shell', 'SQL'].includes(formatScriptTypeValue(props.formData[props.languageField])));

  const rules = [
    {
      validator: (value) => {
        if (!isCustom.value) {
          return true;
        }
        return /^[a-zA-Z]:\\(?:[^<>:"/\\|?*\r\n]+\\)*[^<>:"/\\|?*\r\n]*$/.test(value) && /\.exe$/.test(value);
      },
      message: t('解释器路径有误，需为合法的文件路径、且以 .exe 结尾。'),
      trigger: 'blur',
    },
  ];

  watch(() => props.formData, () => {
    isCustom.value = props.formData[props.field] !== undefined;
  }, {
    immediate: true,
    deep: true,
  });

  watch(isShow, () => {
    if (!isShow.value) {
      emits('on-change', props.field, undefined);
    }
  }, {
    immediate: true,
  });

  const handleCustomChange = (custom) => {
    isCustom.value = custom;
    emits('on-change', props.field, custom ? '' : undefined);
  };

  const handleChange = (value) => {
    emits('on-change', props.field, value);
  };
</script>

