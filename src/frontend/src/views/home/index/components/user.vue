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
    class="user-box">
    <div class="user-name">
      Hi, {{ userInfo.username }}
    </div>
    <greeting />
    <div
      class="work-tips"
      @mouseleave="handleBeginSwiper"
      @mousemove="handleStopSwiper">
      <div
        class="work-tips-container"
        :style="workTipsStyles">
        <div
          v-for="(analysis, index) in analysisList"
          :key="index"
          class="item">
          <div
            @click="handleShowList"
            v-html="analysis.description" />
        </div>
      </div>
    </div>
    <jb-dialog
      v-model="isShowList"
      class="home-analysis-detail-dialog"
      :title="listInfo.dialogTitle"
      :width="520">
      <div class="list-wraper">
        <div class="data-row-header">
          <div class="td-name">
            {{ listInfo.columnName }}
          </div>
          <div class="td-action">
            {{ $t('home.操作') }}
          </div>
        </div>
        <div
          v-for="(item, index) in listData"
          :key="`${item.id}_${index}`"
          class="data-row">
          <div class="td-name">
            {{ item.content }}
          </div>
          <div
            class="td-action"
            @click="handleGoDetail(item)">
            {{ $t('home.查看详情') }}
          </div>
        </div>
      </div>
      <template #footer>
        <bk-button
          class="list-close-btn"
          @click="handleHideList">
          {{ $t('home.关闭') }}
        </bk-button>
      </template>
    </jb-dialog>
  </div>
</template>
<script>
  import _ from 'lodash';
  import marked from 'marked';

  import StatisticsIndexService from '@service/statistics-index';
  import UserService from '@service/user';

  import Greeting from './greeting';

  import I18n from '@/i18n';

  const dialogTitleMap = {
    ForbiddenScriptFinder: I18n.t('home.使用禁用脚本的作业模板/执行方案'),
    TaskPlanTargetChecker: I18n.t('home.存在异常 Agent 的执行方案'),
    TimerTaskFailRateWatcher: I18n.t('home.周期成功率低于60%的定时任务'),
    TimerTaskFailWatcher: I18n.t('home.近期执行失败的定时任务'),
  };
  const columnNameMap = {
    ForbiddenScriptFinder: I18n.t('home.作业模板/执行方案'),
    TaskPlanTargetChecker: I18n.t('home.作业执行方案'),
    TimerTaskFailRateWatcher: I18n.t('home.定时任务'),
    TimerTaskFailWatcher: I18n.t('home.定时任务'),
  };

  export default {
    components: {
      Greeting,
    },
    data() {
      return {
        isLoading: true,
        isShowList: false,
        dialogTitle: '',
        analysisIndex: 0,
        userInfo: {},
        analysisList: {},
        greetingInfo: {},
        listType: '',
        listData: [],
      };
    },
    computed: {
      workTipsStyles() {
        const classes = {
          transform: `translateY(${-100 * this.analysisIndex}%)`,
        };
        if (this.analysisIndex === 0) {
          classes.transition = 'unset';
        }
        return classes;
      },
      listInfo() {
        return {
          dialogTitle: dialogTitleMap[this.listType],
          columnName: columnNameMap[this.listType],
        };
      },
    },
    created() {
      this.analysisResultTimer = '';
      this.analysisMap = {};
      Promise.all([
        this.fetchUserInfo(),
        this.fetchAnalysisResult(),
      ]).finally(() => {
        this.isLoading = false;
      });
    },
    methods: {
      fetchUserInfo() {
        return UserService.fetchUserInfo()
          .then((data) => {
            this.userInfo = Object.freeze(data);
          });
      },
      fetchAnalysisResult() {
        return StatisticsIndexService.fetchAnalysisResult()
          .then((data) => {
            const analysisList = [];
            const analysisMap = {};
            data.forEach((item) => {
              let description = _.trim(marked.parse(item.description), '\n');
              if (dialogTitleMap[item.analysisTaskCode]) {
                description = description.replace(/(?=<\/p>$)/, `<span data-id="${item.id}" class="action-list">${I18n.t('home.查看列表')}</span>`);
              }
              const analysis = {
                ...item,
                description,
              };
              analysisList.push(analysis);
              analysisMap[analysis.id] = analysis;
            });
            this.analysisList = Object.freeze(analysisList);
            this.analysisMap = analysisMap;
            this.swiperAnalysisResult();
          });
      },
      handleHideList() {
        this.isShowList = false;
      },
      handleShowList(event) {
        const $target = event.target;
        if ($target.className !== 'action-list') {
          return;
        }
        const id = $target.getAttribute('data-id');
        this.listType = this.analysisMap[id].analysisTaskCode;
        this.listData = Object.freeze(this.analysisMap[id].contents);
        this.isShowList = true;
      },
      handleGoDetail(payload) {
        let router = {};
        if (payload.type === 'TEMPLATE') {
          router = this.$router.resolve({
            name: 'templateDetail',
            params: {
              id: payload.location.content,
            },
          });
        }
        if (payload.type === 'TASK_PLAN') {
          const [templateId, planId] = payload.location.content.split(',');
          router = this.$router.resolve({
            name: 'viewPlan',
            params: {
              templateId,
            },
            query: {
              viewPlanId: planId,
            },
          });
        }
        if (payload.type === 'TIMER_TASK') {
          router = this.$router.resolve({
            name: 'cronList',
            query: {
              name: payload.location.content,
              mode: 'detail',
            },
          });
        }
        window.open(router.href);
      },
      handleStopSwiper() {
        clearTimeout(this.analysisResultTimer);
      },
      handleBeginSwiper() {
        this.swiperAnalysisResult();
      },
      swiperAnalysisResult() {
        if (this.analysisList.length < 1) {
          return;
        }
        clearTimeout(this.analysisResultTimer);
        this.analysisResultTimer = setTimeout(() => {
          this.swiperAnalysisResult();
          if (this.isShowList) {
            return;
          }
          if (this.analysisIndex < this.analysisList.length - 1) {
            this.analysisIndex += 1;
          } else {
            this.analysisIndex = 0;
          }
        }, 3000);
      },
    },
  };
