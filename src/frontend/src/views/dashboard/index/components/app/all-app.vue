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
  <card-layout
    class="app-item"
    :title="$t('dashboard.接入业务量')"
    :title-tips="$t('dashboard.在 JOB有任意执行过一次任务记录的业务')">
    <render-trend
      ref="trend"
      :date="date"
      metric="APP_COUNT" />
    <template slot="extend">
      <icon
        v-bk-tooltips="$t('dashboard.查看趋势图')"
        type="line-chart-line"
        @click="handleShowTrend" />
      <icon
        v-bk-tooltips="$t('dashboard.查看列表')"
        type="table-line"
        @click="handleShowList" />
    </template>
    <trend-dialog
      v-model="isShowTrend"
      :date="date"
      metric="APP_COUNT"
      :name="$t('dashboard.接入业务量')"
      :title="$t('dashboard.接入业务量趋势图')" />
    <lower-component
      :custom="isShowList"
      level="custom">
      <jb-dialog
        v-model="isShowList"
        header-position="left"
        :show-footer="false"
        :title="$t('dashboard.接入业务量列表')"
        :width="520">
        <div
          v-bkloading="{ isLoading: isListLoading, opacity: 0.8 }"
          style="margin-top: 12px;">
          <bk-table
            :data="listData"
            :max-height="420">
            <bk-table-column
              key="scopeName"
              :label="$t('dashboard.业务名')"
              prop="scopeName" />
          </bk-table>
        </div>
      </jb-dialog>
    </lower-component>
  </card-layout>
</template>
<script>
  import StatisticsService from '@service/statistics';

  import CardLayout from '../card-layout';
  import RenderTrend from '../common/render-trend';
  import TrendDialog from '../common/trend-dialog';

  export default {
    name: '',
    components: {
      CardLayout,
      RenderTrend,
      TrendDialog,
    },
    props: {
      date: {
        type: String,
        required: true,
      },
    },
    data() {
      return {
        isListLoading: false,
        listData: [],
        isShowTrend: false,
        isShowList: false,
      };
    },
    methods: {
      handleShowTrend() {
        this.isShowTrend = true;
      },
      handleShowList() {
        this.isShowList = true;
        this.isListLoading = true;
        StatisticsService.fetchListByPerAppMetrics({
          metric: 'APP_COUNT',
        }).then((data) => {
          this.listData = Object.freeze(data);
        })
          .finally(() => {
            this.isListLoading = false;
          });
      },
    },
  };
</script>
