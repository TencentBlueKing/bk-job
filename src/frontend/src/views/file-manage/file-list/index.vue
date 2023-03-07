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
  <div class="file-node-list-page">
    <list-action-layout>
      <jb-breadcrumb
        :key="`${fileSourceInfo.alias}_${path}`"
        class="list-breadcrumb"
        @on-last="handleBackLast">
        <jb-breadcrumb-item>
          <icon
            style="font-size: 20px;"
            type="folder-open" />
          <span @click="handleGoFileSource">{{ $t('file.文件源列表') }}</span>
        </jb-breadcrumb-item>
        <jb-breadcrumb-item>
          <span @click="handlePathLocation('')">{{ fileSourceInfo.alias }}</span>
        </jb-breadcrumb-item>
        <jb-breadcrumb-item
          v-for="(item) in pathStack"
          :key="item.path"
          role="path">
          <span @click="handlePathLocation(item.path)">{{ item.name }}</span>
        </jb-breadcrumb-item>
      </jb-breadcrumb>
      <template #right>
        <jb-input
          enter-trigger
          :placeholder="$t('file.搜索关键字')"
          style="width: 480px;"
          :value="name"
          @submit="handleSearch" />
      </template>
    </list-action-layout>
    <render-list
      ref="list"
      :data-source="getBucketList"
      @on-refresh="handleListRefresh">
      <render-file-list-column
        v-for="(column, index) in renderColumns"
        :key="`${path}_${index}`"
        :action-handler="handleAction"
        :column="column"
        :file-source-id="fileSourceId"
        :link-handler="handleLink"
        @on-action="handleAction"
        @on-link="handleLink" />
    </render-list>
  </div>
</template>
<script>
  import _ from 'lodash';

  import FileService from '@service/file';

  import ListActionLayout from '@components/list-action-layout';
  import RenderFileListColumn from '@components/render-file-list-column';
  import RenderList from '@components/render-list';

  import I18n from '@/i18n';

  export default {
    name: 'FileList',
    components: {
      ListActionLayout,
      RenderList,
      RenderFileListColumn,
    },
    data() {
      return {
        renderColumns: [],
        path: '',
        fileSourceInfo: {},
      };
    },
    computed: {
      isSkeletonLoading() {
        return this.$refs.list.isLoading;
      },
      /**
       * @desc 面包屑路径
       * @return {Array}
       */
      pathStack() {
        return this.path.split('/').reduce((result, item) => {
          if (item) {
            const last = result.length > 0 ? result[result.length - 1].path : '';

            result.push({
              path: `${last}${item}/`,
              name: item,
            });
          }
          return result;
        }, []);
      },
    },
    created() {
      this.parseUrl();
      this.getBucketList = FileService.fetchgetListFileNode;
    },
    mounted() {
      this.fetchData();
    },
    methods: {
      /**
       * @desc 获取bucket储存桶数据
       */
      fetchData() {
        this.$refs.list.$emit('onFetch', {
          fileSourceId: this.fileSourceId,
          path: this.path,
          name: this.name,
        });
      },
      /**
       * @desc 解析url参数
       */
      parseUrl() {
        const {
          fileSourceId = '',
          path = '',
          name = '',
        } = this.$route.query;

        this.fileSourceId = fileSourceId;
        this.path = path;
        this.name = name;
      },
      /**
       * @desc 文件路径返回上一级
       */
      handleBackLast() {
        const lastPath = this.pathStack[this.pathStack.length - 2];
        this.handlePathLocation(lastPath.path);
      },
      /**
       * @desc 表格数据刷新回调
       * @param {Object} data 用户输入的过滤数据
       *
       * 重新拉取数据
       */
      handleListRefresh(data) {
        // 过滤掉 checkbox 列
        this.renderColumns = Object.freeze(data.metaData.properties.filter(_ => _.type !== 'checkbox'));
        this.fileSourceInfo = Object.freeze(data.fileSourceInfo);
      },
      /**
       * @desc 搜索
       * @param {String} name 搜索文件名
       *
       * 重新拉取数据
       */
      handleSearch(name) {
        this.name = name;
        this.fetchData();
      },
      /**
       * @desc 跳转到文件源列表
       */
      handleGoFileSource() {
        this.$router.push({
          name: 'sourceFileList',
        });
      },
      /**
       * @desc 面包屑切换列表
       * @param {String} path 文件路径
       */
      handlePathLocation(path) {
        if (_.trim(path, '/') === _.trim(this.path, '/')) {
          return;
        }
        this.name = '';
        this.path = path;
        this.fetchData();
      },
      /**
       * @desc 行数据跳转链接
       * @param {String} path 文件路径
       */
      handleLink(path) {
        this.name = '';
        this.path = path;
        this.fetchData();
      },
      /**
       * @desc 行数据执行操作
       * @param {String} actionCode 操作类型code
       * @param {Object} params 操作所需参数
       */
      handleAction(actionCode, params) {
        return FileService.executeAction({
          fileSourceId: this.fileSourceId,
          actionCode,
          params,
        }).then(() => {
          this.messageSuccess(I18n.t('file.操作成功'));
          this.fetchData();
        });
      },
      /**
       * @desc 跳转到文件源列表页面
       */
      routerBack() {
        this.$router.push({
          name: 'sourceFileList',
        });
      },
    },
  };
</script>
<style lang="postcss">
  .file-node-list-page {
    .list-action-layout {
      .right-box {
        flex: 1;
      }

      .list-breadcrumb {
        width: 100%;
      }
    }
  }
</style>
