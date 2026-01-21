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
  <div class="exception-box">
    <img
      alt=""
      :src="notFoundImage">
    <p>{{ $t('没找到页面！') }}</p>
    <div>
      <p v-if="$i18n.locale === 'en-US'">
        The page you are trying to access does not exist, will redirect to
        <router-link :to="{ name: 'home' }">
          Homepage
        </router-link>
        in
        <span style="font-weight: bold;">{{ timeout }}</span>
        seconds.
      </p>
      <p v-else>
        <span>你访问的页面不存在，将在</span>
        <span style="font-weight: bold;">{{ timeout }}</span>
        <span>秒后重定向到</span>
        <router-link :to="{ name: 'home' }">
          首页
        </router-link>
        <span>。</span>
      </p>
    </div>
  </div>
</template>
<script setup>
  import {
    ref,
  } from 'vue';

  import { useRouter } from '@router';


  const notFoundImage = window.__loadAssetsUrl__('/static/images/404.png');
  const router = useRouter();
  const timeout = ref(4);

  const goHome = () => {
    if (timeout.value === 1) {
      router.replace({
        name: 'home',
      });
      return;
    }
    timeout.value = timeout.value - 1;
    setTimeout(goHome, 1000);
  };

  goHome();
</script>
<style scoped lang="postcss">
  .exception-box {
    margin: auto;
    text-align: center;

    img {
      width: 300px;
      margin-top: 150px;
    }

    p {
      margin: 32px 0;
      font-size: 20px;
      font-weight: 400;
      color: #979797;
    }
  }
</style>
