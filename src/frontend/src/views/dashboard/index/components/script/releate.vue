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
  <card-layout
    v-bkloading="{ isLoading, opacity: 0.8 }"
    class="script-ralate-dashboard"
    :title="$t('dashboard.使用率')"
    :title-tips="$t('dashboard.被作业模板引用的脚本总数（去重）/ 脚本总数，比率越高代表脚本在作业的使用率越高')">
    <div class="nums">
      {{ rate }}
    </div>
  </card-layout>
</template>
<script>
  import StatisticsService from '@service/statistics';

  import CardLayout from '../card-layout';

  export default {
    name: '',
    components: {
      CardLayout,
    },
    data() {
      return {
        isLoading: true,
        rate: '0 %',
      };
    },
    watch: {
      date() {
        this.fetchData();
      },
    },
    created() {
      this.fetchData();
    },
    methods: {
      fetchData() {
        this.isLoading = true;
        StatisticsService.fetchScriptCiteInfo()
          .then((data) => {
            const {
              citedScriptCount,
              scriptCount,
            } = data;
            if (scriptCount < 1) {
              this.rate = '0 %';
            } else {
              this.rate = `${Math.round(citedScriptCount / scriptCount * 100).toFixed(2)} %`;
            }
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
    },
  };
</script>
<style lang='postcss' scoped>
  .script-ralate-dashboard {
    .nums {
      font-size: 24px;
      font-weight: 500;
      line-height: 32px;
      color: #63656e;
    }
  }
</style>
