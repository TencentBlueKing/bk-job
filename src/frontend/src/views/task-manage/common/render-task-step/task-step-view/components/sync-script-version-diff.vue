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
    class="sync-script-version-diff"
    :style="{
      'z-index': zIndex,
    }">
    <div v-bkloading="{ isLoading }">
      <div class="header">
        <div class="title">
          {{ oldVersionScript.name }}
        </div>
        <div class="diff-info">
          <div
            class="diff-del"
            @click="handleViewDel">
            <span class="before" />
            <span class="after" />
            <span>{{ $t('删除') }}（{{ del }}）</span>
          </div>
          <div
            class="diff-change"
            @click="handleViewChange">
            <span class="before" />
            <span class="after" />
            <span>{{ $t('变换') }}（{{ change }}）</span>
          </div>
          <div
            class="diff-ins"
            @click="handleViewIns">
            <span class="before" />
            <span class="after" />
            <span>{{ $t('新增.diff') }}（{{ ins }}）</span>
          </div>
        </div>
      </div>
      <div style="padding: 0 16px;">
        <div class="version-select-layout">
          <div class="version-left">
            引用脚本版本（{{ oldVersionScript.version }}）
          </div>
          <div class="version-right">
            最新脚本版本（{{ lastVersionScript.version }}）
          </div>
        </div>
        <scroll-faker class="content-wraper">
          <jb-diff
            ref="diff"
            class="diff-details"
            :context="Infinity"
            format="side-by-side"
            :language="language"
            :new-content="lastVersionScript.content"
            :old-content="oldContent"
            theme="dark" />
        </scroll-faker>
      </div>
    </div>
    <i
      class="bk-icon icon-close"
      @click="handleClose" />
  </div>
