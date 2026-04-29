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
  <div class="create-ticket-page">
    <jb-form
      ref="ticketForm"
      form-type="vertical"
      :model="formData"
      :rules="rules">
      <jb-form-item
        :label="$t('ticket.名称')"
        property="name"
        required>
        <bk-input v-model.trim="formData.name" />
      </jb-form-item>
      <jb-form-item :label="$t('ticket.类型_label')">
        <div class="ticket-type-wraper">
          <bk-select
            v-model="formData.type"
            :clearable="false">
            <bk-option
              id="PASSWORD"
              :name="$t('ticket.单一密码')">
              <div class="ticket-name-option">
                <span>{{ $t('ticket.单一密码') }}</span>
              </div>
            </bk-option>
            <bk-option
              id="USERNAME_PASSWORD"
              :name="$t('ticket.用户名+密码')">
              <div class="ticket-name-option">
                <span>{{ $t('ticket.用户名+密码') }}</span>
              </div>
            </bk-option>
            <bk-option
              id="SECRET_KEY"
              :name="$t('ticket.单一SecretKey')">
              <div class="ticket-name-option">
                <span>{{ $t('ticket.单一SecretKey') }}</span>
              </div>
            </bk-option>
            <bk-option
              id="APP_ID_SECRET_KEY"
              :name="$t('ticket.AppID+SecretKey')">
              <div class="ticket-name-option">
                <span>{{ $t('ticket.AppID+SecretKey') }}</span>
              </div>
            </bk-option>
          </bk-select>
        </div>
      </jb-form-item>
    </jb-form>
    <component
      :is="formItemCom"
      ref="handler"
      :data="formData"
      :secret-key="formData.secretKey"
      :type="type" />
  </div>
</template>

<script>
  import TicketManageService from '@service/ticket-manage';
  import webGlobal from '@service/web-global';

  import { encrypt } from '@utils/assist';

  import I18n from '@/i18n';

  import SecretkeyAppid from './components/app-id-secret-key';
  import Password from './components/password';
  import Secretkey from './components/secret-key';
  import UsernamePassword from './components/username-password';

  const getDefaultData = () => ({
    name: '',
    type: 'PASSWORD',
    value1: '',
    value2: '',
    value3: '',
    description: '',
  });
  export default {
    name: 'Opertion',
    props: {
      data: {
        type: Object,
        default: () => ({}),
      },
    },
    data() {
      return {
        formData: getDefaultData(),
        type: '',
        isAccountEncryptionLoadingError: false,
        accountEncryption: null,
      };
    },
    computed: {
      formItemCom() {
        const formItemMap = {
          PASSWORD: Password,
          USERNAME_PASSWORD: UsernamePassword,
          SECRET_KEY: Secretkey,
          APP_ID_SECRET_KEY: SecretkeyAppid,
        };
        return formItemMap[this.formData.type];
      },
    },
    watch: {
    },
    created() {
      if (this.data.id) {
        this.formData = { ...this.data };
        this.type = this.formData.type;
      }
      this.ticketTypeList = [
        { id: 'PASSWORD', name: I18n.t('ticket.单一密码') },
        { id: 'USERNAME_PASSWORD', name: I18n.t('ticket.用户名+密码') },
        { id: 'SECRET_KEY', name: I18n.t('ticket.单一SecretKey') },
        { id: 'APP_ID_SECRET_KEY', name: I18n.t('ticket.AppID+SecretKey') },
      ];
      this.rules = {
        name: [
          {
            required: true,
            message: I18n.t('ticket.凭证名称必填'),
            trigger: 'blur',
          },
        ],
      };
      this.fetchAccountEncryption();
    },
    methods: {
      fetchAccountEncryption() {
        this.isAccountEncryptionLoadingError = false;
        webGlobal.fetchAccountEncryption()
          .then((data) => {
            this.accountEncryption = data;
          })
          .catch(() => {
            this.isAccountEncryptionLoadingError = true;
          });
      },
      /**
       * @desc 新增、编辑确认
       *
       * 校验通过后,根据文件源ID是否存在提示新建、编辑提示语
       */
      submit() {
        const requestHandler = this.data.id ? TicketManageService.update : TicketManageService.create;

        if (this.isAccountEncryptionLoadingError) {
          this.messageWarn(I18n.t('ticket.凭证加密配置请求失败无法执行当前操作，请刷新页面'));
          return Promise.reject(Error('ticket encryption error'));
        }

        return Promise.all([
          this.$refs.handler.getData(),
          this.$refs.ticketForm.validate(),
          TicketManageService.check({
            id: this.data.id || 0,
            name: this.formData.name,
          }),
        ]).then(([data,, checkData]) => {
          if (checkData?.data === false) {
            return Promise.reject({
              code: 'BK_JOB_TICKET_NAME_EXIST',
            });
          }

          const key = ['USERNAME_PASSWORD', 'APP_ID_SECRET_KEY'].includes(this.formData.type) ? 'value2' : 'value1';
          const encryptValue = encrypt(this.accountEncryption.algorithm, this.accountEncryption.pemPublicKey, data[key]);
          data.algorithm = this.accountEncryption.algorithm;
          data[key] = encryptValue;

          return requestHandler({
            ...this.formData,
            ...data,
          });
        })
          .then(() => {
            if (!this.data.id) {
              this.messageSuccess(I18n.t('ticket.创建成功'));
            } else {
              this.messageSuccess(I18n.t('ticket.更新成功'));
            }
            this.$emit('on-change');
          })
          .catch((error) => {
            if (error.code === 'BK_JOB_TICKET_NAME_EXIST') {
              this.messageError(I18n.t('ticket.凭证名称已存在'));
              return Promise.reject(Error('ticket name exist'));
            }
          });
      },
    },
  };
</script>
