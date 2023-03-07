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
  <div v-bkloading="{ isLoading }">
    <bk-select
      ref="select"
      v-bind="$attrs"
      :clearable="false"
      ext-popover-cls="account-select-menu-list"
      searchable
      :value="value"
      @change="handleChange"
      @toggle="handleClick">
      <auth-option
        v-for="option in accountList"
        :id="option.id"
        :key="option.id"
        auth="account/use"
        :name="option.alias"
        :permission="option.canUse"
        :resource-id="option.id">
        <div>{{ option.alias }}</div>
      </auth-option>
      <template slot="extension">
        <auth-component auth="account/create">
          <div
            style="cursor: pointer;"
            @click="handleCreate">
            <i class="bk-icon icon-plus-circle mr10" />{{ $t('新建账号') }}
          </div>
          <div slot="forbid">
            <i class="bk-icon icon-plus-circle mr10" />{{ $t('新建账号') }}
          </div>
        </auth-component>
      </template>
    </bk-select>
  </div>
</template>
<script>
  import AccountManageService from '@service/account-manage';

  const ACCOUNT_SELECT_LAST = 'ACCOUNT_SELECT_LAST';

  export default {
    props: {
      value: {
        type: [
          Number, String,
        ],
      },
      type: {
        type: String,
        validator: value => [
          'system', 'db',
        ].includes(value),
      },
    },
    data() {
      return {
        isLoading: false,
        wholeList: [],
        systemAccountList: [],
        dbAccountList: [],
      };
    },
    computed: {
      /**
       * @desc 页面展示的账号列表
       */
      accountList() {
        if (this.type === 'system') {
          return this.systemAccountList;
        }
        if (this.type === 'db') {
          return this.dbAccountList;
        }
        return this.wholeList;
      },
    },
    watch: {
      /**
       * @desc 账号类型改变时重新显示账号列表
       */
      type() {
        this.initDefaultAccount();
      },
    },
    created() {
      this.fetchAppAccount();
    },
    methods: {
      /**
       * @desc 获取完整的账号列表
       */
      fetchAppAccount() {
        this.isLoading = true;
        AccountManageService.fetchAccountWhole()
          .then((data) => {
            const systemAccountList = [];
            const dbAccountList = [];
            data.forEach((account) => {
              if (account.isSystem) {
                systemAccountList.push(account);
              } else {
                dbAccountList.push(account);
              }
            });
            this.systemAccountList = Object.freeze(systemAccountList);
            this.dbAccountList = Object.freeze(dbAccountList);
            this.wholeList = Object.freeze(data);
            this.initDefaultAccount();
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      /**
       * @desc 账号列表的默认选中值
       */
      initDefaultAccount() {
        // 编辑状态
        // 现有账号不存在于当前账号列表中默认重置为空
        if (this.value) {
          if (!this.accountList.find(item => item.id === this.value)) {
            this.handleChange('');
          }
          return;
        }

        // 新建状态
        // 1，有缓存使用缓存
        const cacheAccountId = ~~localStorage.getItem(ACCOUNT_SELECT_LAST);
        if (cacheAccountId) {
          if (this.accountList.find(item => item.id === cacheAccountId)) {
            this.handleChange(cacheAccountId);
            return;
          }
        }
        // 2，查找是否有root账号
        const rootAccount = this.accountList.find(item => item.alias === 'root');
        if (rootAccount) {
          this.handleChange(rootAccount.id);
          return;
        }

        // 3，使用第一个账号
        const [firstAccount] = this.accountList;
        if (firstAccount) {
          this.handleChange(firstAccount.id);
        }
      },
      /**
       * @desc 跳转到创建账号页面
       */
      handleCreate() {
        const { href } = this.$router.resolve({
          name: 'accountList',
        });
        this.$refs.select.close();
        window.open(href);
      },
      /**
       * @desc select 的下面板展开时重新获取最新账号数据
       * @param {Boolean} toogle 下拉面板的展开收起状态
       */
      handleClick(toggle) {
        if (toggle) {
          this.fetchAppAccount();
        }
      },
      /**
       * @desc 用户选中账号
       * @param {Number} 选中的账号ID
       */
      handleChange(value) {
        if (value === this.value) {
          return;
        }
        localStorage.setItem(ACCOUNT_SELECT_LAST, value);
        this.$emit('input', value);
        this.$emit('change', value);
      },
    },
  };
</script>
