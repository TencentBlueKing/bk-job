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
    v-bkloading="{ isLoading }"
    class="job-detail"
    :class="{
      'is-loading': isLoading,
    }">
    <bk-alert :title="$t('template.同步执行方案需要重新确认定时任务的全局变量，不使用的定时任务可以直接停用。')" />
    <div class="title">
      <span>「{{ info.name }}」{{ $t('template.的全局变量') }}</span>
      <span
        v-if="!data.enable"
        class="disable">{{ $t('template.已停用') }}</span>
      <span
        v-else-if="data.hasConfirm"
        class="confirm">{{ $t('template.已确认') }}</span>
      <span
        v-else
        class="waiting">{{ $t('template.待确认') }}</span>
    </div>
    <div v-if="!isLoading">
      <empty v-if="isEmpty">
        <p>{{ $t('template.无关联的全局变量') }}</p>
        <p style="margin-top: 8px;">
          {{ $t('template.已直接确认') }}
        </p>
      </empty>
      <global-variable-layout v-else>
        <div>
          <global-variable
            v-for="variable in info.variableValue"
            :key="variable.id"
            ref="variable"
            :data="variable"
            :readonly="readonly"
            :type="variable.type" />
        </div>
        <div class="global-variable-action">
          <div class="variable-name">
            <span />
          </div>
          <div class="variable-value">
            <template v-if="isEditing">
              <bk-button
                class="job-button"
                :disabled="!data.enable"
                theme="primary"
                @click="handleSubmit">
                {{ $t('template.确认') }}
              </bk-button>
              <bk-button
                class="job-button"
                :disabled="!data.enable"
                @click="handleReset">
                {{ $t('template.重置') }}
              </bk-button>
            </template>
            <bk-button
              v-else
              class="job-button"
              :disabled="!data.enable"
              theme="primary"
              @click="handleToggleEdit">
              {{ $t('template.编辑') }}
            </bk-button>
          </div>
        </div>
      </global-variable-layout>
    </div>
  </div>
</template>
<script>
  import _ from 'lodash';

  import TimeTaskService from '@service/time-task';

  import Empty from '@components/empty';
  import GlobalVariable from '@components/global-variable/edit';
  import GlobalVariableLayout from '@components/global-variable/layout';

  import I18n from '@/i18n';

  export default {
    name: '',
    components: {
      GlobalVariableLayout,
      GlobalVariable,
      Empty,
    },
    props: {
      data: {
        type: Object,
        default: () => ({}),
      },
      variableList: {
        type: Array,
        required: true,
      },
    },
    data() {
      return {
        isLoading: false,
        isEmpty: false,
        isEditing: false,
        isSubmiting: false,
        info: {
          name: '',
          enable: true,
          hasConfirm: false,
          variableValue: [],
        },
      };
    },
    computed: {
      readonly() {
        if (!this.data.enable) {
          return true;
        }
        return this.isEditing ? false : this.info.hasConfirm;
      },
    },
    created() {
      if (!this.data.id) {
        return;
      }
      this.info = { ...this.data };
      this.isEditing = !this.info.hasConfirm;
      // 没有确认的定时任务，通过接口获取定时任务的name和变量
      // 默认展示模板的变量
      if (!this.info.hasConfirm) {
        this.info.variableValue = Object.freeze(_.cloneDeep(this.variableList));
        this.fetchData();
      }
    },
    methods: {
      fetchData() {
        this.isLoading = true;
        TimeTaskService.getDetail({
          id: this.data.id,
        }).then(({ name, variableValue }) => {
          this.info.name = name;
          // 作业模板中没有变量
          if (this.variableList.length < 1) {
            this.isEmpty = true;
            this.$emit('on-change', []);
            return;
          }
          // 同步作业模板中变量到定时任务
          // 作业模板和定时任务相同的变量——保留定时任务中的变量值
          const crontabVariableMap = variableValue.reduce((result, item) => {
            result[item.id] = item;
            return result;
          }, {});
          this.info.variableValue.forEach((variable) => {
            if (crontabVariableMap[variable.id]) {
              const { value, targetValue } = crontabVariableMap[variable.id];
              variable.defaultValue = value;
              variable.defaultTargetValue = targetValue;
            }
          });
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
      handleToggleEdit() {
        this.isEditing = true;
        this.$emit('on-update-confirm', false);
      },
      handleSubmit() {
        Promise.all(this.$refs.variable.map(item => item.validate()))
          .then((variableValue) => {
            window.changeFlag = false;
            this.isEditing = false;
            this.info.hasConfirm = true;
            this.info.variableValue = Object.freeze(variableValue);
            this.messageSuccess(I18n.t('template.定时任务确认成功'));
            this.$emit('on-change', Object.freeze(variableValue));
          });
      },
      handleReset() {
        this.$refs.variable.forEach(item => item.reset());
      },
    },
  };
</script>
<style lang='postcss' scoped>
  .job-detail {
    min-height: 300px;
    padding: 20px 40px;
    opacity: 100%;
    transition: opacity 0.1s;

    &.is-loading {
      opacity: 0%;
    }

    .title {
      display: flex;
      margin-top: 2px;
      margin-bottom: 30px;
      font-size: 18px;
      font-weight: bold;
      line-height: 54px;
      color: #000;
      border-bottom: 1px solid #f0f1f5;
      align-items: center;

      .disable,
      .confirm,
      .waiting {
        width: 42px;
        height: 17px;
        margin-left: 8px;
        font-size: 11px;
        line-height: 17px;
        text-align: center;
        border-radius: 2px;
      }

      .disable {
        color: #63656e;
        background: rgb(99 101 110 / 14%);
      }

      .confirm {
        color: rgb(45 203 157);
        background-color: rgb(45 203 157 / 14%);
      }

      .waiting {
        color: #ff9c01;
        background: rgb(255 156 1 / 14%);
      }
    }

    .global-variable-action {
      display: flex;
      margin-top: 10px;

      .variable-name {
        padding-right: 28px;
        box-sizing: content-box;
      }
    }

    .job-button {
      width: 100px;
      margin-right: 10px;
    }

    .job-empty {
      margin-top: 140px;
    }
  }
</style>
