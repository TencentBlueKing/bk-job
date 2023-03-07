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
  <router-link
    class="work-statistics-box"
    :to="{ name: link }">
    <div class="work-flag">
      <icon
        class="hexagon"
        svg
        type="hexagon" />
      <icon
        class="statistics"
        svg
        :type="type" />
    </div>
    <div class="work-total">
      <slot
        :job-num="jobNum"
        :script-num="scriptNum" />
    </div>
    <div class="work-name">
      <slot name="name" />
    </div>
  </router-link>
</template>
<script>
  import HomeService from '@service/home';

  export default {
    name: '',
    props: {
      type: {
        type: String,
        default: '',
      },
      link: {
        type: String,
        default: '',
      },
    },
    data() {
      return {
        jobNum: 0,
        scriptNum: 0,
      };
    },
    created() {
      this.fetchStatisticsJobAndScript();
    },
    methods: {
      fetchStatisticsJobAndScript() {
        HomeService.fetchStatisticsJobAndScript()
          .then(({ jobNum, scriptNum }) => {
            this.jobNum = jobNum;
            this.scriptNum = scriptNum;
          });
      },
    },
  };
</script>
<style lang='postcss'>
  @keyframes hexagon-scale {
    0% {
      transform: scale(0);
    }

    70% {
      transform: scale(1.5);
    }

    80% {
      transform: scale(1.2);
    }

    90% {
      transform: scale(1.4);
    }

    100% {
      transform: scale(1.3);
    }
  }

  .work-statistics-box {
    display: flex;
    justify-content: center;
    flex-direction: column;
    align-items: center;
    padding-top: 18px;
    text-align: center;
    cursor: pointer;

    .work-flag {
      position: relative;
      display: flex;
      justify-content: center;
      width: 100px;
      height: 60px;
      margin-bottom: 15px;
    }

    .work-total {
      font-size: 24px;
      font-weight: bold;
      color: #313238;
    }

    .work-name {
      margin-top: 2px;
      color: #979ba5;
    }

    .hexagon {
      font-size: 60px;
      color: #e3edff;
    }

    .statistics {
      position: absolute;
      top: 15px;
      font-size: 30px;
      color: #3a84ff;
    }
  }
</style>
