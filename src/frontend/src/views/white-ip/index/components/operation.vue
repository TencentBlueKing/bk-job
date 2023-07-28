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
    <jb-form
      ref="whiteIpForm"
      form-type="vertical"
      :model="formData"
      :rules="rules">
      <jb-form-item :label="$t('whiteIP.目标业务.label')">
        <div class="app-wraper">
          <bk-select
            v-model="scopeValue"
            class="app-select"
            :clearable="false"
            :disabled="formData.allScope"
            multiple
            searchable>
            <bk-option
              v-for="option in appList"
              :id="option.localKey"
              :key="option.localKey"
              :name="option.name" />
          </bk-select>
          <bk-checkbox
            class="whole-business"
            :value="formData.allScope"
            @change="handleAllAPP">
            {{ $t('whiteIP.全业务') }}
          </bk-checkbox>
        </div>
      </jb-form-item>
      <jb-form-item
        :label="$t('whiteIP.IP')"
        property="hostList"
        required>
        <bk-button @click="handleShowIpSelector">
          {{ $t('whiteIP.添加服务器') }}
        </bk-button>
        <ip-selector
          v-bind="ipSelectorConfig"
          model="dialog"
          :show-dialog="isShowIpSelector"
          show-view
          :value="ipSelectorValue"
          @change="handleIpSelectorChange"
          @close-dialog="handleColseIpSelector" />
      </jb-form-item>
      <jb-form-item
        :label="$t('whiteIP.备注.label')"
        property="remark"
        required>
        <bk-input
          v-model="formData.remark"
          :maxlength="100"
          type="textarea" />
      </jb-form-item>
      <jb-form-item
        :label="$t('whiteIP.生效范围.label')"
        property="actionScopeIdList"
        required
        style="margin-bottom: 0;">
        <bk-checkbox-group
          v-model="formData.actionScopeIdList"
          @change="handleRangeChange">
          <bk-checkbox
            v-for="(item, index) in actionScope"
            :key="item.id"
            :class="{ 'scope-checkbox': index !== actionScope.length - 1 }"
            :value="item.id">
            {{ item.name }}
          </bk-checkbox>
        </bk-checkbox-group>
      </jb-form-item>
    </jb-form>
  </div>
</template>
<script>
  import AppManageService from '@service/app-manage';
  import HostAllManageService from '@service/host-all-manage';
  import WhiteIpService from '@service/white-ip';

  import I18n from '@/i18n';

  const getDefaultData = () => ({
    id: 0,
    // 业务ID
    scopeList: [],
    allScope: false,
    hostList: [],
    // 备注
    remark: '',
    // 生效范围
    actionScopeIdList: [],
  });

  export default {
    name: '',
    props: {
      data: {
        type: Object,
        default: () => ({}),
      },
    },
    data() {
      return {
        isLoading: true,
        formData: getDefaultData(),
        scopeValue: [],
        ipSelectorValue: {},
        appList: [],
        actionScope: [],
        isShowIpSelector: false,
      };
    },
    watch: {
      data: {
        handler(data) {
          if (!data.id) {
            return;
          }
          const {
            id,
            scopeList,
            allScope,
            hostList,
            remark,
            actionScopeList,
          } = data;
          this.formData = {
            ...this.formData,
            id,
            scopeList,
            allScope,
            hostList,
            remark,
            actionScopeIdList: actionScopeList.map(item => item.id),
          };
          this.ipSelectorValue = {
            hostList,
          };
          this.scopeValue = this.formData.scopeList.map(item => `#${item.scopeType}#${item.scopeId}`);
        },
        immediate: true,
      },
    },
    created() {
      Promise.all([
        this.fetchAppList(),
        this.fetchActionScope(),
      ]).finally(() => {
        this.isLoading = false;
      });
      this.ipSelectorConfig = {
        config: {
          panelList: ['staticTopo', 'manualInput'],
        },
        service: {
          fetchTopologyHostCount: HostAllManageService.fetchTopologyWithCount,
          fetchTopologyHostsNodes: HostAllManageService.fetchTopologyHost,
          fetchTopologyHostIdsNodes: HostAllManageService.fetchTopogyHostIdList,
          fetchHostsDetails: HostAllManageService.fetchHostInfoByHostId,
          fetchHostCheck: HostAllManageService.fetchInputParseHostList,
        },
      };

      this.rules = {
        hostList: [
          {
            validator: value => value.length > 0,
            message: I18n.t('whiteIP.IP必填'),
            trigger: 'change',
          },
        ],
        remark: [
          {
            required: true,
            message: I18n.t('whiteIP.备注必填'),
            trigger: 'blur',
          },

        ],
        actionScopeIdList: [
          {
            validator: value => value.length > 0,
            message: I18n.t('whiteIP.生效范围必填'),
            trigger: 'blur',
          },
        ],
      };
    },
    methods: {
      /**
       * @desc 业务列表
       */
      fetchAppList() {
        return AppManageService.fetchAppList()
          .then((data) => {
            this.appList = data.map(item => ({
              ...item,
              localKey: `#${item.scopeType}#${item.scopeId}`,
            }));
            // 默认选中第一个
            if (this.formData.scopeList.length < 1) {
              const [
                {
                  scopeType,
                  scopeId,
                },
              ] = data;
              this.formData.scopeList = [
                {
                  scopeType,
                  scopeId,
                },
              ];
            }
            this.scopeValue = this.formData.scopeList.map(item => `#${item.scopeType}#${item.scopeId}`);
          });
      },
      /**
       * @desc 获取生效范围列表
       */
      fetchActionScope() {
        return WhiteIpService.getScope()
          .then((data) => {
            this.actionScope = data;
          });
      },

      // 切换全业务
      handleAllAPP(value) {
        this.formData.allScope = value;
      },

      handleShowIpSelector() {
        this.isShowIpSelector = true;
      },
      handleColseIpSelector() {
        this.isShowIpSelector = false;
      },
      handleIpSelectorChange(data) {
        this.formData.hostList = data.hostList;
        if (data.hostList.length > 0) {
          this.$refs.whiteIpForm.clearError('actionScopeIdList');
        }
      },
      handleRangeChange(value) {
        if (value.length > 0) {
          this.$refs.whiteIpForm.clearError('hostList');
        }
      },

      submit() {
        return this.$refs.whiteIpForm.validate()
          .then(() => {
            const params = { ...this.formData };
            if (params.id < 1) {
              delete params.id;
            }
            if (params.allScopeh) {
              params.scopeList = [];
            } else {
              params.scopeList = this.scopeValue.map((scopeLocalKey) => {
                const [
                  ,
                  type,
                  id,
                ] = scopeLocalKey.match(/^#([^#]+)#(.+)$/);
                return {
                  type,
                  id,
                };
              });
            }

            return WhiteIpService.whiteIpUpdate(params)
              .then(() => {
                this.messageSuccess(this.formData.id ? I18n.t('whiteIP.编辑成功') : I18n.t('whiteIP.新建成功'));
                this.$emit('on-update');
              });
          });
      },
    },
  };
</script>
<style lang='postcss'>
  .app-wraper {
    display: flex;
    align-items: center;

    .app-select {
      flex: 1;
    }

    .whole-business {
      margin-left: 10px;
    }
  }

  .scope-checkbox {
    margin-right: 30px;
  }
</style>
