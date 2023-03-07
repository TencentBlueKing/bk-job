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
  <div>
    <jb-search-select
      :data="searchSelect"
      :placeholder="$t('script.搜索名称，版本号')"
      :popover-zindex="99999"
      style="width: 100%; margin-bottom: 20px;"
      @on-change="handleSearch" />
    <div v-bkloading="{ isLoading }">
      <bk-table :data="renderList">
        <!-- 执行方案引用 -->
        <bk-table-column
          align="left"
          :label="$t('script.执行方案')"
          show-overflow-tooltip>
          <template slot-scope="{ row }">
            <bk-button
              text
              @click="handleGoPlanDetail(row)">
              {{ row.taskPlanName }}
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
          name: I18n.t('script.版本号.colHead'),
          id: 'scriptVersion',
        },
      ];
    },

    methods: {
      /**
       * @desc 获取关联脚本列表
       */
      fetchData() {
        this.isLoading = true;
        this.serviceHandler.citeInfo(this.params)
          .then(({ citedTaskPlanList }) => {
            this.wholeList = Object.freeze(citedTaskPlanList);
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
            realKey = 'taskPlanName';
          }
          list = list.filter(item => reg.test(item[realKey]));
        });
        this.renderList = Object.freeze(list);
      },
      /**
       * @desc 查看引用脚本的执行方案详情
       * @param {Object} payload 应用字段数据
       *
       * 需要解析资源的 scopeType、scopeId
       */
      handleGoPlanDetail(payload) {
        const { href } = this.$router.resolve({
          name: 'viewPlan',
          params: {
            templateId: payload.taskTemplateId,
          },
          query: {
            viewPlanId: payload.taskPlanId,
          },
        });
        window.open(href.replace(/^\/[^/]+\/\d+/, `/${payload.scopeType}/${payload.scopeId}`));
      },
    },
  };
</script>
