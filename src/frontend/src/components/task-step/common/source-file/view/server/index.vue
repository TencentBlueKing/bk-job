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
    class="server-file-edit-box">
    <table>
      <thead>
        <th>{{ $t('文件路径') }}</th>
        <th>{{ $t('服务器列表') }}</th>
        <th>{{ $t('Agent 状态') }}</th>
        <th>{{ $t('服务器账号') }}</th>
        <th>{{ $t('操作') }}</th>
      </thead>
      <tbody>
        <tr
          v-for="(row, index) in serverFileList"
          :key="index">
          <td>
            <edit-file-path
              :value="row.fileLocation"
              @on-change="value => handleFilePathEdit(value, index)" />
          </td>
          <td>
            <!-- 编辑主机变量 -->
            <div
              v-if="row.isVar"
              class="global-variable-render-box"
              :class="{ 'value-error': !row.host.variable }"
              style="margin-left: -10px;">
              <bk-select
                class="variable-edit-select"
                :clearable="false"
                searchable
                :value="row.host.variable"
                @change="value => handleVariableChange(value, index)">
                <bk-option
                  v-for="(option, variableIndex) in variable"
                  :id="option.name"
                  :key="variableIndex"
                  :name="option.name" />
              </bk-select>
              <icon
                v-if="!row.host.variable"
                v-bk-tooltips="$t('全局变量被删除，重新设置')"
                class="error-tips"
                type="info" />
            </div>
            <!-- 手动添加主机 -->
            <div
              v-else
              class="server-edit-btn"
              @click="handleHostEdit(index)"
              v-html="row.serverDesc" />
          </td>
          <td>
            <!-- 展示服务器列表主机信息 -->
            <render-server-agent
              v-if="!row.isVar"
              key="host"
              :host-node-info="row.host.hostNodeInfo"
              :separator="agentSeparator"
              :title="`${$t('服务器文件-服务器列表')}`" />
            <!-- 展示变量的主机信息 -->
            <render-server-agent
              v-else
              :key="row.serverDesc"
              :host-node-info="findVariableValue(row.serverDesc)"
              :separator="agentSeparator"
              :title="`${$t('全局变量')} - ${row.host.variable}`" />
          </td>
          <td>
            <account-select
              class="account-add-btn"
              type="system"
              :value="row.account"
              @change="value => handleAccountChange(value, index)" />
          </td>
          <td>
            <bk-button
              text
              @click="handlerRemove(index)">
              {{ $t('移除') }}
            </bk-button>
          </td>
        </tr>
      </tbody>
      <component
        :is="addCom"
        key="add"
        v-bind="$attrs"
        :account="accountList"
        :data="serverFileList"
        :variable="variable"
        @on-cancel="handleAddCancel"
        @on-change="handleAddSave" />
    </table>
    <!-- <lower-component level="custom" :custom="isShowChooseIp">
            <choose-ip
                v-model="isShowChooseIp"
                required
                :host-node-info="currentHost"
                @on-change="handleHostChange" />
        </lower-component> -->
    <ip-selector
      :show-dialog="isShowChooseIp"
      :value="currentHost"
      @change="handleHostChange"
      @close-dialog="handleCloseIpSelector" />
  </div>
