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
        <jb-form ref="whiteIpForm" form-type="vertical" :model="formData" :rules="rules">
            <jb-form-item :label="$t('whiteIP.目标业务.label')">
                <div class="app-wraper">
                    <bk-select
                        class="app-select"
                        :clearable="false"
                        v-model="formData.appIdStr"
                        multiple
                        :disabled="isAll"
                        searchable>
                        <bk-option
                            v-for="option in appList"
                            :key="option.id"
                            :id="option.id"
                            :name="option.name" />
                    </bk-select>
                    <bk-checkbox
                        v-if="wholeBusinessId"
                        class="whole-business"
                        :value="isAll"
                        @change="handleAllAPP">
                        {{ $t('whiteIP.全业务') }}
                    </bk-checkbox>
                </div>
            </jb-form-item>
            <jb-form-item :label="$t('whiteIP.云区域')" required property="cloudAreaId">
                <bk-select
                    class="input"
                    :clearable="false"
                    v-model="formData.cloudAreaId"
                    searchable>
                    <bk-option
                        v-for="option in cloudAreaList"
                        :key="option.id"
                        :id="option.id"
                        :name="option.name" />
                </bk-select>
            </jb-form-item>
            <jb-form-item label="IP" required property="ipStr">
                <bk-input
                    :placeholder="$t('whiteIP.输入IP，以“回车”分隔')"
                    class="input"
                    type="textarea"
                    v-model="formData.ipStr" />
            </jb-form-item>
            <jb-form-item :label="$t('whiteIP.备注')" required property="remark">
                <bk-input class="input" type="textarea" v-model="formData.remark" :maxlength="100" />
            </jb-form-item>
            <jb-form-item
                :label="$t('whiteIP.生效范围.label')"
                required
                property="actionScopeIdList"
                style="margin-bottom: 0;">
                <bk-checkbox-group v-model="formData.actionScopeIdList" @change="handleRangeChange">
                    <bk-checkbox
                        v-for="(item, index) in actionScope"
                        :key="item.id"
                        :value="item.id"
                        :class="{ 'scope-checkbox': index !== actionScope.length - 1 }">
                        {{ item.name }}
                    </bk-checkbox>
                </bk-checkbox-group>
            </jb-form-item>
        </jb-form>
    </div>
</template>
<script>
    import I18n from '@/i18n';
    import WhiteIpService from '@service/white-ip';
    import AppManageService from '@service/app-manage';

    const getDefaultData = () => ({
        id: 0,
        // 生效范围
        actionScopeIdList: [],
        // 业务ID
        appIdStr: '',
        // 云区域ID
        cloudAreaId: '',
        ipStr: '',
        // 备注
        remark: '',
    });
    export default {
        name: '',
        props: {
            data: {
                type: Object,
                default: () => ({}),
            },
        },
        data () {
            return {
                // isAll: false,
                formData: getDefaultData(),
                appList: [],
                wholeBusinessId: '',
                cloudAreaList: [],
                actionScope: [],
                requestQueue: ['app', 'cloundArea', 'scope'],
            };
        },
        computed: {
            isLoading () {
                return this.requestQueue.length > 0;
            },
            isAll () {
                return this.formData.appIdStr.includes(this.wholeBusinessId);
            },
        },
        watch: {
            data: {
                handler (data) {
                    if (!data.id) {
                        return;
                    }
                    const { id, actionScopeList, cloudAreaId, appList, remark, ipList } = data;
                    this.formData = {
                        ...this.formData,
                        id,
                        actionScopeIdList: actionScopeList.map(item => item.id),
                        cloudAreaId,
                        appIdStr: appList.map(_ => _.id),
                        remark,
                        ipStr: ipList.join('\n'),
                    };
                },
                immediate: true,
            },
        },
        created () {
            this.rules = {
                ipStr: [
                    {
                        required: true,
                        message: I18n.t('whiteIP.IP必填'),
                        trigger: 'blur',
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
            this.fetchAppList();
            this.fetchWholeBusinessId();
            this.fetchAllCloudArea();
            this.fetchActionScope();
        },
        methods: {
            fetchAppList () {
                AppManageService.fetchAppListWholeBusiness()
                    .then((data) => {
                        this.appList = data;
                        if (this.formData.appIdStr.length < 1) {
                            this.formData.appIdStr = [this.appList[0].id];
                        }
                    })
                    .finally(() => {
                        this.requestQueue.pop();
                    });
            },
            fetchWholeBusinessId () {
                AppManageService.fetchWholeBusinessId()
                    .then((data) => {
                        this.wholeBusinessId = data;
                    });
            },
            fetchAllCloudArea () {
                WhiteIpService.getAllCloudArea()
                    .then((data) => {
                        this.cloudAreaList = data;
                        if (this.formData.cloudAreaId === '') {
                            this.formData.cloudAreaId = this.cloudAreaList[0].id;
                        }
                    })
                    .finally(() => {
                        this.requestQueue.pop();
                    });
            },
            fetchActionScope () {
                WhiteIpService.getScope()
                    .then((data) => {
                        this.actionScope = data;
                    })
                    .finally(() => {
                        this.requestQueue.pop();
                    });
            },
            handleAllAPP (value) {
                if (value) {
                    this.formData.appIdStr = [this.wholeBusinessId];
                } else if (this.appList.length > 0) {
                    this.formData.appIdStr = [this.appList[0].id];
                }
            },
            handleRangeChange (value) {
                if (value.length > 0) {
                    this.$refs.whiteIpForm.clearError('actionScopeIdList');
                }
            },

            submit () {
                return this.$refs.whiteIpForm.validate()
                    .then((validator) => {
                        const params = { ...this.formData };
                        if (params.id < 1) {
                            delete params.id;
                        }
                        params.appIdStr = params.appIdStr.join(',');
                        return WhiteIpService.whiteIpUpdate(params)
                            .then(() => {
                                this.messageSuccess(this.formData.id ? I18n.t('whiteIP.编辑成功') : I18n.t('whiteIP.新建成功'));
                                this.$emit('on-update');
                            })
                            .finally(() => {
                                this.reset();
                            });
                    });
            },

            reset () {
            // this.$refs.whiteIpForm.clearError()
            // this.formData = getDefaultData()
            // this.formData.cloudAreaId = this.cloudAreaList[0].id
            // this.formData.appId = this.appList[0].id
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

    .input {
        width: 495px;
    }

    .scope-checkbox {
        margin-right: 30px;
    }
</style>
