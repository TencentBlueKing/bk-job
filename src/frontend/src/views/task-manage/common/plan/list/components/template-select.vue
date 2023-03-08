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
  <jb-dialog
    class="template-select-dialog"
    :draggable="false"
    :esc-close="false"
    header-position="left"
    :mask-close="false"
    render-directive="if"
    :title="$t('template.新建执行方案')"
    :value="value"
    :width="480"
    @cancel="handleCancel">
    <jb-form
      ref="form"
      form-type="vertical"
      :model="formData"
      :rules="rules">
      <jb-form-item
        :label="$t('template.作业模板')"
        property="templateId"
        required>
        <bk-select
          v-model="formData.templateId"
          :clearable="false"
          :placeholder="$t('template.请选择作业模板')"
          :remote-method="fetchData"
          searchable>
          <bk-option
            v-for="item in templateList"
            :id="item.id"
            :key="item.id"
            :name="item.name" />
          <template slot="extension">
            <auth-component auth="job_template/create">
              <div
                style="cursor: pointer;"
                @click="handleCreate">
                <i class="bk-icon icon-plus-circle" />{{ $t('template.新建模板') }}
              </div>
              <div slot="forbid">
                <i class="bk-icon icon-plus-circle" />{{ $t('template.新建模板') }}
              </div>
            </auth-component>
          </template>
        </bk-select>
      </jb-form-item>
    </jb-form>
    <div slot="footer">
      <auth-button
        :auth="formData.templateId ? 'job_plan/create' : ''"
        class="mr10"
        :disabled="!formData.templateId"
        :resource-id="formData.templateId"
        theme="primary"
        @click="handleSubmit">
        {{ $t('template.确定') }}
      </auth-button>
      <bk-button @click="handleCancel">
        {{ $t('template.取消') }}
      </bk-button>
    </div>
  </jb-dialog>
</template>
<script>
  import TaskManageService from '@service/task-manage';

  import {
    leaveConfirm,
  } from '@utils/assist';

  import I18n from '@/i18n';

  export default {
    name: '',
    props: {
      value: {
        type: Boolean,
        default: false,
      },
    },
    data() {
      return {
        templateList: [],
        formData: {
          templateId: '',
        },
      };
    },
    watch: {
      value: {
        handler(value) {
          if (value) {
            this.fetchData();
          }
        },
        immediate: true,
      },
    },
    created() {
      this.rules = {
        templateId: [
          {
            required: true,
            message: I18n.t('template.请选择作业模板'),
            trigger: 'blur',
          },
        ],
      };
    },
    methods: {
      fetchData(search) {
        TaskManageService.taskList({
          name: search,
          start: 0,
          pageSize: 10,
        }).then((data) => {
          this.templateList = Object.freeze(data.data);
        });
      },
      handleCreate() {
        const router = this.$router.resolve({
          name: 'templateCreate',
        });
        window.open(router.href);
      },
      handleSubmit() {
        this.$refs.form.validate()
          .then(() => {
            window.changeFlag = false;
            this.handleCancel();
            this.$emit('on-change', this.formData.templateId);
          });
      },
      handleCancel() {
        leaveConfirm()
          .then(() => {
            this.$emit('input', false);
          });
      },
    },
  };
</script>
<style lang='postcss'>
  .template-select-dialog {
    .bk-dialog-header {
      padding-bottom: 0 !important;
    }

    .bk-form-item:last-child {
      margin-bottom: 0 !important;
    }
  }
</style>
