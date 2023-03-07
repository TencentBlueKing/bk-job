<!--
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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
    class="task-list-tag-edit"
    :class="{
      'active': value === id,
      'display': !canEdit,
      'edit': canEdit,
      'edit-error': !!error,
    }"
    @click.stop="">
    <icon
      class="tag-flag"
      :type="icon" />
    <template v-if="!isEditable">
      <div
        v-bk-tooltips="{
          allowHtml: true,
          width: 240,
          distance: 15,
          trigger: 'mouseenter',
          theme: 'light',
          content: `#${sefId}`,
          placement: 'right-start',
          boundary: 'window',
          disabled: tooltipsDisabled,
        }"
        class="tag-name"
        @click="handleSelect">
        <div
          v-bk-overflow-tips
          class="name-text">
          {{ displayName }}
        </div>
      </div>
      <div class="tag-num-box">
        <span class="tag-num">{{ count }}</span>
      </div>
      <auth-component
        v-if="canEdit"
        auth="tag/edit"
        :resource-id="id">
        <div
          class="edit-action"
          @click="handleEdit">
          <icon type="edit-2" />
        </div>
        <div
          slot="forbid"
          class="edit-action">
          <icon type="edit-2" />
        </div>
      </auth-component>
    </template>
    <template v-else>
      <bk-input
        ref="input"
        :placeholder="$t('template.请输入标签名...')"
        :value="displayName"
        @blur="handleBlur"
        @change="handleChange"
        @keyup="handleEnter" />
      <div
        v-if="error"
        v-bk-tooltips="errorTipsConfig"
        class="input-edit-info">
        <icon type="info" />
      </div>
    </template>
    <div style="display: none;">
      <table
        :id="sefId"
        style="font-size: 12px; line-height: 24px; color: #63656e;">
        <tr>
          <td style="color: #979ba5; text-align: right; white-space: nowrap; vertical-align: top;">
            {{ $t('template.标签名称：') }}
          </td>
          <td style="word-break: break-all; vertical-align: top;">
            {{ displayName }}
          </td>
        </tr>
        <tr>
          <td style="color: #979ba5; text-align: right; white-space: nowrap; vertical-align: top;">
            {{ $t('template.标签描述：') }}
          </td>
          <td style="word-break: break-all; vertical-align: top;">
            {{ description || '--' }}
          </td>
        </tr>
      </table>
    </div>
  </div>
