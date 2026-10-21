<template>
  <div class="issue-status-list">
    <div class="operation-bar">
      <bk-input
        v-model="idKeyword"
        class="filter-input"
        clearable
        placeholder="请输入 Issue ID 进行过滤"
        right-icon="bk-icon icon-search"
        @enter="handleFilterImmediately" />
    </div>
    <bk-table
      v-bkloading="{ isLoading: loading }"
      :data="list"
      :outer-border="false">
      <bk-table-column
        label="Issue ID"
        prop="id"
        width="200">
        <template slot-scope="{ row }">
          <a
            v-if="resolveIssueUrl(row.id)"
            :href="resolveIssueUrl(row.id)"
            rel="noopener noreferrer"
            target="_blank">
            {{ row.id }}
          </a>
          <span v-else>{{ displayText(row.id) }}</span>
        </template>
      </bk-table-column>
      <bk-table-column
        label="状态"
        prop="status"
        width="220">
        <template slot-scope="{ row }">
          <bk-tag
            v-if="row.status"
            :theme="statusTheme(row.status)">
            {{ row.status }}
          </bk-tag>
          <span v-else>{{ EMPTY_TEXT }}</span>
        </template>
      </bk-table-column>
      <bk-table-column
        label="状态描述"
        min-width="300"
        prop="statusDescription"
        show-overflow-tooltip>
        <template slot-scope="{ row }">
          {{ displayText(row.statusDescription) }}
        </template>
      </bk-table-column>
      <bk-table-column
        label="PR"
        width="100">
        <template slot-scope="{ row }">
          <a
            v-if="resolvePullRequestUrl(row.prId)"
            :href="resolvePullRequestUrl(row.prId)"
            rel="noopener noreferrer"
            target="_blank">
            前往PR
          </a>
        </template>
      </bk-table-column>
      <bk-table-column
        label="更新时间"
        prop="updateTime"
        width="180">
        <template slot-scope="{ row }">
          {{ formatTime(row.updateTime) }}
        </template>
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
      <div slot="empty">{{ emptyText }}</div>
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
    displayText,
    EMPTY_TEXT,
    formatTime,
    resolveIssueUrl,
    resolvePullRequestUrl,
  } from '../common/format';
  import { deleteIssueStatus, listIssueStatus } from '../service/issue-status';
  import { AuthError } from '../service/request';

  const POLL_INTERVAL = 3000;
  // 连续失败退避，防止OP重启期间每3秒刷一次失败请求
  const BACKOFF_INTERVALS = [3000, 6000, 12000, 30000];
  const FILTER_DEBOUNCE = 300;

  const STATUS_THEME = {
    issue_todo: 'default',
    issue_evaluate_doing: 'warning',
    issue_plan_doing: 'warning',
    issue_develop_doing: 'warning',
    issue_test_doing: 'warning',
    issue_pr_doing: 'warning',
    issue_done: 'success',
    issue_failed: 'danger',
  };

  export default {
    name: 'IssueStatusList',
    data() {
      return {
        list: [],
        total: 0,
        current: 1,
        limit: 20,
        limitList: [10, 20, 50, 100],
        idKeyword: '',
        loading: false,
        EMPTY_TEXT,
      };
    },
    computed: {
      emptyText() {
        return this.idKeyword.trim() ? '未匹配到 Issue，请调整过滤条件' : '暂无 Issue 状态数据';
      },
    },
    watch: {
      idKeyword() {
        clearTimeout(this.filterTimer);
        this.filterTimer = setTimeout(this.handleFilterImmediately, FILTER_DEBOUNCE);
      },
    },
    created() {
      // 以下均为非响应式的轮询控制状态，不放进 data
      this.pollTimer = null;
      this.filterTimer = null;
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
      clearTimeout(this.filterTimer);
      this.clearPollTimer();
      this.abortInFlight();
    },
    methods: {
      displayText,
      formatTime,
      resolveIssueUrl,
      resolvePullRequestUrl,
      statusTheme(status) {
        // status 是自由字符串，未命中映射表一律用默认灰色，绝不因未知状态而不渲染
        return STATUS_THEME[status] || 'default';
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
      /**
       * 用户主动操作（首次进入、改过滤词、翻页、切每页条数）走这里：
       * 取消在途请求、重置轮询计时、立即请求
       *
       * @param {boolean} silent 是否静默（不显示loading）
       */
      restart(silent) {
        this.clearPollTimer();
        this.abortInFlight();
        this.boundaryRetried = false;
        this.fetchList(silent);
      },
      handleFilterImmediately() {
        clearTimeout(this.filterTimer);
        this.current = 1;
        this.restart(false);
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
        // 在弹框前固定目标ID并暂停轮询，避免列表刷新后误删其他行
        const issueId = row.id;
        this.deleteConfirming = true;
        this.clearPollTimer();
        this.requestSeq += 1;
        this.abortInFlight();
        this.loading = false;

        this.$bkInfo({
          title: `确认删除 ${issueId}？`,
          subTitle: '将删除该 Issue 状态数据且不可恢复',
          theme: 'danger',
          maskClose: false,
          confirmFn: async () => {
            try {
              const result = await deleteIssueStatus(issueId);
              if (this.list.length === 1 && this.current > 1) {
                this.current -= 1;
              }
              this.$bkMessage({
                theme: 'success',
                message: result.deleted > 0 ? '删除成功' : '该 Issue 状态数据已不存在',
              });
              this.deleteConfirming = false;
              this.restart(false);
              return true;
            } catch (error) {
              this.$bkMessage({
                theme: 'error',
                message: error.message || '删除Issue状态数据失败',
              });
              // 返回false使确认框保持打开，用户可重试或取消
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
          // 后台标签页轮询纯属浪费，且定时器会被浏览器节流
          this.clearPollTimer();
          return;
        }
        if (this.stopped) {
          return;
        }
        // 恢复可见时用户最需要最新数据，立即静默刷新一次再恢复轮询
        this.restart(true);
      },
      /**
       * @param {boolean} silent 轮询触发时为true，不显示loading，避免表格每3秒闪一次
       */
      fetchList(silent) {
        this.requestSeq += 1;
        const seq = this.requestSeq;
        if (!silent) {
          this.loading = true;
        }
        const controller = new AbortController();
        this.abortController = controller;
        const params = {
          idKeyword: this.idKeyword.trim(),
          start: (this.current - 1) * this.limit,
          pageSize: this.limit,
        };
        let outdated = false;
        listIssueStatus(params, controller.signal)
          .then((data) => {
            // 丢弃过期响应：避免旧请求后到把新结果覆盖掉
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
              // 权限状态是稳定的，继续轮询无意义
              this.stopped = true;
              return;
            }
            this.failCount += 1;
            if (!this.hasShownError) {
              this.hasShownError = true;
              this.$bkMessage({
                theme: 'error',
                message: error.message || '查询Issue状态列表失败',
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
            // 一次请求完成后才调度下一次，任意时刻最多一个在途请求
            this.scheduleNext();
          });
      },
      applyData(data, params) {
        const page = data || {};
        this.list = page.data || [];
        this.total = page.total || 0;
        const outOfRange = this.list.length === 0 && this.total > 0 && params.start >= this.total;
        if (outOfRange && !this.boundaryRetried) {
          // 轮询期间数据可能因TTL过期而减少导致页码越界，回退到最后一页并只重试一次
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
  .issue-status-list .operation-bar {
    margin-bottom: 16px;
  }

  .issue-status-list .filter-input {
    width: 360px;
  }

  .issue-status-list .list-pagination {
    padding: 12px 16px;
    background: #fff;
  }
</style>
