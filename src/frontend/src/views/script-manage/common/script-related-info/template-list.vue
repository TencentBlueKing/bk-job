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
  <div>
    <jb-search-select
      ref="searchSelect"
      :data="searchSelect"
      :placeholder="$t('script.搜索名称，版本号')"
      :popover-zindex="99999"
      style="width: 100%; margin-bottom: 20px;"
      @on-change="handleSearch" />
    <div v-bkloading="{ isLoading }">
      <bk-table :data="renderList">
        <!-- 作业模板引用 -->
        <bk-table-column
          align="left"
          :label="$t('script.作业模板')"
          show-overflow-tooltip>
          <template slot-scope="{ row }">
            <bk-button
              text
              @click="handleGoTemplateDetail(row)">
              {{ row.taskTemplateName }}
            </bk-button>
          </template>
        </bk-table-column>
        <bk-table-column
          align="left"
          :label="$t('script.引用的版本号')"
          prop="scriptVersion" />
        <bk-table-column
          align="left"
          :label="$t('script.状态')"
          prop="scriptStatusDesc"
          width="120">
          <template slot-scope="{ row }">
            <span v-html="row.statusHtml" />
          </template>
        </bk-table-column>
        <empty
          v-if="isSearching"
          slot="empty"
          type="search">
          <div>
            <div style="font-size: 14px; color: #63656e;">
              {{ $t('搜索结果为空') }}
            </div>
            <div style="margin-top: 8px; font-size: 12px; line-height: 16px; color: #979ba5;">
              <span>{{ $t('可以尝试调整关键词') }}</span>
              <span>{{ $t('或') }}</span>
              <bk-button
                text
                @click="handleClearSearch">
                {{ $t('清空搜索条件') }}
              </bk-button>
            </div>
          </div>
        </empty>
      </bk-table>
    </div>
  </div>
</template>
<script>
  import PublicScriptService from '@service/public-script-manage';
  import ScriptService from '@service/script-manage';

  import {
    checkPublicScript,
    encodeRegexp,
  } from '@utils/assist';

  import JbSearchSelect from '@components/jb-search-select';

  import I18n from '@/i18n';

  export default {
    name: '',
    components: {
      JbSearchSelect,
    },
    props: {
      params: {
        type: Object,
        required: true,
      },
    },
    data() {
      return {
        isLoading: false,
        isSearching: false,
        wholeList: [],
        renderList: [],
      };
    },
    created() {
      this.publicScript = checkPublicScript(this.$route);
      this.serviceHandler = this.publicScript ? PublicScriptService : ScriptService;

      this.fetchData();

      this.searchSelect = [
        {
          name: I18n.t('script.名称'),
          id: 'name',
          default: true,
        },
        {
          name: I18n.t('script.版本号_colHead'),
          id: 'scriptVersion',
        },
      ];
    },
    methods: {
      /**
       * @desc 获取关联脚本列表
       */
      fetchData() {
        this.$request(this.serviceHandler.citeInfo(this.params), () => {
          this.isLoading = true;
        }).then(({ citedTemplateList }) => {
          this.wholeList = Object.freeze(citedTemplateList);
          this.renderList = this.wholeList;
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
      /**
       * @desc 本地搜索
       * @param {Object} payload 搜索条件
       */
      handleSearch(payload) {
        let list = this.wholeList;
        Object.keys(payload).forEach((key) => {
          const reg = new RegExp(encodeRegexp(payload[key]), 'i');
          let realKey = key;
          if (key === 'name') {
            realKey = 'taskTemplateName';
          }
          list = list.filter(item => reg.test(item[realKey]));
        });
        this.isSearching = Object.keys(payload).length > 0;
        this.renderList = Object.freeze(list);
      },
      handleClearSearch() {
        this.$refs.searchSelect.reset();
        this.handleSearch({});
      },
      /**
       * @desc 查看引用脚本的作业模板详情
       * @param {Object} payload 应用字段数据
       *
       * 需要解析资源的 scopeType、scopeId
       */
      handleGoTemplateDetail(payload) {
        const { href } = this.$router.resolve({
          name: 'templateDetail',
          params: {
            id: payload.taskTemplateId,
          },
        });
        window.open(href.replace(/^\/[^/]+\/\d+/, `/${payload.scopeType}/${payload.scopeId}`));
      },
    },
  };
</script>