</template>
<script>
  import { mapMutations } from 'vuex';

  import AccountManageService from '@service/account-manage';

  import TaskHostNodeModel from '@model/task-host-node';

  import AccountSelect from '@components/account-select';
  import ChooseIp from '@components/choose-ip';
  import RenderServerAgent from '@components/render-server-agent';

  import EditFilePath from '../../components/edit-file-path';

  import AddHostAndVariable from './host-and-variable';
  import AddOnlyHost from './only-host';

  export default {
    name: 'SourceFileServer',
    components: {
      ChooseIp,
      RenderServerAgent,
      AccountSelect,
      AddOnlyHost,
      AddHostAndVariable,
      EditFilePath,
    },
    inheritAttrs: false,
    props: {
      data: {
        type: Array,
        required: true,
      },
      mode: {
        type: String,
        default: '',
      },
      variable: {
        type: Array,
        default: () => [],
      },
    },
    data() {
      return {
        isLoading: true,
        serverFileList: [],
        currentHost: {},
        accountList: [],
        isShowChooseIp: false,
        currentIndex: 0,
      };
    },
    computed: {
      addCom() {
        if (this.isLoading) {
          return 'div';
        }
        if (this.mode === 'onlyHost') {
          return AddOnlyHost;
        }
        return AddHostAndVariable;
      },
      agentSeparator() {
        if (this.mode === 'onlyHost') {
          return '、';
        }
        return '\n';
      },
    },
    watch: {
      data: {
        handler(newData) {
          if (this.innerChange) {
            this.innerChange = false;
            return;
          }
          this.serverFileList = newData;
        },
        immediate: true,
      },
    },
    created() {
      this.fetchAccount();
      this.editNewSourceFile(false);
    },
    methods: {
      ...mapMutations('distroFile', [
        'editNewSourceFile',
      ]),
      /**
       * @desc 获取系统账号列表
       */
      fetchAccount() {
        AccountManageService.fetchAccountWhole({
          category: 1,
        }).then((data) => {
          this.accountList = data;
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
      /**
       * @desc 从全局变量列表中查找指定全局变量的值
       * @param {String} variableName 全局变量名
       */
      findVariableValue(variableName) {
        const curVariable = this.variable.find(item => item.name === variableName);
        if (!curVariable) {
          const {
            hostNodeInfo,
          } = new TaskHostNodeModel({});
          return hostNodeInfo;
        }
        return curVariable.defaultTargetValue.hostNodeInfo;
      },
      /**
       * @desc 服务器文件更新
       */
      triggerChange() {
        this.innerChange = true;
        this.$emit('on-change', [
          ...this.serverFileList,
        ]);
      },
      /**
       * @desc 编辑服务器文件的路径
       * @param {Array} fileLocation 服务器文件
       * @param {Number} index 编辑中的服务器文件的索引
       */
      handleFilePathEdit(fileLocation, index) {
        this.serverFileList[index].fileLocation = fileLocation;
        this.triggerChange();
      },
      /**
       * @desc 开始编辑服务器文件的主机——显示ip选择器弹框
       * @param {Number} index 编辑中的服务器文件的索引
       */
      handleHostEdit(index) {
        this.isShowChooseIp = true;
        this.currentIndex = index;
        this.currentHost = this.data[index].host.hostNodeInfo;
      },
      handleCloseIpSelector() {
        this.isShowChooseIp = false;
      },
      /**
       * @desc 更新编辑服务器文件的主机
       * @param {Object} hostNodeInfo 服务器主机信息
       */
      handleHostChange(hostNodeInfo) {
        this.serverFileList[this.currentIndex].host.hostNodeInfo = hostNodeInfo;
        this.triggerChange();
      },
      /**
       * @desc 更新编辑服务器文件的全局变量
       * @param {Array} variable 服务器主机信息
       * @param {Number} index 编辑中的服务器文件的索引
       */
      handleVariableChange(variable, index) {
        this.serverFileList[index].host.variable = variable;
        this.triggerChange();
      },
      /**
       * @desc 开始编辑服务器文件的服务器账号
       * @param {Number} index 编辑中的服务器文件的索引
       */
      handleEditAccount(index) {
        this.serverFileList[index].isEditAccount = true;
        this.editAccountIndex = index;
        this.triggerChange();
      },
      /**
       * @desc 更新编辑服务器文件的服务器账号
       * @param {Number} account 服务器账号id
       * @param {Number} index 编辑中的服务器文件的索引
       */
      handleAccountChange(account, index) {
        const serverFile = this.serverFileList[index];
        serverFile.account = account;
        this.triggerChange();
      },
      /**
       * @desc 删除指定的服务器文件
       * @param {Number} index 服务器文件的索引
       */
      handlerRemove(index) {
        this.serverFileList.splice(index, 1);
        this.triggerChange();
      },
      /**
       * @desc 添加一条服务器文件
       * @param {Object} serverFile 服务器文件
       */
      handleAddSave(serverFile) {
        this.serverFileList.push(serverFile);
        this.triggerChange();
      },
      /**
       * @desc 取消添加一条服务器文件
       */
      handleAddCancel() {
        if (this.serverFileList.length < 1) {
          this.$emit('on-close');
        }
      },
    },
  };
</script>
<style lang='postcss'>
  .server-file-edit-box {
    .global-variable-render-box {
      position: relative;
      border: 1px solid transparent;
      border-radius: 2px;

      &.value-error {
        border-color: #ea3636;
      }

      .error-tips {
        position: absolute;
        top: 0;
        right: 0;
        z-index: 1999;
        display: flex;
        height: 30px;
        padding: 0 6px;
        font-size: 16px;
        color: #ea3636;
        cursor: pointer;
        align-items: center;
      }
    }

    .variable-edit-select,
    .account-add-btn {
      &:hover,
      &.is-focus {
        .bk-select-angle {
          display: block;
        }
      }

      .bk-select-angle {
        display: none;
        font-size: 20px;
      }
    }

    .account-add-btn,
    .server-edit-btn,
    .server-add-only-host-btn {
      margin-left: -10px;
    }

    .server-add-btn,
    .server-edit-btn,
    .server-add-host {
      cursor: pointer;
      border-radius: 2px;

      &:hover {
        background: #f0f1f5;
      }
    }

    .server-add-btn,
    .server-add-host {
      height: 30px;
      line-height: 30px;
    }

    .server-edit-btn {
      min-height: 30px;
      padding: 5px 10px;
      line-height: 20px;
    }

    .server-add-btn {
      display: flex;
      height: 30px;
      margin-left: -10px;
      font-size: 12px;
      cursor: pointer;
      background: #f7f8fa;
      border-radius: 2px;
      align-items: center;

      .server-type-select {
        flex: 0 0 auto;
      }

      .line {
        height: 16px;
        background: #dcdee5;
        flex: 0 0 1px;
      }

      .server-add-variable {
        flex: 1;
      }

      .server-add-host {
        display: flex;
        height: 30px;
        font-size: 12px;
        border-radius: 2px;
        flex: 1;
        align-items: center;
        justify-content: center;
      }
    }

    .server-add-only-host-btn {
      display: flex;
      height: 30px;
      padding: 0 10px;
      font-size: 12px;
      cursor: pointer;
      background: #f7f8fa;
      border-radius: 2px;
      align-items: center;
      justify-content: center;

      &:hover {
        color: #979ba5;
        background: #f0f1f5;
      }

      .add-flag {
        margin-right: 6px;
        margin-bottom: 2px;
      }
    }

    .create-server-file {
      tr {
        td {
          border-top: 1px solid #dcdee5;
        }
      }
    }

    .bk-select {
      height: 30px;
      line-height: 30px;
      border: none;

      &:hover {
        background: #f0f1f5;
      }

      .bk-select-name {
        height: 30px;
        padding-right: 16px;
        padding-left: 10px;
        line-height: 30px;
      }
    }
  }
</style>
