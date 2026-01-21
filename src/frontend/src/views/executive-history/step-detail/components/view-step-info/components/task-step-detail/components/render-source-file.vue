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
  <div class="render-server-file">
    <bk-collapse
      v-if="isShowLocalFile || isShowServerFile || isShowSourceFile"
      v-model="activeResult"
      class="host-detail">
      <jb-collapse-item
        v-if="isShowLocalFile"
        :active="activeResult"
        name="local">
        <span class="collapse-title">
          {{ $t('template.已选择') }}
          <span class="number strong">{{ localFileList.length }}</span>
          {{ $t('template.个本地文件') }}
        </span>
        <template #content>
          <table>
            <tbody>
              <tr
                v-for="(row, index) in localFileList"
                :key="index">
                <td style="width: 40%; word-break: break-all;">
                  {{ row.fileLocationText }}
                </td>
                <td>{{ row.fileSizeText }}</td>
              </tr>
            </tbody>
          </table>
        </template>
      </jb-collapse-item>
      <jb-collapse-item
        v-if="isShowSourceFile"
        :active="activeResult"
        name="source">
        <span class="collapse-title">
          {{ $t('template.已选择') }}
          <span class="number strong">{{ sourceFileList.length }}</span>
          {{ $t('template.个文件源文件') }}
        </span>
        <template #content>
          <table>
            <thead>
              <th style="width: 40%;">
                {{ $t('文件名称') }}
              </th>
              <th>{{ $t('文件源_text') }}</th>
            </thead>
            <tbody class="source-file-list">
              <tr
                v-for="(row, index) in sourceFileList"
                :key="index">
                <td>
                  <render-file-path :data="row.fileLocation" />
                </td>
                <td>
                  <span
                    class="source-file-alias"
                    @click="handleGoSource(row, index)">
                    {{ fileSourceAliasList[index] }}
                    <icon
                      class="source-file-icon"
                      svg
                      type="edit" />
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </template>
      </jb-collapse-item>
      <jb-collapse-item
        v-if="isShowServerFile"
        :active="activeResult"
        name="server">
        <span class="collapse-title">
          {{ $t('template.已选择') }}
          <span class="number strong">{{ serverFileList.length }}</span>
          {{ $t('template.个服务器文件') }}
        </span>
        <template #content>
          <table>
            <thead>
              <th style="width: 40%;">
                {{ $t('template.文件路径') }}
              </th>
              <th style="width: 15%;">
                {{ $t('template.服务器列表') }}
              </th>
              <th>{{ $t('template.Agent 状态') }}</th>
              <th style="width: 20%;">
                {{ $t('template.服务器账号') }}
              </th>
            </thead>
            <tbody>
              <tr
                v-for="(row, index) in serverFileList"
                :key="index">
                <td>
                  <render-file-path :data="row.fileLocation" />
                </td>
                <td>
                  <render-file-server :data="row" />
                </td>
                <td>
                  <server-host-agent
                    :host-list="row.host.executeObjectsInfo.hostList"
                    :title="$t('template.服务器文件-服务器列表')" />
                </td>
                <td>{{ findAccountAlias(row.account) }}</td>
              </tr>
            </tbody>
          </table>
        </template>
      </jb-collapse-item>
    </bk-collapse>
  </div>
</template>
<script setup>
  import {
    computed,
    shallowRef,
    watch,
  } from 'vue';

  import { useRouter } from '@router';

  import FileManageService from '@service/file-source-manage';

  import JbCollapseItem from '@components/jb-collapse-item';

  import SourceFileVO from '@domain/variable-object/source-file';

  import RenderFilePath from './render-file-path';
  import RenderFileServer from './render-file-server';
  import ServerHostAgent from './server-host-agent.vue';

  const props = defineProps({
    data: {
      type: Array,
      default: () => [],
    },
    account: {
      type: Array,
      default: () => [],
    },
  });

  const router = useRouter();

  const activeResult = shallowRef([]);
  const localFileList = shallowRef([]);
  const serverFileList = shallowRef([]);
  const sourceFileList = shallowRef([]);
  const fileSourceIdsList = shallowRef([]);
  const fileSourceAliasList = shallowRef([]);

  const isShowLocalFile = computed(() => localFileList.value.length > 0);
  const isShowServerFile = computed(() => serverFileList.value.length > 0);
  const isShowSourceFile = computed(() => sourceFileList.value.length > 0);

  watch(() => props.data, () => {
    props.data.forEach((fileItem) => {
      const fileSource = new SourceFileVO(fileItem);
      if (fileSource.isServerFile) {
        serverFileList.value.push(fileSource);
      } else if (fileSource.isLocalFile) {
        localFileList.value.push(fileSource);
      } else if (fileSource.isSourceFile) {
        sourceFileList.value.push(fileSource);
        const idLists = []; // 文件源ID
        sourceFileList.value.forEach((e) => {
          idLists.push(e.fileSourceId);
          fileSourceIdsList.value = idLists;
        });
      }
      if (localFileList.value.length > 0) {
        activeResult.value = ['local'];
      } else if (serverFileList.value.length > 0) {
        activeResult.value = ['server'];
      }
    });
  }, {
    immediate: true,
  });

  watch(() => fileSourceIdsList.value, () => {
    if (fileSourceIdsList.value.length) {
      const promiseList = fileSourceIdsList.value.map(id => FileManageService.getSourceInfo({
        id,
      }));
      Promise.all(promiseList).then((res) => {
        res.forEach((e) => {
          fileSourceAliasList.value.push(e.alias);
        });
      });
    }
  }, {
    immediate: true,
  });

  const findAccountAlias = (payload) => {
    const accountData = props.account.find(item => item.id === payload);
    if (accountData) {
      return accountData.alias;
    }
    return '';
  };

  const handleGoSource = (payload, index) => {
    const { fileSourceId } = payload;
    const sourceAlias = fileSourceAliasList.value[index];
    const { href } = router.resolve({
      name: 'bucketList',
      query: {
        fileSourceId,
        sourceAlias,
      },
    });
    window.open(href, '_blank');
  };

</script>
<style lang='postcss'>
  @import url("@/css/mixins/scroll");

  .render-server-file {
    flex: 1;

    .bk-collapse-item-header {
      display: flex;
      align-items: center;
      padding-left: 23px;

      .collapse-title {
        padding-left: 23px;
      }
    }

    table {
      width: 100%;
      line-height: 20px;
      background: #fff;

      tr:nth-child(n+2) {
        td {
          border-top: 1px solid #dcdee5;
        }
      }

      th,
      td {
        height: 42px;
        padding-top: 5px;
        padding-bottom: 5px;
        padding-left: 16px;
        font-size: 12px;
        text-align: left;

        &:first-child {
          padding-left: 60px;
        }
      }

      th {
        font-weight: normal;
        color: #313238;
        border-bottom: 1px solid #dcdee5;
      }

      td {
        color: #63656e;

        .file-path {
          display: inline-block;
          max-width: 100%;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }

    .source-file-alias {
      cursor: pointer;

      &:hover {
        color: #3a84ff !important;

        .source-file-icon {
          display: inline;
        }
      }
    }

    .source-file-icon {
      display: none;
    }

    .bk-table-empty-block {
      display: none;
    }

    .source-file-tips-box {
      max-width: 300px;
      max-height: 280px;
      min-width: 60px;
      overflow-y: auto;

      @mixin scroller;

      .row {
        word-break: break-all;
      }

      .dot {
        display: inline-block;
        width: 6px;
        height: 6px;
        background: currentcolor;
        border-radius: 50%;
      }
    }
  }
</style>
