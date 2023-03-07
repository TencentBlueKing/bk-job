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
  <div
    v-bkloading="{ isLoading }"
    class="job-execute-record">
    <template v-if="!isLoading">
      <bk-table
        v-if="data.length > 0"
        :data="data">
        <bk-table-column
          :label="$t('history.时间')"
          prop="createTime"
          width="180" />
        <bk-table-column
          :label="$t('history.操作人')"
          prop="operator"
          width="120" />
        <bk-table-column
          :label="$t('history.操作')"
          prop="operationName"
          width="120" />
        <bk-table-column
          class-name="step-name"
          :label="$t('history.步骤')"
          prop="stepName"
          show-overflow-tooltip />
        <bk-table-column :label="$t('history.详情')">
          <template slot-scope="{ row }">
            <template v-if="row.detailEnable">
              <span v-if="chekcIsCurrentUrl(row)">
                ({{ $t('history.当前正在显示的内容') }})
              </span>
              <bk-button
                v-else
                text
                theme="primary"
                @click="handleView(row)">
                {{ row.detail }}
              </bk-button>
            </template>
            <div v-else>
              {{ row.detail }}
            </div>
          </template>
        </bk-table-column>
      </bk-table>
      <empty
        v-else
        class="empty" />
    </template>
  </div>
</template>
<script>
  import TaskExecuteService from '@service/task-execute';

  import Empty from '@components/empty';

  export default {
    name: '',
    components: {
      Empty,
    },
    props: {
      id: {
        type: [Number, String],
        default: 0,
      },
      from: {
        type: String,
        default: '',
      },
    },
    data() {
      return {
        isLoading: true,
        data: [],
      };
    },
    created() {
      this.fetchTaskOperationLog();
    },
    methods: {
      fetchTaskOperationLog(id) {
        this.$request(TaskExecuteService.fetchTaskOperationLog({
          id: this.id,
        }), () => {
          this.isLoading = true;
        }).then((data) => {
          this.data = data;
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
      chekcIsCurrentUrl(data) {
        const urlSearch = new URLSearchParams(window.location.search);
        const stepInstanceId = urlSearch.get('stepInstanceId');
        const retryCount = urlSearch.get('retryCount');

        return parseInt(stepInstanceId, 10) === data.stepInstanceId
          && parseInt(retryCount, 10) === data.retry;
      },
      handleView(payload) {
        const routerInfo = {
          name: 'historyStep',
          params: {
            taskInstanceId: payload.taskInstanceId,
          },
          query: {
            stepInstanceId: payload.stepInstanceId,
            batch: payload.batch,
            retryCount: payload.retry,
            from: this.from || this.$route.query.from,
          },
        };

        this.$emit('on-change');
        this.$router.push(routerInfo);
      },
    },
  };
</script>
<style lang='postcss'>
  .job-execute-record {
    min-height: calc(100vh - 120px);

    .empty {
      padding-top: 80px;
    }
  }
</style>