</template>
<script>
  import { Base64 } from 'js-base64';

  import PublicScriptService from '@service/public-script-manage';
  import ScriptService from '@service/script-manage';

  export default {
    props: {
      oldVersionScript: {
        type: Object,
      },
    },
    data() {
      return {
        isLoading: true,
        oldContent: '',
        newContent: '',
        lastVersionScript: {},
        zIndex: 'auto',
        del: 0,
        ins: 0,
        change: 0,
      };
    },
    created() {
      this.scrollTopMemo = 0;
      this.insElements = [];
      this.insIndex = 0;
      this.delElements = [];
      this.delIndex = 0;
      this.changeElements = [];
      this.changeIndex = 0;
      this.fetchData();
    },
    mounted() {
      this.zIndex = window.__bk_zIndex_manager.nextZIndex(); // eslint-disable-line no-underscore-dangle
      document.body.append(this.$el);
      window.addEventListener('keydown', this.handleEsc);
      this.resetBodyStyle();

      this.$once('hook:beforeDestroy', () => {
        window.removeEventListener('keydown', this.handleEsc);
        try {
          document.body.removeChild(this.$el);
        } catch {
          console.log('error');
        }
      });
    },
    methods: {
      fetchData() {
        this.isLoading = true;
        this.oldContent = Base64.decode(this.oldVersionScript.content);
        this.language = this.oldVersionScript.typeName;

        const requestHandler = this.oldVersionScript.publicScript ? PublicScriptService : ScriptService;
        requestHandler.getOneOnlineScript({
          id: this.oldVersionScript.id,
          publicScript: this.oldVersionScript.publicScript,
        })
          .then((data) => {
            this.lastVersionScript = data;
            this.statisticsDiff();
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      statisticsDiff() {
        this.$nextTick(() => {
          const $contentTarget = this.$refs.diff.$el.querySelectorAll('.d2h-file-side-diff');
          const $newTarget = $contentTarget[1];// eslint-disable-line prefer-destructuring
          const $allChangeElemennts = Array.from($newTarget.querySelectorAll('td.d2h-code-side-linenumber.d2h-ins'));
          $allChangeElemennts.pop();

          this.insElements = [];
          this.ins = 0;
          this.insIndex = 0;
          this.changeElements = [];
          this.change = 0;
          this.changeIndex = 0;

          $allChangeElemennts.forEach((ele) => {
            if (ele.classList.contains('d2h-change')) {
              this.changeElements.push(ele);
              this.change += 1;
            } else {
              this.insElements.push(ele);
              this.ins += 1;
            }
          });

          const $dels = $newTarget.querySelectorAll('td.d2h-code-side-linenumber.d2h-code-side-emptyplaceholder');
          this.delElements = $dels;
          this.del = $dels.length;
          this.delIndex = 0;

          this.lineElements = $newTarget.querySelectorAll('td.d2h-code-side-linenumber');
        });
      },
      lineViewReset() {
        this.lineElements.forEach((item) => {
          item.classList.remove('active');
        });
      },
      handleViewDel() {
        this.lineViewReset();
        if (this.del < 1) {
          return;
        }
        const $target = this.delElements[this.delIndex];
        $target.scrollIntoView();
        $target.classList.add('active');
        this.changeIndex = 0;
        this.insIndex = 0;
        this.delIndex += 1;
        if (this.delIndex >= this.del) {
          this.delIndex = 0;
        }
      },
      handleViewChange() {
        this.lineViewReset();
        if (this.change < 1) {
          return;
        }
        const $target = this.changeElements[this.changeIndex];
        $target.scrollIntoView();
        $target.classList.add('active');
        this.insIndex = 0;
        this.delIndex = 0;
        this.changeIndex += 1;
        if (this.changeIndex >= this.change) {
          this.changeIndex = 0;
        }
      },
      handleViewIns() {
        this.lineViewReset();
        if (this.ins < 1) {
          return;
        }
        const $target = this.insElements[this.insIndex];
        $target.scrollIntoView();
        $target.classList.add('active');
        this.delIndex = 0;
        this.changeIndex = 0;
        this.insIndex += 1;
        if (this.insIndex >= this.ins) {
          this.insIndex = 0;
        }
      },
      handleEsc(event) {
        if (event && event.code === 'Escape') {
          this.handleClose();
        }
      },
      handleClose() {
        this.$emit('on-change', {
          [this.oldVersion]: true,
          [this.newVersion]: true,
        });
        this.$emit('close');
      },
      resetBodyStyle() {
        this.scrollTopMemo = document.scrollingElement.scrollTop;
        document.scrollingElement.style.overflow = 'hidden';
      },
      recoveryBodyStyle() {
        document.scrollingElement.style.overflow = 'initial';
        document.scrollingElement.scrollTop = this.scrollTopMemo;
      },
    },
  };
</script>
<style lang="postcss">
  .sync-script-version-diff {
    position: fixed;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    padding: 8px 24px;
    background-color: #fff;

    .header {
      display: flex;
      align-items: center;
      padding-right: 16px;

      .title {
        font-size: 20px;
        line-height: 28px;
        color: #313238;
      }

      .diff-info {
        display: flex;
        margin-top: 15px;
        margin-left: auto;
        font-size: 12px;
        line-height: 1em;
        align-items: center;

        .diff-del,
        .diff-change,
        .diff-ins {
          display: flex;
          margin-left: 30px;
          cursor: pointer;
          user-select: none;
        }

        .diff-del {
          color: #bd5c58;

          .before {
            background: #b1615b;
          }

          .after {
            background: #dcdcdc;
          }
        }

        .diff-change {
          color: #63656e;

          .before {
            background: #b1615b;
          }

          .after {
            background: #9aad76;
          }
        }

        .diff-ins {
          color: #9aad76;

          .before {
            background: #dcdcdc;
          }

          .after {
            background: #9aad76;
          }
        }

        .before,
        .after {
          width: 1em;
          height: 1em;
        }

        .after {
          margin-right: 6px;
          margin-left: 1px;
        }
      }
    }

    .version-select-layout {
      display: flex;
      height: 48px;
      margin-top: 18px;
      background: #eaebf0;
      border-radius: 2px 2px 0 0;

      .version-left,
      .version-right {
        display: flex;
        padding: 0 8px;
        flex: 0 0 50%;
        align-items: center;
      }

      .version-left {
        border-right: 1px solid #dcdee5;
      }
    }

    .version-selector {
      width: 300px;
    }

    .content-wraper {
      max-height: calc(100vh - 92px);
    }

    .d2h-file-wrapper {
      border: none;
    }

    .diff-details {
      position: relative;
      background-color: #fff;
    }

    .d2h-code-side-linenumber {
      line-height: 20px;

      &.active::before {
        float: left;
        width: 10px;
        height: 10px;
        margin-top: 5px;
        margin-left: 13px;
        background: #666;
        border-radius: 50%;
        content: "";
      }
    }

    .icon-close {
      position: absolute;
      top: 8px;
      right: 8px;
      display: flex;
      width: 26px;
      height: 26px;
      font-size: 22px;
      color: #979ba5;
      cursor: pointer;
      border-radius: 50%;
      transition: all 0.1s;
      align-items: center;
      justify-content: center;

      &:hover {
        background-color: #f0f1f5;
      }
    }
  }
</style>
