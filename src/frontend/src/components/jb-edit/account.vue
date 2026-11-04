<!--
* Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
*
* Copyright (C) 2021 Tencent.  All rights reserved.
*
* BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
*
* License for BK-JOB蓝鲸智云作业平台:
*
*
* Terms of the MIT License:
* ---------------------------------------------------
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
* documentation files (the "Software"), to deal in the Software without restriction, including without limitation
* the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
* to permit persons to whom the Software is furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all copies or substantial portions of
* the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
* THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
* CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
* IN THE SOFTWARE.
-->

<template>
  <div
    class="jb-edit-account"
    :class="mode"
    :style="styles">
    <template v-if="!isEditing">
      <div
        class="render-value-box"
        @click.stop="handleBlockShowEdit">
        <div
          v-bk-overflow-tips
          class="value-text">
          <slot :value="renderText">
            <span>{{ renderText }}</span>
          </slot>
        </div>
        <div class="edit-action-box">
          <icon
            v-if="!isBlock"
            class="edit-action"
            type="edit-2"
            @click.self.stop="handleShowEdit" />
        </div>
      </div>
    </template>
    <template v-else>
      <div
        class="edit-value-box"
        @click.stop="">
        <account-select
          :value="localValue"
          @change="handleAccountChange" />
      </div>
    </template>
  </div>
</template>
<script>
  export default {
    name: 'JbEditAccount',
  };
</script>

<script setup>
  import {
    computed,
    getCurrentInstance,
    onBeforeUnmount,
    onMounted,
    ref,
    watch,
  } from 'vue';

  import AccountManageService from '@service/account-manage';

  import AccountSelect from '@components/account-select';

  const { proxy } = getCurrentInstance();

  const props = defineProps({
    /**
     * @value block 块级交互
     * @value ‘’ 默认鼠标点击编辑按钮
     */
    mode: {
      type: String,
      default: '',
    },
    /**
     * @desc 编辑操作对应的字段名称
     */
    field: {
      type: String,
      required: true,
    },
    /**
     * @desc 默认值，账号 ID
     */
    value: {
      type: [
        String, Number,
      ],
      default: '',
    },
    /**
     * @desc 宽度
     */
    width: {
      type: String,
      default: 'auto',
    },
  });
  const emits = defineEmits(['on-change']);

  // 后端 defaultValue 是字符串，账号 id 是数字，需要归一避免误判为账号不存在
  const localValue = ref(Number(props.value) || '');
  const accountList = ref([]);
  const isEditing = ref(false);

  /**
   * @desc 展示态文案（账号 ID -> 别名）
   *
   * 账号列表未就绪或账号已被删除时展示 --
   */
  const renderText = computed(() => {
    const account = accountList.value.find(item => item.id === Number(localValue.value));
    return account ? account.alias : '--';
  });
  const styles = computed(() => ({
    width: props.width,
  }));
  const isBlock = computed(() => props.mode === 'block');

  watch(() => props.value, (value) => {
    localValue.value = Number(value) || '';
  });

  // 展示态需要用账号 ID 换取别名
  AccountManageService.fetchAccountWhole()
    .then((data) => {
      accountList.value = Object.freeze(data);
    });

  const handleShowEdit = () => {
    // 关闭其他编辑态
    document.body.click();
    isEditing.value = true;
  };

  const handleBlockShowEdit = () => {
    if (!isBlock.value) {
      return;
    }
    handleShowEdit();
  };

  /**
   * @desc 选中账号后退出编辑态并提交
   * @param {Number} value 选中的账号 ID
   */
  const handleAccountChange = (value) => {
    isEditing.value = false;
    if (value === localValue.value) {
      return;
    }
    localValue.value = value;
    emits('on-change', {
      [props.field]: value,
    });
  };

  /**
   * @desc 点击组件外部时退出编辑态
   * @param {Event} event dom 事件
   *
   * 账号下拉面板挂载在 body 上，需要一并排除，避免点击候选项时提前退出编辑态
   */
  const handleHideEdit = (event) => {
    if (!isEditing.value) {
      return;
    }
    if (proxy.$el.contains(event.target)) {
      return;
    }
    if (event.target.closest && event.target.closest('.account-select-menu-list')) {
      return;
    }
    isEditing.value = false;
  };

  onMounted(() => {
    document.body.addEventListener('click', handleHideEdit);
  });

  onBeforeUnmount(() => {
    document.body.removeEventListener('click', handleHideEdit);
  });
</script>
<style lang='postcss'>
  .jb-edit-account {
    &.block {
      position: relative;
      margin-left: -10px;
      cursor: pointer;

      .render-value-box {
        padding-left: 10px;

        &:hover {
          background: #f0f1f5;
        }
      }

      .edit-action-box {
        position: absolute;
        top: 0;
        right: 10px;
        width: 16px;
      }
    }

    .render-value-box {
      position: relative;
      display: flex;
      height: 30px;
      min-width: 36px;
      min-height: 28px;

      &:hover {
        .edit-action {
          opacity: 100%;
          transform: scale(1);
        }
      }
    }

    .value-text {
      overflow: hidden;
      line-height: 30px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .edit-action-box {
      display: flex;
      align-items: center;
      min-height: 1em;
      margin-right: auto;
      font-size: 16px;
      color: #979ba5;

      .edit-action {
        padding: 6px 0 6px 2px;
        cursor: pointer;
        opacity: 0%;
        transform: scale(0);
        transition: 0.15s;
        transform-origin: left center;

        &:hover {
          color: #3a84ff;
        }
      }
    }

    .edit-value-box {
      position: relative;
      width: 100%;
      font-size: 0;
    }
  }
</style>