</template>
<script>
  import _ from 'lodash';

  import { tagNameRule } from '@utils/validator';

  import I18n from '@/i18n';

  export default {
    name: '',
    props: {
      icon: {
        type: String,
        default: 'tag',
      },
      loading: {
        type: Boolean,
        default: true,
      },
      count: {
        type: Number,
        default: 0,
      },
      name: {
        type: String,
        default: '',
      },
      description: String,
      value: {
        type: Number,
      },
      tooltipsDisabled: {
        type: Boolean,
        default: false,
      },
      id: {
        type: Number,
      },
      canEdit: {
        type: Boolean,
        default: false,
      },
      tagList: {
        type: Array,
        default: () => [],
      },
    },
    data() {
      return {
        displayName: this.name,
        isEditable: false,
        error: '',
      };
    },
    computed: {
      /**
       * @desc 重名检测
       * @returns { Array }
       */
      checkRenameList() {
        return this.tagList.filter(_ => _.name !== this.name);
      },
      /**
       * @desc 编辑时错误信息提示
       * @returns { Object }
       */
      errorTipsConfig() {
        const errorMap = {
          1: I18n.t('template.标签名不可为空'),
          2: I18n.t('template.标签名已存在，请重新输入'),
          3: tagNameRule.message,
        };
        return {
          placement: 'top-left',
          content: errorMap[this.error],
          width: this.error === 3 ? 200 : '',
        };
      },
    },
    created() {
      this.sefId = `tag_${_.random(1, 1000)}_${Date.now()}`;
    },
    mounted() {
      document.body.addEventListener('click', this.hideEdit);
      this.$once('hook:beforeDestroy', () => {
        document.body.removeEventListener('click', this.hideEdit);
      });
    },
    methods: {
      /**
       * @desc 选中当前 TAG
       * @returns { viod }
       */
      handleSelect() {
        this.$emit('on-select', this.id, this.name);
      },
      /**
       * @desc 编辑状态值更新
       */
      triggerChange() {
        if (this.displayName === this.name) {
          this.isEditable = false;
          return;
        }
        if (this.displayName === '') {
          this.error = 1;
          return;
        }
        if (_.find(this.checkRenameList, _ => _.name === this.displayName)) {
          this.error = 2;
          return;
        }
        if (!tagNameRule.validator(this.displayName)) {
          this.error = 3;
          return;
        }
        this.error = '';

        this.isEditable = false;

        this.$emit('on-edit', {
          id: this.id,
          name: this.displayName,
        });
      },
      /**
       * @desc 开始编辑
       */
      handleEdit() {
        document.body.click();
        this.isEditable = true;
        this.$nextTick(() => {
          this.$refs.input.focus();
        });
      },
      /**
       * @desc 输入框值更新
       */
      handleChange(value) {
        this.displayName = value.trim();
      },
      /**
       * @desc 输入框失去焦点
       */
      handleBlur() {
        this.triggerChange();
      },
      /**
       * @desc Enter 触发值更新
       */
      handleEnter(value, event) {
        if (!this.isEditable) return;
        if (event.key === 'Enter' && event.keyCode === 13) {
          this.triggerChange();
        }
      },
      /**
       * @desc 取消编辑状态
       */
      hideEdit() {
        if (!this.isEditable) {
          return;
        }
        if (this.error) {
          return;
        }
        this.isEditable = false;
      },
    },
  };
</script>
<style lang='postcss'>
  @keyframes ani-rotate {
    to {
      transform: rotateZ(360deg);
    }
  }

  .task-list-tag-edit {
    position: relative;
    display: flex;
    align-items: center;
    height: 36px;
    padding-right: 20px;
    padding-left: 25px;
    font-size: 14px;
    color: #63656e;
    cursor: pointer;

    &:hover {
      color: #3a84ff;
      background: #e1ecff;

      .tag-flag {
        color: #3a84ff;
      }

      .tag-num {
        color: #3a84ff;
      }
    }

    &.disabled {
      color: #c4c6cc;
      cursor: not-allowed;

      &:hover {
        .tag-flag {
          color: #c4c6cc;
        }

        .tag-num {
          color: #c4c6cc;
        }
      }
    }

    &.edit:hover {
      .tag-num {
        display: none;
      }

      .edit-action {
        display: flex;
      }
    }

    &.active {
      color: #3a84ff;
      background: #e1ecff;

      .tag-flag {
        color: #3a84ff;
      }

      .tag-num {
        color: #3a84ff;
      }
    }

    &.edit-error {
      .bk-form-input {
        border-color: #ea3636 !important;
      }
    }

    .tag-flag {
      margin-right: 7px;
      color: #c4c6cc;
    }

    .tag-name {
      flex: 1;
      display: flex;
      align-items: center;
      height: 100%;
      padding-right: 10px;
      overflow: hidden;

      .name-text {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .tag-num-box {
      flex: 0 0 auto;
      margin-left: auto;
      color: #c4c6cc;

      .tag-loading {
        animation: "ani-rotate" 2s linear infinite;
      }
    }

    .edit-action {
      position: absolute;
      top: 0;
      right: 0;
      bottom: 0;
      display: none;
      align-items: center;
      justify-content: center;
      padding-right: 20px;
      padding-left: 10px;
      cursor: pointer;
    }

    .input-edit-info {
      position: absolute;
      top: 0;
      right: 20px;
      bottom: 0;
      z-index: 1;
      display: flex;
      align-items: center;
      padding: 0 10px;
      font-size: 16px;
      color: #ea3636;
    }
  }
</style>
