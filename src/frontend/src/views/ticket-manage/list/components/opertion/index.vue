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
        <bk-input v-model="formData.name" />
      </jb-form-item>
      <jb-form-item :label="$t('ticket.类型.label')">
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
  import TicketService from '@service/ticket-manage';

  import SecretkeyAppid from './components/app-id-secret-key';
  import Password from './components/password';
  import Secretkey from './components/secret-key';
  import UsernamePassword from './components/username-password';

  import I18n from '@/i18n';

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
    },
    methods: {
      /**
       * @desc 新增、编辑确认
       *
       * 校验通过后,根据文件源ID是否存在提示新建、编辑提示语
       */
      submit() {
        return Promise.all([
          this.$refs.handler.getData(),
          this.$refs.ticketForm.validate(),
        ]).then(([data, validate]) => TicketService.update({
          ...this.formData,
          ...data,
        }))
          .then(() => {
            if (!this.data.id) {
              this.messageSuccess(I18n.t('ticket.创建成功'));
            } else {
              this.messageSuccess(I18n.t('ticket.更新成功'));
            }
            this.$emit('on-change');
          });
      },
    },
  };
</script>
