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
    class="task-step-execute-target"
    :class="{
      'only-host': mode === 'onlyHost',
    }">
    <jb-form-item
      ref="targetServerRef"
      :label="$t('目标服务器')"
      :property="property"
      required
      :rules="rules">
      <div
        ref="actionBox"
        style="display: flex;">
        <template v-if="mode === 'onlyHost'">
          <!-- 快速执行场景只能操作主机列表 -->
          <bk-button
            class="mr10"
            @click="handleShowChooseIp">
            <icon type="plus" />
            {{ $t('添加服务器') }}
          </bk-button>
        </template>
        <template v-else>
          <!-- 作业步骤场景可以切换主机列表和主机变量 -->
          <compose-form-item>
            <bk-select
              :clearable="false"
              :style="targetSelectorStyle"
              :value="targetType"
              @change="handleTargetTypeChange">
              <bk-option
                id="variable"
                :name="$t('全局变量')" />
              <bk-option
                id="executeObjectsInfo"
                :name="$t('手动添加')" />
            </bk-select>
            <template v-if="isGolbalVariableType">
              <bk-select
                class="server-global-variable-select"
                :clearable="false"
                :placeholder="$t('请选择主机列表变量')"
                :value="localVariable"
                @change="handleVariableChange">
                <bk-option
                  v-for="(item, index) in variable"
                  :id="item.name"
                  :key="index"
                  :name="item.name" />
              </bk-select>
            </template>
            <template v-else>
              <bk-button
                class="w120 mr10"
                @click="handleShowChooseIp">
                <icon type="plus" />
                {{ $t('添加服务器') }}
              </bk-button>
            </template>
          </compose-form-item>
        </template>
        <template v-if="isShowServerAction">
          <dropdown-menu>
            <bk-button
              class="mr10"
              :loading="isCopyLoading"
              type="primary">
              <span>{{ $t('复制 IP') }}</span>
              <icon
                class="action-flag"
                type="down-small" />
            </bk-button>
            <template slot="menu">
              <dropdown-menu-group>
                <span>{{ $t('所有 IP') }}</span>
                <template slot="action">
                  <dropdown-menu-item @click="() => handleCopyIPv4()">
                    IPv4
                  </dropdown-menu-item>
                  <dropdown-menu-item @click="() => handleCopyIPv4(true)">
                    {{ $t('管控区域 ID:IPv4') }}
                  </dropdown-menu-item>
                  <dropdown-menu-item @click="() => handleCopyIPv6()">
                    IPv6
                  </dropdown-menu-item>
                  <dropdown-menu-item @click="() => handleCopyIPv6(true)">
                    {{ $t('管控区域 ID:IPv6') }}
                  </dropdown-menu-item>
                </template>
              </dropdown-menu-group>
              <dropdown-menu-group>
                <span>{{ $t('异常 IP') }}</span>
                <template slot="action">
                  <dropdown-menu-item @click="() => handleCopyAbnormalIPv4()">
                    IPv4
                  </dropdown-menu-item>
                  <dropdown-menu-item @click="() => handleCopyAbnormalIPv4(true)">
                    {{ $t('管控区域 ID:IPv4') }}
                  </dropdown-menu-item>
                  <dropdown-menu-item @click="() => handleCopyAbnormalIPv6()">
                    IPv6
                  </dropdown-menu-item>
                  <dropdown-menu-item @click="() => handleCopyAbnormalIPv6(true)">
                    {{ $t('管控区域 ID:IPv6') }}
                  </dropdown-menu-item>
                </template>
              </dropdown-menu-group>
            </template>
          </dropdown-menu>
          <bk-button
            class="mr10"
            @click="handleClearAll">
            <span>{{ $t('清空') }}</span>
          </bk-button>
          <bk-button
            type="primary"
            @click="handleRefreshHost">
            {{ $t('刷新状态') }}
          </bk-button>
        </template>

        <bk-input
          v-if="isShowHostSearchInput"
          class="ip-search"
          :placeholder="$t('筛选主机')"
          right-icon="bk-icon icon-search"
          :value="searchText"
          @change="handleHostSearch" />
      </div>
      <ip-selector
        ref="ipSelector"
        :config="ipSelectorConfig"
        :show-dialog="isShowChooseIp"
        show-view
        :value="localExecuteObjectsInfo"
        :view-search-key="searchText"
        @change="handleExecuteObjectsInfoChange"
        @close-dialog="handleCloseIpSelector" />
    </jb-form-item>
  </div>
