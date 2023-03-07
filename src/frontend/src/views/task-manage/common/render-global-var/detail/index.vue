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
  <div class="job-variable-detail">
    <table class="info-table">
      <tr
        v-for="(item, index) in describeMap"
        :key="index"
        class="variable-item">
        <td class="info-label">
          {{ item.label }}
        </td>
        <td class="info-value">
          <jb-edit-textarea
            :field="item.filed"
            readonly
            :value="data[item.filed]" />
        </td>
      </tr>
    </table>
    <template v-if="data.isHost">
      <!-- <server-panel
                style="margin-top: 20px;"
                detail-fullscreen
                :host-node-info="data.defaultTargetValue.hostNodeInfo" /> -->
      <ip-selector
        readonly
        show-view
        :value="data.defaultTargetValue.hostNodeInfo" />
    </template>
  </div>
</template>
<script>
  import GlobalVariableModel from '@model/task/global-variable';

  // import ServerPanel from '@components/choose-ip/server-panel';
  import JbEditTextarea from '@components/jb-edit/textarea';

  import I18n from '@/i18n';

  const type = () => ({ label: I18n.t('template.变量类型'), filed: 'typeText' });
  const name = () => ({ label: I18n.t('template.变量名称'), filed: 'name' });
  const defaultValue = (defaultField = I18n.t('template.初始值')) => ({ label: defaultField, filed: 'valueText' });
  const description = () => ({ label: I18n.t('template.变量描述'), filed: 'description' });
  const changeable = () => ({ label: I18n.t('template.赋值可变'), filed: 'changeableText' });
  const required = () => ({ label: I18n.t('template.执行时必填'), filed: 'requiredText' });

  const generateVariableDescribeMap = (defaultField = I18n.t('template.初始值')) => ({
    [GlobalVariableModel.TYPE_STRING]: [type(), name(), defaultValue(defaultField), description(), changeable(), required()],
    [GlobalVariableModel.TYPE_NAMESPACE]: [type(), name(), defaultValue(defaultField), description(), required()],
    [GlobalVariableModel.TYPE_HOST]: [type(), name(), defaultValue(defaultField), description(), required()],
    [GlobalVariableModel.TYPE_PASSWORD]: [type(), name(), defaultValue(defaultField), description(), required()],
    [GlobalVariableModel.TYPE_RELATE_ARRAY]: [type(), name(), defaultValue(defaultField), description(), required()],
    [GlobalVariableModel.TYPE_INDEX_ARRAY]: [type(), name(), defaultValue(defaultField), description(), required()],
  });

  export default {
    name: 'GlobalVarView',
    components: {
      // ServerPanel,
      JbEditTextarea,
    },
    props: {
      data: {
        type: Object,
        default() {
          return {};
        },
      },
      defaultField: {
        type: String,
        default: I18n.t('template.初始值'),
      },
    },
    computed: {
      describeMap() {
        return generateVariableDescribeMap(this.defaultField)[this.data.type];
      },
    },
  };
</script>
<style lang='postcss' scoped>
  .job-variable-detail {
    padding-bottom: 20px;
    font-size: 14px;
    color: #63656e;

    .info-table {
      width: 100%;
      line-height: 24px;

      td {
        padding-top: 9px;
        padding-bottom: 9px;
        border: 1px solid #dde4eb;
      }
    }

    .info-label {
      width: 110px;
      padding-right: 20px;
      color: #b2b5bd;
      text-align: right;
    }

    .info-value {
      padding-left: 21px;
      word-break: break-all;
    }
  }
</style>
