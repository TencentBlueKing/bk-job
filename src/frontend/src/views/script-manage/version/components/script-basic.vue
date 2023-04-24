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
    ref="wraper"
    v-bkloading="{ isLoading }"
    class="script-version-basic-box">
    <template v-if="data.id">
      <div class="script-type-flag">
        <img :src="`/static/images/script/${data.typeName}.svg`">
      </div>
      <div
        class="detail-column"
        style="width: 270px;">
        <div class="detail-col">
          <div class="item-label">
            {{ $t('script.脚本名：') }}
          </div>
          <div class="item-value">
            <auth-component
              auth="script/edit"
              :resource-id="data.id">
              <jb-edit-input
                field="scriptName"
                :remote-hander="val => handleUpdateScript('scriptName', val)"
                style="width: 100%;"
                :value="data.name" />
              <div slot="forbid">
                {{ data.name }}
              </div>
            </auth-component>
          </div>
        </div>
        <div class="detail-col">
          <div class="item-label">
            {{ $t('script.更新人：') }}
          </div>
          <div class="item-value">
            <div
              v-bk-overflow-tips
              class="text-box">
              {{ data.lastModifyUser }}
            </div>
          </div>
        </div>
        <div class="detail-col">
          <div class="item-label">
            {{ $t('script.创建人：') }}
          </div>
          <div class="item-value">
            <div
              v-bk-overflow-tips
              class="text-box">
              {{ data.creator }}
            </div>
          </div>
        </div>
      </div>
      <div
        class="detail-column"
        style="width: 282px;">
        <div class="detail-col">
          <div class="item-label">
            {{ $t('script.脚本语言：') }}
          </div>
          <div class="item-value">
            <div class="text-box">
              {{ data.typeName }}
            </div>
          </div>
        </div>
        <div class="detail-col">
          <div class="item-label">
            {{ $t('script.更新时间：') }}
          </div>
          <div class="item-value">
            <div
              v-bk-overflow-tips
              class="text-box">
              {{ data.lastModifyTime }}
            </div>
          </div>
        </div>
        <div class="detail-col">
          <div class="item-label">
            {{ $t('script.创建时间：') }}
          </div>
          <div
            v-bk-overflow-tips
            class="item-value">
            <div class="text-box">
              {{ data.createTime }}
            </div>
          </div>
        </div>
      </div>
      <div class="detail-column last">
        <div class="detail-col">
          <div class="item-label">
            {{ $t('script.场景标签：') }}
          </div>
          <div class="item-value">
            <auth-component
              auth="script/edit"
              :resource-id="data.id">
              <jb-edit-tag
                class="input"
                field="scriptTags"
                :remote-hander="val => handleUpdateScript('scriptTags', val)"
                :rows="1"
                :value="data.tags" />
              <div slot="forbid">
                {{ data.tagText }}
              </div>
            </auth-component>
          </div>
        </div>
        <div class="detail-col">
          <div class="item-label">
            {{ $t('script.脚本描述：') }}
          </div>
          <div class="item-value">
            <jb-edit-textarea
              field="scriptDesc"
              :maxlength="500"
              :placeholder="$t('script.在此处标注该脚本的备注和使用说明')"
              :remote-hander="val => handleUpdateScript('scriptDesc', val)"
              :rows="1"
              single-row-render
              :value="data.description" />
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
<script>
  import PublicScriptService from '@service/public-script-manage';
  import ScriptService from '@service/script-manage';

  import { checkPublicScript } from '@utils/assist';

  import JbEditInput from '@components/jb-edit/input';
  import JbEditTag from '@components/jb-edit/tag';
  import JbEditTextarea from '@components/jb-edit/textarea';

  export default {
    name: '',
    components: {
      JbEditInput,
      JbEditTag,
      JbEditTextarea,
    },
    data() {
      return {
        isLoading: true,
        data: {},
      };
    },
    created() {
      this.publicScript = checkPublicScript(this.$route);
      this.serviceHandler = this.publicScript ? PublicScriptService : ScriptService;
      this.scriptId = this.$route.params.id;
      this.fetchScriptBasic();
    },
    methods: {
      /**
       * @desc 获取脚本基本信息
       */
      fetchScriptBasic() {
        this.isLoading = true;
        this.serviceHandler.fetchBasicInfo({
          id: this.scriptId,
        }).then((data) => {
          this.data = Object.freeze(data);
          this.calcLableWidth();
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
      /**
       * @desc 计算脚本lable的宽度
       */
      calcLableWidth() {
        this.$refs.wraper.querySelectorAll('.detail-column').forEach((columnEl) => {
          const $lableEles = columnEl.querySelectorAll('.item-label');
          let maxWidth = 0;
          $lableEles.forEach((ele) => {
            const { width } = ele.getBoundingClientRect();
            maxWidth = Math.max(maxWidth, width);
          });
          $lableEles.forEach((ele) => {
            ele.style.width = `${maxWidth + 2}px`;
          });
        });
      },
      /**
       * @desc 更新脚本基本信息
       * @param {String} field 指定更新的字段名
       * @param {Object} payload 更新的字段key和value
       */
      handleUpdateScript(field, payload) {
        return this.serviceHandler.scriptUpdateMeta({
          id: this.scriptId,
          ...payload,
          updateField: field,
        });
      },
    },
  };
</script>
<style lang='postcss'>
    .script-version-basic-box {
      position: relative;
      display: flex;
      height: 128px;
      padding: 16px 24px;
      background: #fff;
      box-shadow: 0 1px 2px 0 rgb(0 0 0 / 16%);

      .script-type-flag {
        flex: 0 0  auto;
        margin-right: 20px;

        img {
          width: 100%;
          height: 100%;
        }
      }

      .detail-column {
        display: flex;
        line-height: 30px;
        flex-direction: column;

        &.last {
          flex: 1;
        }

        .item-label {
          flex: 0 0 auto;
          color: #b2b5bd;
        }

        .item-value {
          flex: 1;
          width: 0;
          padding-right: 20px;
          color: #63656e;

          .text-box {
            height: 32px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }
      }

      .detail-col {
        display: flex;
      }
    }
</style>