</template>
<script>
  import _ from 'lodash';

  import ExecuteTargetModel from '@model/execute-target';

  import { execCopy } from '@utils/assist';

  import ComposeFormItem from '@components/compose-form-item';

  import I18n from '@/i18n';

  import DropdownMenu from './components/dropdown-menu';
  import DropdownMenuGroup from './components/dropdown-menu/group';
  import DropdownMenuItem from './components/dropdown-menu/item';

  export default {
    components: {
      ComposeFormItem,
      DropdownMenu,
      DropdownMenuGroup,
      DropdownMenuItem,
    },
    inheritAttrs: false,
    props: {
      executeTarget: {
        type: Object,
        required: true,
      },
      windowsInterpreter: {
        type: String,
      },
      variable: {
        type: Array,
        default: () => [],
      },
      property: {
        type: String,
      },
      mode: {
        type: String,
        default: '', // onlyHost: 快速执行只可以选择主机列表
      },
      // 组件被使用的场景，快速执行｜作业模板
      from: {
        type: String,
        required: true,
      },
    },
    data() {
      return {
        isShowChooseIp: false,
        isCopyLoading: false,
        searchText: '',
        isSearchMode: false,
        searchData: [],
        targetType: 'variable', // variable：主机变量；executeObjectsInfo：手动添加
        localVariable: '',
        localExecuteObjectsInfo: {},
      };
    },
    computed: {
      /**
       * @desc 执行目标是否是全局变量
       * @returns {Boolean}
       */
      isGolbalVariableType() {
        return this.targetType === 'variable';
      },
      /**
       * @desc 是否显示主机结果面板
       * @returns {Boolean}
       */
      isShowServerPanel() {
        if (this.isGolbalVariableType) {
          return false;
        }
        return !ExecuteTargetModel.isExecuteObjectsInfoEmpty(this.localExecuteObjectsInfo);
      },
      /**
       * @desc 是否显示主机结果快捷操作
       * @returns {Boolean}
       */
      isShowServerAction() {
        if (this.isGolbalVariableType) {
          return false;
        }
        return !ExecuteTargetModel.isExecuteObjectsInfoEmpty(this.localExecuteObjectsInfo);
      },
      /**
       * @desc 清除异常主机是否可用
       * @returns {Boolean}
       */
      isClearFailDisabled() {
        return this.localExecuteObjectsInfo.hostList.length < 1;
      },
      /**
       * @desc 选择的主机才显示主机搜索框
       * @returns {Boolean}
       */
      isShowHostSearchInput() {
        if (this.isGolbalVariableType) {
          return false;
        }
        return this.localExecuteObjectsInfo.hostList.length > 0;
      },
      /**
       * @desc 切换执行目标选择的展示样式
       * @returns {Object}
       */
      targetSelectorStyle() {
        return {
          width: this.$i18n.locale === 'en-US' ? '156px' : '120px',
        };
      },
    },
    watch: {
      executeTarget: {
        handler() {
          const {
            executeObjectsInfo,
            variable,
          } = this.executeTarget;

          this.localExecuteObjectsInfo = executeObjectsInfo;
          this.localVariable = variable;
          if (this.mode !== 'onlyHost') {
            // 编辑态，执行目标为服务器列表
            if (!ExecuteTargetModel.isExecuteObjectsInfoEmpty(this.localExecuteObjectsInfo)) {
              this.targetType = 'executeObjectsInfo';
            }
          } else {
            this.targetType = 'executeObjectsInfo';
          }
        },
        immediate: true,
      },
    },
    created() {
      this.ipSelectorConfig = {};
      if (this.from === 'execute' && window.PROJECT_CONFIG.SCOPE_TYPE !== 'biz_set') {
        this.ipSelectorConfig = {
          panelList: [
            'staticTopo',
            'dynamicTopo',
            'dynamicGroup',
            'manualInput',
            'containerStaticTopo',
            'containerManualInput',
          ],
        };
      }
      // 执行目标是主机变量
      if (this.isGolbalVariableType) {
        if (this.localVariable) {
          // 编辑态
          // 如果被引用的主机变量被删除，则将执行目标的值重置为空
          // 主机变量被删除
          if (!this.variable.find(_ => _.name === this.localVariable)) {
            setTimeout(() => {
              this.handleVariableChange('');
            });
          }
        } else {
          // 主机变量为空，默认选中第一个
          if (this.variable.length > 0) {
            setTimeout(() => {
              this.handleVariableChange(this.variable[0].name);
            });
          }
        }
      }

      this.rules = [
        {
          validator: () => {
            if (this.isGolbalVariableType) {
              return Boolean(this.localVariable);
            }
            return !ExecuteTargetModel.isExecuteObjectsInfoEmpty(this.localExecuteObjectsInfo);
          },
          message: I18n.t('目标服务器必填'),
          trigger: 'blur',
        },
        {
          validator: () => {
            if (!this.windowsInterpreter) {
              return true;
            }
            return new Promise((resolve) => {
              this.$refs.ipSelector.getHostList()
                .then((hostList) => {
                  const nonWindowsHostList = _.filter(hostList, item => !/windows/i.test(item.os_type));
                  if (nonWindowsHostList.length < 1) {
                    return resolve(true);
                  }
                  const infoboxHandle = this.$bkInfo({
                    title: I18n.t('自定义windows解释器路径'),
                    maskClose: false,
                    escClose: false,
                    draggable: false,
                    showFooter: false,
                    closeIcon: false,
                    width: 450,
                    subHeader: (
                      <div>
                        <div style="font-size: 14px; line-height: 22px; color: #63656E; text-align: center">
                          {I18n.t('执行目标包含 Linux 服务器，但自定义解释器仅对 Windows 有效，请知悉。')}
                        </div>
                        <div style="padding: 24px 0 21px; text-align: center">
                          <bk-button
                            onClick={() => {
                              resolve(true);
                              infoboxHandle.close();
                            }}
                            style="width: 96px"
                            theme="primary">
                            { I18n.t('好的') }
                          </bk-button>
                        </div>
                      </div>
                    ),
                    closeFn: () => {
                      resolve(true);
                      infoboxHandle.close();
                    },
                  });
                });
            });
          },
          message: I18n.t('目标服务器必填'),
          trigger: 'blur',
        },
      ];
    },
    methods: {
      /**
       * @desc 执行目标值更新
       */
      triggerChange() {
        const executeTarget = new ExecuteTargetModel({});
        if (this.isGolbalVariableType) {
          executeTarget.variable = this.localVariable;
        } else {
          executeTarget.executeObjectsInfo = this.localExecuteObjectsInfo;
        }
        if (!executeTarget.isEmpty) {
          this.$refs.targetServerRef.clearValidator();
        }
        this.$emit('on-change', Object.freeze(executeTarget));
      },
      /**
       * @desc 执行目标类型改变
       */
      handleTargetTypeChange(value) {
        this.targetType = value;
        this.triggerChange();
      },
      /**
       * @desc 弹出ip选择器弹层
       */
      handleShowChooseIp() {
        this.isShowChooseIp = true;
        this.searchText = '';
      },
      /**
       * @desc 选择全局变量
       * @param {String} value 全局变量名
       */
      handleVariableChange(value) {
        this.localVariable = value;
        this.triggerChange();
      },
      /**
       * @desc 主机值更新
       * @param {Object} executeObjectsInfo 主机信息
       */
      handleExecuteObjectsInfoChange(executeObjectsInfo) {
        this.localExecuteObjectsInfo = Object.freeze(executeObjectsInfo);
        this.triggerChange();
        setTimeout(() => {
          this.$refs.actionBox.scrollIntoView();
        }, 500);
      },
      handleCloseIpSelector() {
        this.isShowChooseIp = false;
      },
      handleCopyIPv4(withNet = false) {
        this.isCopyLoading = true;
        this.$refs.ipSelector.getIpv4HostList().then((hostList) => {
          if (hostList.length < 1) {
            this.messageWarn(I18n.t('没有可复制的 IPv4'));
            return;
          }

          const ipStr = hostList.map(item => (withNet ? `${item.cloud_area.id}:${item.ip}` : item.ip)).join('\n');

          execCopy(ipStr, `${I18n.t('复制成功')}（${hostList.length}${I18n.t('个IP')}）`);
        })
          .finally(() => {
            this.isCopyLoading = false;
          });
      },
      handleCopyIPv6(withNet = false) {
        this.isCopyLoading = true;
        this.$refs.ipSelector.getIpv6HostList().then((hostList) => {
          if (hostList.length < 1) {
            this.messageWarn(I18n.t('没有可复制的 IPv6'));
            return;
          }

          const ipv6Str = hostList.map(item => (withNet ? `${item.cloud_area.id}:${item.ipv6}` : item.ipv6)).join('\n');

          execCopy(ipv6Str, `${I18n.t('复制成功')}（${hostList.length}${I18n.t('个IP')}）`);
        })
          .finally(() => {
            this.isCopyLoading = false;
          });
      },
      handleCopyAbnormalIPv4(withNet = false) {
        this.isCopyLoading = true;
        this.$refs.ipSelector.getAbnormalIpv4HostList().then((hostList) => {
          if (hostList.length < 1) {
            this.messageWarn(I18n.t('没有可复制的异常 IPv4'));
            return;
          }

          const ipStr = hostList.map(item => (withNet ? `${item.cloud_area.id}:${item.ip}` : item.ip)).join('\n');

          execCopy(ipStr, `${I18n.t('复制成功')}（${hostList.length}${I18n.t('个IP')}）`);
        })
          .finally(() => {
            this.isCopyLoading = false;
          });
      },
      handleCopyAbnormalIPv6(withNet = false) {
        this.isCopyLoading = true;
        this.$refs.ipSelector.getAbnormalHostIpv6List().then((hostList) => {
          if (hostList.length < 1) {
            this.messageWarn(I18n.t('没有可复制的异常 IPv6'));
            return;
          }

          const ipv6Str = hostList.map(item => (withNet ? `${item.cloud_area.id}:${item.ipv6}` : item.ipv6)).join('\n');

          execCopy(ipv6Str, `${I18n.t('复制成功')}（${hostList.length}${I18n.t('个IP')}）`);
        })
          .finally(() => {
            this.isCopyLoading = false;
          });
      },
      /**
       * @desc 复制所有主机数据
       */
      handleClearAll() {
        this.$refs.ipSelector.resetValue();
        this.messageSuccess(I18n.t('清空成功'));
      },
      /**
       * @desc 刷新所有主机的状态信息
       */
      handleRefreshHost() {
        this.$refs.ipSelector.refresh();
      },
      /**
       * @desc 筛选主机
       * @param {String} search 筛选值
       */
      handleHostSearch(search) {
        this.searchText = _.trim(search);
      },
    },
  };
</script>
<style lang='postcss'>
  html[lang="en-US"] {
    .compose-form-item {
      .server-global-variable-select {
        width: 341px;
      }
    }

    .ip-search {
      width: 162px;
    }

    .only-host {
      .ip-search {
        width: 314px;
      }
    }
  }

  .task-step-execute-target {
    &.only-host {
      .ip-search {
        width: 500px;
      }
    }

    .bk-button {
      font-size: 14px !important;
    }

    .action-flag {
      font-size: 18px;
    }

    .view-server-panel {
      margin-top: 14px;
    }

    .ip-search {
      flex: 0 0 auto;
      width: 245px;
      margin-left: auto;
    }

    .compose-form-item {
      .server-global-variable-select {
        width: 376px;
      }
    }
  }

  .execute-target-host-clear {
    user-select: none;

    .disabled {
      a {
        color: #c4c6cc !important;
        cursor: not-allowed;
        background-color: #fafafa !important;
      }
    }
  }
</style>
