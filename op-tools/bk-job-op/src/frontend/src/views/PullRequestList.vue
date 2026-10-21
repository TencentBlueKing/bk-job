<template>
  <div class="pull-request-list">
    <bk-table
      v-bkloading="{ isLoading: loading }"
      :data="list"
      :outer-border="false">
      <bk-table-column
        label="PR ID"
        prop="prId"
        width="100">
        <template slot-scope="{ row }">
          <a
            v-if="resolvePullRequestUrl(row.prId)"
            :href="resolvePullRequestUrl(row.prId)"
            rel="noopener noreferrer"
            target="_blank">
            {{ row.prId }}
          </a>
        </template>
      </bk-table-column>
      <bk-table-column
        label="codeDir"
        min-width="260"
        prop="codeDir"
        show-overflow-tooltip>
        <template slot-scope="{ row }">{{ emptyValue(row.codeDir) }}</template>
      </bk-table-column>
      <bk-table-column
        label="状态"
        width="120">
        <template slot-scope="{ row }">
          <bk-tag
            v-if="row.status"
            :theme="pullRequestStatusTheme(row.status)">
            {{ pullRequestStatusText(row.status) }}
          </bk-tag>
        </template>
      </bk-table-column>
      <bk-table-column
        label="agentName"
        min-width="140"
        prop="agentName">
        <template slot-scope="{ row }">{{ emptyValue(row.agentName) }}</template>
      </bk-table-column>
      <bk-table-column
        label="authorName"
        min-width="140"
        prop="authorName">
        <template slot-scope="{ row }">{{ emptyValue(row.authorName) }}</template>
      </bk-table-column>
      <bk-table-column
        label="会话ID"
        min-width="180"
        prop="imateConversationId"
        show-overflow-tooltip>
        <template slot-scope="{ row }">{{ emptyValue(row.imateConversationId) }}</template>
      </bk-table-column>
      <bk-table-column
        label="会话标题"
        min-width="240"
        prop="imateConversationTitle"
        show-overflow-tooltip>
        <template slot-scope="{ row }">{{ emptyValue(row.imateConversationTitle) }}</template>
      </bk-table-column>
      <bk-table-column
        label="创建时间"
        prop="createTime"
        width="180">
        <template slot-scope="{ row }">{{ formatTime(row.createTime) }}</template>
      </bk-table-column>
      <bk-table-column
        label="更新时间"
        prop="updateTime"
        width="180">
        <template slot-scope="{ row }">{{ formatTime(row.updateTime) }}</template>
      </bk-table-column>
      <bk-table-column
        fixed="right"
        label="操作"
        width="100">
        <template slot-scope="{ row }">
          <bk-button
            text
            theme="danger"
            @click="handleDelete(row)">
            删除
          </bk-button>
        </template>
      </bk-table-column>
      <div slot="empty">暂无 PR 数据</div>
    </bk-table>
    <bk-pagination
      class="list-pagination"
      align="right"
      :count="total"
      :current="current"
      :limit="limit"
      :limit-list="limitList"
      show-limit
      @change="handlePageChange"
      @limit-change="handleLimitChange" />
  </div>
</template>

