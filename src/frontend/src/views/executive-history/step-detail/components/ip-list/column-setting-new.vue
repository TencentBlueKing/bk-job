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
  <bk-popover
    ref="popoverRef"
    placement="bottom-start"
    theme="light"
    trigger="click"
    :width="450">
    <div
      id="stepDetailIpListSettingBtn"
      class="select-btn">
      <i class="bk-icon icon-cog-shape" />
    </div>
    <div
      slot="content"
      class="list-column-setting">
      <div class="select-body">
        <div class="title">
          {{ $t('history.字段显示设置') }}
        </div>
        <bk-checkbox
          :checked="isAllColumn"
          :indeterminate="isIndeterminate"
          @click.native="handleToggleAll">
          {{ $t('history.全选') }}
        </bk-checkbox>
        <bk-checkbox-group v-model="tempAllShowColumn">
          <template v-for="item in columnList">
            <span
              v-if="item.name === 'ipv4'"
              :key="`ip_${item.name}`"
              v-bk-tooltips="{
                content: $t('history.IP 与 IPv6 至少需保留一个'),
                disabled: tempAllShowColumn.includes('ipv6'),
              }"
              class="select-column">
              <bk-checkbox
                :checked="item.checked"
                :disabled="!tempAllShowColumn.includes('ipv6')"
                :value="item.name">
                {{ item.label }}
              </bk-checkbox>
            </span>
            <span
              v-else-if="item.name === 'ipv6'"
              :key="`ipv6_${item.name}`"
              v-bk-tooltips="{
                content: $t('history.IP 与 IPv6 至少需保留一个'),
                disabled: tempAllShowColumn.includes('ipv4'),
              }"
              class="select-column">
              <bk-checkbox
                :checked="item.checked"
                :disabled="!tempAllShowColumn.includes('ipv4')"
                :value="item.name">
                {{ item.label }}
              </bk-checkbox>
            </span>
            <bk-checkbox
              v-else
              :key="item.name"
              :checked="item.checked"
              class="select-column"
              :value="item.name">
              {{ item.label }}
            </bk-checkbox>
          </template>
        </bk-checkbox-group>
      </div>
      <div class="select-footer">
        <bk-button
          theme="primary"
          @click="handleSubmitSetting">
          {{ $t('history.确定') }}
        </bk-button>
        <bk-button @click="handleHideSetting">
          {{ $t('history.取消') }}
        </bk-button>
      </div>
    </div>
  </bk-popover>
</template>
<script setup>
  import {
    computed,
    ref,
  } from 'vue';

  const props = defineProps({
    columnList: {
      type: Array,
      required: true,
    },
    value: {
      type: Array,
      required: true,
    },
  });
  const emits = defineEmits([
    'change',
    'close',
  ]);

  const popoverRef = ref();
  const tempAllShowColumn = ref([...props.value]);

  const isIndeterminate = computed(() => tempAllShowColumn.value.length !== props.columnList.length);

  const isAllColumn = computed(() => tempAllShowColumn.value.length === props.columnList.length);

  const handleToggleAll = () => {
    if (isAllColumn.value) {
      tempAllShowColumn.value = props.columnList.reduce((result, item) => {
        if (item.disabled) {
          result.push(item.name);
        }
        return result;
      }, []);
    } else {
      tempAllShowColumn.value = props.columnList.map(item => item.name);
    }
  };

  const handleSubmitSetting = () => {
    emits('change', [...tempAllShowColumn.value]);
    popoverRef.value.hideHandler();
  };

  const handleHideSetting = () => {
    popoverRef.value.hideHandler();
  };
</script>
<style lang="postcss" scoped>
  .select-btn{
    width: 45px;
    height: 40px;
    padding: 0;
    font-size: 14px;
    color: #979ba5;
    text-align: center;
    cursor: pointer;
    border-left: 1px solid #dcdee5;
  }

  .list-column-setting {
    .select-body {
      padding: 15px 22px 30px;

      .title {
        margin-bottom: 22px;
        font-size: 16px;
        color: #313238;
      }
    }

    .select-column {
      display: inline-block;
      margin-top: 20px;
      margin-right: 36px;
      vertical-align: middle;

      &:last-child {
        margin-right: 0;
      }
    }

    .select-footer {
      display: flex;
      height: 50px;
      background: #fafbfd;
      border-top: 1px solid #dbdde4;
      align-items: center;
      justify-content: center;

      .bk-button {
        margin: 0 5px;
      }
    }
  }
</style>

