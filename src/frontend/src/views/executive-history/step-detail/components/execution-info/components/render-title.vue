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
    v-if="executeObject"
    class="render-execution-info-title"
    :class="taskExecuteDetail.result">
    <div
      v-if="executeObject.container"
      class="contaier-box">
      {{ executeObject.container.podName }}
    </div>
    <div
      v-if="executeObject.host"
      class="ip-box"
      :class="{
        'is-mult': executeObject.host.ip && executeObject.host.ipv6
      }">
      <div v-if="executeObject.host.ip">
        {{ executeObject.host.ip }}
      </div>
      <div v-if="executeObject.host.ipv6">
        {{ executeObject.host.ipv6 }}
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
  import { computed } from 'vue';

  const props = defineProps({
    taskExecuteDetail: {
      type: Object,
    },
  });

  const executeObject = computed(() => (props.taskExecuteDetail ? props.taskExecuteDetail.executeObject : undefined));
</script>
<style lang="postcss">
  .render-execution-info-title {
    display: flex;
    width: 325px;
    padding-left: 20px;
    overflow: hidden;
    line-height: 14px;
    cursor: default;
    flex-direction: column;
    justify-content: center;


    &.success,
    &.fail,
    &.running,
    &.waiting {
      &::before {
        position: absolute;
        top: 50%;
        left: 0;
        width: 3px;
        height: 12px;
        background: #2dc89d;
        content: "";
        transform: translateY(-50%);
      }
    }

    &.fail {
      &::before {
        background: #ea3636;
      }
    }

    &.running {
      &::before {
        background: #699df4;
      }
    }

    &.waiting {
      &::before {
        background: #dcdee5;
      }
    }

    .contaier-box{
      width: 100%;
      height: 20px;
      overflow: hidden;
      line-height: 20px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .ip-box {
      &.is-mult {
        font-size: 12px;
      }
    }
  }
</style>
