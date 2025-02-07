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
    class="exec-detail-script-view"
    :class="{ loading: isLoading }">
    <detail-item :label="$t('template.脚本来源：')">
      {{ stepInfo.scriptSourceText }}
    </detail-item>
    <detail-item
      v-if="isReferScript"
      :label="$t('template.脚本引用：')">
      <span>{{ scriptName }}</span>
      <icon
        v-bk-tooltips="$t('template.脚本详情')"
        class="script-detail"
        type="jump"
        @click="handleGoScriptDetail" />
    </detail-item>
    <detail-item
      :label="$t('template.脚本内容：')"
      layout="vertical">
      <ace-editor
        :lang="language"
        :options="languageOption"
        readonly
        :value="scriptContent" />
    </detail-item>
    <div>
      <detail-item :label="$t('template.脚本参数：')">
        <jb-edit-textarea
          field="scriptParam"
          readonly
          :value="stepInfo.scriptParamText || '--'" />
      </detail-item>
      <detail-item
        v-if="stepInfo.windowsInterpreter"
        :label="$t('template.解释器：')">
        {{ stepInfo.windowsInterpreter }}
      </detail-item>
      <detail-item :label="$t('template.超时时长：')">
        {{ stepInfo.timeout }}（s）
      </detail-item>
      <detail-item :label="$t('template.错误处理：')">
        {{ stepInfo.ignoreErrorText }}
      </detail-item>
      <detail-item :label="$t('template.执行账号：')">
        {{ executeAccountText }}
      </detail-item>
    </div>
    <detail-item
      :label="$t('template.执行目标：')"
      layout="vertical">
      <ip-selector
        :complete-container-list="containerDetail(stepInfo.executeTarget.executeObjectsInfo.containerList)"
        :complete-host-list="hostsDetails(stepInfo.executeTarget.executeObjectsInfo.hostList)"
        readonly
        show-view />
    </detail-item>
    <slot />
  </div>
</template>
<script setup>
  import {
    computed,
    ref,
    shallowRef,
  } from 'vue';

  import { useRouter } from '@router';

  import AccountManageService from '@service/account-manage';
  import ScriptService from '@service/script-manage';

  import {
    formatScriptTypeValue,
  } from '@utils/assist';

  import AceEditor from '@components/ace-editor';
  import DetailItem from '@components/detail-layout/item';
  import JbEditTextarea from '@components/jb-edit/textarea';

  import {
    containerDetail,
    hostsDetails,
  } from '@blueking/ip-selector/dist/adapter';

  const props = defineProps({
    data: {
      type: Object,
      default: () => ({}),
    },
  });

  const router = useRouter();

  const language = formatScriptTypeValue(props.data.scriptStepInfo.scriptLanguage);
  const languageOption = [language];

  const stepInfo = shallowRef(props.data.scriptStepInfo);

  const executeAccountText = ref('');
  const scriptName = ref('');
  const scriptContent = ref(stepInfo.value.content);
  const scriptInfo = shallowRef({});
  const requestQueue = ref([]);

  const isLoading = computed(() => requestQueue.value.length > 0);
  const isReferScript = computed(() => props.data.scriptStepInfo.scriptSource
    && props.data.scriptStepInfo.scriptSource !== 1);

  /**
   * @desc 更新脚本版本获取版本详情
   */
  if (stepInfo.value.scriptVersionId) {
    requestQueue.value.push(true);
    ScriptService.versionDetail({
      id: stepInfo.value.scriptVersionId,
    }).then((data) => {
      scriptName.value = data.name;
      scriptContent.value = data.content;
      scriptInfo.value = data;
    })
      .finally(() => {
        requestQueue.value.pop();
      });
  }

  /**
   * @desc 获取完整的账号列表
   */
  AccountManageService.fetchAccountWhole()
    .then((data) => {
      const accountData = data.find(item => item.id === stepInfo.value.account);
      if (accountData) {
        executeAccountText.value = accountData.alias;
      } else {
        executeAccountText.value = '--';
      }
    })
    .finally(() => {
      requestQueue.value.pop();
    });
  /**
   * @desc 新开窗口跳转脚本版本列表
   */
  const handleGoScriptDetail = () => {
    const routerName = scriptInfo.value.publicScript ? 'publicScriptVersion' : 'scriptVersion';

    const routerResult = router.resolve({
      name: routerName,
      params: {
        id: stepInfo.value.scriptId,
      },
      query: {
        scriptVersionId: scriptInfo.value.scriptVersionId,
      },
    });
    window.open(routerResult.href);
  };
</script>
<style lang="postcss">
  .exec-detail-script-view {
    &.loading {
      height: calc(100vh - 100px);
    }

    .detail-item {
      margin-bottom: 0;
    }

    .script-detail {
      color: #3a84ff;
      cursor: pointer;
    }

    .script-update-flag {
      display: inline-block;

      .script-update {
        color: #ff5656;
      }
    }
  }
</style>
