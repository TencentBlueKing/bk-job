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
    class="operation-account">
    <jb-form
      :key="`${formData.category}_${formData.type}`"
      ref="operateAccountForm"
      v-test="{ type: 'form', value: 'createAccount' }"
      form-type="vertical"
      :model="formData"
      :rules="rules">
      <jb-form-item
        :label="$t('account.用途')"
        required
        style="margin-bottom: 20px;">
        <div class="radio-button-group-wraper">
          <bk-radio-group
            :value="formData.category"
            @change="handleCategoryChange">
            <bk-radio-button
              v-for="item in categoryList"
              :key="item.value"
              class="account-type-radio"
              :disabled="isEdit && formData.category !== item.value"
              :value="item.value">
              {{ item.name }}
            </bk-radio-button>
          </bk-radio-group>
        </div>
      </jb-form-item>
      <component
        :is="accountCom"
        :key="formData.category"
        :change="handleFieldChange"
        :form-data="formData"
        :is-edit="isEdit"
        :name-placeholder="namePlaceholder" />
    </jb-form>
  </div>
</template>
<script>
  import AccountManageService from '@service/account-manage';
  import QueryGlobalSettingService from '@service/query-global-setting';

  import AccountModel from '@model/account';

  import { accountAliasNameRule } from '@utils/validator';

  import AccountSelect from '@components/account-select';
  import JbInput from '@components/jb-input';

  import AccountDatabase from './account-strategy/database-account';
  import AccountOS from './account-strategy/os-account';

  import I18n from '@/i18n';

  const generatorDefault = () => ({
    id: '',
    account: '',
    alias: '',
    category: AccountModel.OS,
    dbPassword: '',
    dbPort: '',
    dbSystemAccountId: '',
    grantees: [],
    os: '',
    password: '',
    remark: '',
    type: AccountModel.TYPE_LINUX,
    rePassword: '',
  });

  export default {
    name: 'OperationAccount',
    components: {
      JbInput,
      AccountSelect,
    },
    props: {
      data: {
        type: Object,
        default: () => ({}),
      },
    },
    data() {
      return {
        isLoading: true,
        isEdit: false,
        isRulesLoadingError: false,
        formData: generatorDefault(),
        currentRules: Object.freeze({
          linux: {
            expression: '.',
          },
          windows: {
            expression: '.',
          },
          db: {
            expression: '.',
          },
        }),
      };
    },
    computed: {
      /**
       * @desc 账号分类对应的表单
       * @returns { Object }
       */
      accountCom() {
        const comMap = {
          [AccountModel.OS]: AccountOS,
          [AccountModel.DB]: AccountDatabase,
        };
        return comMap[this.formData.category];
      },
      /**
       * @desc 账号名称 Input 输入框的placeholder, 读账号名称规则的配置
       * @returns { Boolean }
       */
      namePlaceholder() {
        if (this.isLoading) {
          return '';
        }
        // DB 账号的命名规则
        if (this.formData.category === AccountModel.DB) {
          return this.currentRules.db.description;
        }
        // linux 账号的命名规则
        if (this.formData.type === AccountModel.TYPE_LINUX) {
          return this.currentRules.linux.description;
        }
        // windows 账号的命名规则
        return this.currentRules.windows.description;
      },
      rules() {
        if (this.isLoading) {
          return {};
        }
        const baseRule = {
          account: [
            {
              required: true,
              message: I18n.t('account.名称必填'),
              trigger: 'blur',
            },

          ],
          alias: [
            {
              required: true,
              message: I18n.t('account.别名必填'),
              trigger: 'blur',
            },
            {
              validator: accountAliasNameRule.validator,
              message: accountAliasNameRule.message,
              trigger: 'blur',
            },
          ],
        };
        // db账号
        if (this.formData.category === AccountModel.DB) {
          // DB 账号管理员配置规则
          baseRule.account.push({
            validator: (value) => {
              const regx = new RegExp(this.currentRules.db.expression);
              return regx.test(value);
            },
            message: this.currentRules.db.description,
            trigger: 'blur',
          });
          return {
            ...baseRule,
            dbPassword: [
              {
                validator: value => !/[\u4e00-\u9fa5]/.test(value),
                message: I18n.t('account.密码不支持中文'),
                trigger: 'blur',
              },
            ],
            rePassword: [
              {
                validator: value => this.formData.rePassword === this.formData.dbPassword,
                message: I18n.t('account.密码不一致'),
                trigger: 'blur',
              },
            ],
            dbPort: [
              {
                required: true,
                message: I18n.t('account.端口必填'),
                trigger: 'blur',
              },
            ],
            dbSystemAccountId: [
              {
                required: true,
                message: I18n.t('account.依赖系统账号必填'),
                trigger: 'blur',
              },
            ],
          };
        }
        // Linux系统账号不需要密码
        if (this.formData.type === AccountModel.TYPE_LINUX) {
          // linux 管理源配置账号规则
          baseRule.account.push({
            validator: (value) => {
              const regx = new RegExp(this.currentRules.linux.expression);
              return regx.test(value);
            },
            message: this.currentRules.linux.description,
            trigger: 'blur',
          });
          return baseRule;
        }

        // windows 系统管理员配置规则
        baseRule.account.push({
          validator: (value) => {
            const regx = new RegExp(this.currentRules.windows.expression);
            return regx.test(value);
          },
          message: this.currentRules.windows.description,
          trigger: 'blur',
        });
        return {
          ...baseRule,
          password: [
            {
              required: true,
              message: I18n.t('account.密码必填'),
              trigger: 'blur',
            },
            {
              validator: value => !/[\u4e00-\u9fa5]/.test(value),
              message: I18n.t('account.密码不支持中文'),
              trigger: 'blur',
            },
          ],
          rePassword: [
            {
              validator: value => value === this.formData.password,
              message: I18n.t('account.密码不一致'),
              trigger: 'blur',
            },
          ],
        };
      },
    },
    created() {
      this.fetchRules();

      this.categoryList = [
        {
          value: AccountModel.OS,
          name: I18n.t('account.系统账号'),
        },
        {
          value: AccountModel.DB,
          name: I18n.t('account.数据库账号'),
        },
      ];

      if (this.data.id) {
        this.isEdit = true;
        const {
          id,
          account,
          alias,
          category,
          dbPassword,
          dbPort,
          dbSystemAccountId,
          grantees,
          os,
          password,
          remark,
          type,
        } = this.data;
        // 确认密码字段，优先判断 password 字段优先级高，然后是 dbPassword
        let rePassword = dbPassword;
        if (password) {
          rePassword = password;
        }
        this.formData = {
          id,
          account,
          alias,
          category,
          dbPassword,
          dbPort,
          dbSystemAccountId,
          grantees,
          os,
          password,
          remark,
          type,
          rePassword,
        };
      }
    },
    methods: {
      /**
       * @desc 获取管理员配置的账号命名规则
       */
      fetchRules() {
        if (this.data.id) {
          this.isLoading = false;
          return Promise.resolve();
        }
        return QueryGlobalSettingService.fetchAllNameRule()
          .then((data) => {
            const { currentRules } = data;
            this.currentRules = Object.freeze(currentRules.reduce((result, item) => {
              result[item.osTypeKey] = item;
              return result;
            }, {}));
          })
          .catch(() => {
            this.isRulesLoadingError = false;
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      /**
       * @desc 提交新建账号
       */
      createAccount() {
        const params = { ...this.formData };
        delete params.rePassword;
        return AccountManageService.createAccount(params)
          .then(() => {
            this.messageSuccess(I18n.t('account.新建账号成功'));
            this.$emit('on-change');
          });
      },
      /**
       * @desc 提交编辑账号
       */
      updateAccount() {
        const params = { ...this.formData };
        delete params.rePassword;
        return AccountManageService.updateAccount(params)
          .then(() => {
            this.messageSuccess(I18n.t('account.编辑账号成功'));
            this.$emit('on-change');
          });
      },
      /**
       * @desc 账号分类切换
       * @param { Number } category
       *
       * 切换账号分类重置表单验证
       */
      handleCategoryChange(category) {
        this.formData = generatorDefault();
        this.formData.category = category;
        const defaultType = {
          [AccountModel.OS]: AccountModel.TYPE_LINUX,
          [AccountModel.DB]: AccountModel.TYPE_MYSQL,
        };
        this.formData.type = defaultType[category];
        this.$refs.operateAccountForm.clearError();
      },
      /**
       * @desc 表单字段更新
       * @param { String } key
       * @param { Any } value
       */
      handleFieldChange(key, value) {
        this.formData[key] = value;
      },
      /**
       * @desc 表单提交
       */
      submit() {
        if (this.isRulesLoadingError) {
          this.messageWarn(I18n.t('account.命名规则请求失败无法执行当前操作，请刷新页面'));
          return Promise.reject(Error('rule error'));
        }
        return this.$refs.operateAccountForm.validate()
          .then(() => {
            if (this.formData.id) {
              return this.updateAccount();
            }
            return this.createAccount();
          });
      },
    },
  };
</script>
<style lang="postcss">
  .operation-account {
    margin-bottom: -20px;

    .radio-button-group-wraper {
      position: relative;
      z-index: 1;
    }

    .account-type-radio {
      width: 120px;

      .bk-radio-button-text {
        width: 100%;
      }
    }
  }
</style>
