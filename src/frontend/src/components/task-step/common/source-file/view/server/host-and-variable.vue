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
  <tbody
    :key="reset"
    class="create-server-file">
    <tr v-if="hasSaved">
      <td colspan="4">
        <bk-button
          text
          @click="handleAddNew">
          <icon type="plus" />
          {{ $t('添加一行') }}
        </bk-button>
      </td>
      <td />
    </tr>
    <tr v-else>
      <td>
        <edit-file-path
          mode="input"
          :value="serverFile.fileLocation"
          @on-change="handleFileChange" />
      </td>
      <template v-if="serverFile.isHostEmpty">
        <td colspan="2">
          <div class="server-add-btn">
            <bk-select
              ref="serverTypeSelect"
              class="server-type-select"
              :clearable="false"
              ext-popover-cls="server-file-popover-class"
              :popover-width="$i18n.locale === 'en-US' ? 130 : 85"
              style="width: 78px;"
              :value="sourceFileType"
              @change="handleSourceFileTypeChange">
              <bk-option
                id="globalVar"
                :name="$t('全局变量')" />
              <bk-option
                id="manualAddition"
                :name="$t('手动添加')" />
            </bk-select>
            <div class="line" />
            <template v-if="sourceFileType === 'globalVar'">
              <bk-select
                class="server-add-variable"
                :clearable="false"
                :placeholder="$t('请选择主机列表变量')"
                searchable
                :value="serverFile.host.variable"
                @change="handleVariableChange">
                <bk-option
                  v-for="(option, index) in variable"
                  :id="option.name"
                  :key="index"
                  :name="option.name" />
              </bk-select>
            </template>
            <template v-else>
              <div
                class="server-add-host"
                @click="handleShowChooseIp">
                <icon
                  class="add-flag"
                  type="plus" />
                {{ $t('添加服务器') }}
              </div>
            </template>
          </div>
        </td>
      </template>
      <template v-else>
        <td>
          <div
            class="file-edit-server"
            @click="handleShowChooseIp">
            <p v-html="serverFile.serverDesc" />
          </div>
        </td>
        <td>--</td>
      </template>
      <td>
        <account-select
          class="account-add-btn"
          type="system"
          :value="serverFile.account"
          @change="handleAccountChange" />
      </td>
      <td>
        <bk-button
          :disabled="serverFile.isDisableSave"
          text
          @click="handlerSave">
          {{ $t('保存') }}
        </bk-button>
        <bk-button
          text
          @click="handlerCancel">
          {{ $t('取消') }}
        </bk-button>
      </td>
    </tr>
    <ip-selector
      ref="ipSelector"
      :show-dialog="isShowChooseIp"
      @change="handleHostChange"
      @close-dialog="handleCloseIpSelector" />
    <!-- <choose-ip
            ref="chooseIp"
            v-model="isShowChooseIp"
            @on-change="handleHostChange" /> -->
  </tbody>
</template>
<script>
  import _ from 'lodash';
  import { mapMutations } from 'vuex';

  import TaskHostNodeModel from '@model/task-host-node';

  import { findParent } from '@utils/vdom';

  // import ChooseIp from '@components/choose-ip';
  import AccountSelect from '@components/account-select';

  import SourceFileVO from '@domain/variable-object/source-file';

  import EditFilePath from '../../components/edit-file-path';

  const generatorDefault = () => new SourceFileVO({
    fileLocation: [],
    fileType: SourceFileVO.typeServer,
    account: '',
  });

  export default {
    name: '',
    components: {
      // ChooseIp,
      AccountSelect,
      EditFilePath,
    },
    props: {
      // 服务器文件列表
      data: {
        type: Array,
        required: true,
      },
      account: {
        type: Array,
        required: true,
      },
      variable: {
        type: Array,
        required: true,
      },
    },
    data() {
      return {
        isShowChooseIp: false,
        // 服务器文件列表为空时，默认显示
        hasSaved: this.data.length > 0,
        sourceFileType: 'globalVar',
        serverFile: new SourceFileVO(generatorDefault()),
        reset: 0,
      };
    },
    created() {
      if (this.variable.length > 0) {
        this.handleVariableChange(this.variable[0].name);
        // 设置默认数据，需要取消 window.changeFlag 的状态
        window.changeFlag = false;
        // 设置默认数据，需要取消服务器文件的编辑状态
        this.editNewSourceFile(false);
      }
    },
    methods: {
      ...mapMutations('distroFile', [
        'editNewSourceFile',
      ]),
      /**
       * @desc 文件路径更新
       * @param {Array} fileLocation 文件路径
       */
      handleFileChange(fileLocation) {
        this.serverFile.fileLocation = fileLocation;
        window.changeFlag = true;
        this.editNewSourceFile(true);
      },
      /**
       * @desc 服务器类型更新
       * @param {String} type 服务器类型
       */
      handleSourceFileTypeChange(type) {
        this.sourceFileType = type;
        this.serverFile.host = new TaskHostNodeModel({});
        const formItem = findParent(this, 'JbFormItem');
        if (formItem) {
          setTimeout(() => {
            formItem.clearValidator();
          });
        }
      },
      /**
       * @desc 服务器类型为全局变量时更新选择的全局变量
       * @param {String} variable 全局变量名
       */
      handleVariableChange(variable) {
        if (!variable) {
          return;
        }
        this.serverFile.host.variable = variable;
        window.changeFlag = true;
        this.editNewSourceFile(true);
        const formItem = findParent(this, 'JbFormItem');
        if (formItem) {
          setTimeout(() => {
            formItem.clearValidator();
          });
        }
      },
      /**
       * @desc 服务器类型为主机时更新显示ip选择器弹层
       */
      handleShowChooseIp() {
        this.isShowChooseIp = true;
      },
      handleCloseIpSelector() {
        this.isShowChooseIp = false;
      },
      /**
       * @desc 服务器类型为主机时主机值更新
       * @param {Object} hostNodeInfo 主机值
       */
      handleHostChange(hostNodeInfo) {
        window.changeFlag = true;
        this.serverFile.host.hostNodeInfo = hostNodeInfo;
        this.editNewSourceFile(true);
      },
      /**
       * @desc 服务器账号更新
       * @param {Number} accountId 主机值
       */
      handleAccountChange(accountId) {
        if (accountId === '') {
          return;
        }
        const { id } = _.find(this.account, item => item.id === accountId);
        this.serverFile.account = id;
        const formItem = findParent(this, 'JbFormItem');
        if (formItem) {
          setTimeout(() => {
            formItem.clearValidator();
          });
        }
      },
      /**
       * @desc 添加一个服务器文件
       */
      handleAddNew() {
        this.hasSaved = false;
      },
      /**
       * @desc 保存添加的服务器文件
       */
      handlerSave() {
        this.$emit('on-change', this.serverFile);
        this.handlerCancel();
      },
      /**
       * @desc 取消添加的服务器文件
       */
      handlerCancel() {
        this.$emit('on-cancel');
        this.serverFile = generatorDefault();
        this.sourceFileType = 'globalVar';
        this.$refs.ipSelector.resetValue();
        this.reset += 1;
        this.hasSaved = true;
        setTimeout(() => {
          this.editNewSourceFile(false);
        });
      },
    },
  };
</script>