</script>
<style lang='postcss'>
  .user-box {
    line-height: 20px;

    .user-name {
      font-size: 24px;
      line-height: 1;
      color: #313238;
    }

    .work-tips {
      width: 100%;
      height: 60px;
      overflow: hidden;

      .work-tips-container {
        display: flex;
        flex-direction: column;
        height: 100%;
        transition: all 0.15s;
      }

      .action-list {
        color: #3a84ff;
        cursor: pointer;
      }

      .item {
        /* stylelint-disable value-no-vendor-prefix */
        display: -webkit-box;
        overflow: hidden;
        text-overflow: ellipsis;
        -webkit-box-orient: vertical;
        flex: 0 0 100%;
        -webkit-line-clamp: 3;
      }

      code {
        padding: 2px 4px;
        font-family: Consolas, monospace, tahoma, Arial;
        color: #d14;
        border: 1px solid #e1e1e8;
      }

      em {
        font-style: italic;
      }

      a:hover,
      a:active {
        outline: 0;
      }

      a:focus {
        outline: thin dotted #333;
        outline: 5px auto -webkit-focus-ring-color;
        outline-offset: -2px;
      }

      a:hover {
        color: #5590cc;
        text-decoration: underline;
      }

      a {
        color: #6293e2;
        text-decoration: none;
      }
    }
  }

  .home-analysis-detail-dialog {
    .bk-dialog-wrapper .bk-dialog-body {
      padding: 0;
    }

    .bk-dialog-header-inner {
      text-align: left;
    }

    .list-wraper {
      max-height: 450px;
      overflow-y: auto;
      line-height: 41px;
    }

    .data-row-header {
      display: flex;
      background: #fafbfd;
      border-top: 1px solid #dcdee5;

      .td-name,
      .td-action {
        color: #313238;
      }
    }

    .data-row {
      display: flex;
      font-size: 12px;
      color: #63656e;
      border-top: 1px solid #dcdee5;

      .td-action {
        color: #3a84ff;
        cursor: pointer;
      }
    }

    .td-name {
      width: 390px;
      padding-left: 24px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .td-action {
      flex: 0 0 100px;
      margin-left: auto;
    }

    .list-close-btn {
      width: 76px;
    }
  }

</style>