<script>
  import {
    formatTime,
    resolvePullRequestUrl,
  } from '../common/format';
  import {
    pullRequestStatusText,
    pullRequestStatusTheme,
  } from '../common/pull-request';
  import { deletePullRequest, listPullRequests } from '../service/pull-request';
  import { AuthError } from '../service/request';

  const POLL_INTERVAL = 3000;
  const BACKOFF_INTERVALS = [3000, 6000, 12000, 30000];

  export default {
    name: 'PullRequestList',
    data() {
      return {
        list: [],
        total: 0,
        current: 1,
        limit: 20,
        limitList: [10, 20, 50, 100],
        loading: false,
      };
    },
    created() {
      this.pollTimer = null;
      this.requestSeq = 0;
      this.abortController = null;
      this.failCount = 0;
      this.hasShownError = false;
      this.stopped = false;
      this.boundaryRetried = false;
      this.deleteConfirming = false;
      this.onVisibilityChange = this.handleVisibilityChange.bind(this);
    },
    mounted() {
      document.addEventListener('visibilitychange', this.onVisibilityChange);
      this.restart(false);
    },
    beforeDestroy() {
      document.removeEventListener('visibilitychange', this.onVisibilityChange);
      this.clearPollTimer();
      this.abortInFlight();
    },
    methods: {
      formatTime,
      resolvePullRequestUrl,
      pullRequestStatusText,
      pullRequestStatusTheme,
      emptyValue(value) {
        return value === null || value === undefined ? '' : value;
      },
      clearPollTimer() {
        if (this.pollTimer) {
          clearTimeout(this.pollTimer);
          this.pollTimer = null;
        }
      },
      abortInFlight() {
        if (this.abortController) {
          this.abortController.abort();
          this.abortController = null;
        }
      },
      restart(silent) {
        this.clearPollTimer();
        this.abortInFlight();
        this.boundaryRetried = false;
        this.fetchList(silent);
      },
      handlePageChange(page) {
        this.current = page;
        this.restart(false);
      },
      handleLimitChange(limit) {
        this.limit = limit;
        this.current = 1;
        this.restart(false);
      },
      handleDelete(row) {
        const prId = row.prId;
        this.deleteConfirming = true;
        this.clearPollTimer();
        this.requestSeq += 1;
        this.abortInFlight();
        this.loading = false;

        this.$bkInfo({
          title: `确认删除 PR #${prId}？`,
          subTitle: '将删除该 PR 数据且不可恢复',
          theme: 'danger',
          maskClose: false,
          confirmFn: async () => {
            try {
              const result = await deletePullRequest(prId);
              if (this.list.length === 1 && this.current > 1) {
                this.current -= 1;
              }
              this.$bkMessage({
                theme: 'success',
                message: result.deleted > 0 ? '删除成功' : '该 PR 数据已不存在',
              });
              this.deleteConfirming = false;
              this.restart(false);
              return true;
            } catch (error) {
              this.$bkMessage({
                theme: 'error',
                message: error.message || '删除PR数据失败',
              });
              return false;
            }
          },
          afterLeaveFn: () => {
            if (this.deleteConfirming) {
              this.deleteConfirming = false;
              this.restart(true);
            }
          },
        });
      },
      handleVisibilityChange() {
        if (document.hidden) {
          this.clearPollTimer();
          return;
        }
        if (!this.stopped) {
          this.restart(true);
        }
      },
      fetchList(silent) {
        this.requestSeq += 1;
        const seq = this.requestSeq;
        if (!silent) {
          this.loading = true;
        }
        const controller = new AbortController();
        this.abortController = controller;
        const params = {
          start: (this.current - 1) * this.limit,
          pageSize: this.limit,
        };
        let outdated = false;
        listPullRequests(params, controller.signal)
          .then((data) => {
            if (seq !== this.requestSeq) {
              outdated = true;
              return;
            }
            this.failCount = 0;
            this.hasShownError = false;
            this.applyData(data, params);
          })
          .catch((error) => {
            if (seq !== this.requestSeq) {
              outdated = true;
              return;
            }
            if (error instanceof AuthError) {
              this.stopped = true;
              return;
            }
            this.failCount += 1;
            if (!this.hasShownError) {
              this.hasShownError = true;
              this.$bkMessage({
                theme: 'error',
                message: error.message || '查询PR数据列表失败',
              });
            }
          })
          .then(() => {
            if (outdated || seq !== this.requestSeq) {
              return;
            }
            if (!silent) {
              this.loading = false;
            }
            this.abortController = null;
            this.scheduleNext();
          });
      },
      applyData(data, params) {
        const page = data || {};
        this.list = page.data || [];
        this.total = page.total || 0;
        const outOfRange = this.list.length === 0 && this.total > 0 && params.start >= this.total;
        if (outOfRange && !this.boundaryRetried) {
          this.boundaryRetried = true;
          this.current = Math.max(1, Math.ceil(this.total / this.limit));
          this.fetchList(true);
          return;
        }
        if (this.list.length > 0) {
          this.boundaryRetried = false;
        }
      },
      scheduleNext() {
        this.clearPollTimer();
        if (this.stopped || this.deleteConfirming || document.hidden) {
          return;
        }
        const delay = this.failCount > 0
          ? BACKOFF_INTERVALS[Math.min(this.failCount - 1, BACKOFF_INTERVALS.length - 1)]
          : POLL_INTERVAL;
        this.pollTimer = setTimeout(() => {
          this.pollTimer = null;
          this.fetchList(true);
        }, delay);
      },
    },
  };
</script>

<style>
  .pull-request-list .list-pagination {
    padding: 12px 16px;
    background: #fff;
  }
</style>
