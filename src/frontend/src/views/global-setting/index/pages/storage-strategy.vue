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
    class="page-storage-strategy">
    <div class="wraper">
      <div class="execute-block">
        <div class="block-title">
          {{ $t('setting.执行历史保留') }}：
        </div>
        <jb-form
          ref="form"
          form-type="vertical"
          :model="formData"
          :rules="rules">
          <jb-form-item property="days">
            <bk-input
              v-model="formData.days"
              :min="1"
              type="number">
              <template slot="append">
                <div class="group-text">
                  {{ $t('setting.天') }}
                </div>
              </template>
            </bk-input>
          </jb-form-item>
        </jb-form>
      </div>
      <div class="action-box">
        <bk-button
          class="w120 mr10"
          :loading="isSubmiting"
          theme="primary"
          @click="handleSave">
          {{ $t('setting.保存') }}
        </bk-button>
        <bk-button @click="handleReset">
          {{ $t('setting.重置') }}
        </bk-button>
      </div>
    </div>
  </div>
</template>
<script>
  import GlobalSettingService from '@service/global-setting';

  import I18n from '@/i18n';

  export default {
    name: '',

    data() {
      return {
        isLoading: false,
        isSubmiting: false,
        formData: {
          days: 0,
        },
      };
    },
    created() {
      this.fetchData();
      this.memoDay = 0;
      this.rules = {
        days: [
          {
            validator: value => value >= 1,
            message: I18n.t('setting.保留天数必须大于0'),
            trigger: 'blur',
          },
        ],
      };
    },
    methods: {
      fetchData() {
        this.isLoading = true;
        GlobalSettingService.fetchHistroyExpire()
          .then((data) => {
            this.formData.days = data;
            this.memoDay = data;
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      handleSave() {
        this.$refs.form.validate().then(() => {
          this.isSubmiting = true;
          GlobalSettingService.updateHistroyExpire({
            ...this.formData,
          }).then(() => {
            this.messageSuccess(I18n.t('setting.设置执行保留时间成功'));
          })
            .finally(() => {
              this.isSubmiting = false;
            });
        });
      },
      handleReset() {
        this.formData.days = this.memoDay;
      },
    },
  };
</script>
<style lang='postcss'>
  .page-storage-strategy {
    display: flex;
    padding: 40px 0;
    justify-content: center;

    .wraper {
      width: 600px;
    }

    .storage-block {
      margin-bottom: 40px;
    }

    .storage-tips {
      margin-top: 10px;
      font-size: 12px;
      color: #63656e;
    }

    .action-box {
      margin-top: 10px;
    }
  }
</style>
