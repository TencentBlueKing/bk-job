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
                        v-model="scopeLocalKeyList"
                        class="app-select"
                        :clearable="false"
                        multiple
                        searchable>
                        <bk-option
                            v-for="option in appList"
                            :id="option.localKey"
                            :key="option.localKey"
                            :name="option.name" />
                    </bk-select>
                </div>
            </jb-form-item>
            <jb-form-item
                :label="$t('whiteIP.云区域')"
                property="cloudAreaId"
                required>
                <bk-select
                    v-model="formData.cloudAreaId"
                    class="input"
                    :clearable="false"
                    searchable>
                    <bk-option
                        v-for="option in cloudAreaList"
                        :id="option.id"
                        :key="option.id"
                        :name="option.name" />
                </bk-select>
            </jb-form-item>
            <jb-form-item
                label="IP"
                property="ipStr"
                required>
                <bk-input
                    v-model="formData.ipStr"
                    class="input"
                    :placeholder="$t('whiteIP.输入IP，以“回车”分隔')"
                    type="textarea" />
            </jb-form-item>
            <jb-form-item
                :label="$t('whiteIP.备注')"
                property="remark"
                required>
                <bk-input
                    v-model="formData.remark"
                    class="input"
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
    import WhiteIpService from '@service/white-ip';

    import I18n from '@/i18n';

    const getDefaultData = () => ({
        id: 0,
        // 生效范围
        actionScopeIdList: [],
        // 业务ID
        scopeList: [],
        // 云区域ID
        cloudAreaId: '',
        ipStr: '',
        // 备注
        remark: '',
    });

    const getScopeLocalKey = scopeData => `#${scopeData.scopeType}#${scopeData.scopeId}`;

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
                isLoading: true,
                formData: getDefaultData(),
                scopeLocalKeyList: [],
                appList: [],
                cloudAreaList: [],
                actionScope: [],
            };
        },
        watch: {
            data: {
                handler (data) {
                    if (!data.id) {
                        return;
                    }
                    const {
                        id,
                        actionScopeList,
                        cloudAreaId,
                        appList,
                        remark,
                        hostList,
                    } = data;
                    this.formData = {
                        ...this.formData,
                        id,
                        actionScopeIdList: actionScopeList.map(item => item.id),
                        cloudAreaId,
                        scopeList: appList.map(item => ({
                            type: item.scopeType,
                            id: item.scopeId,
                        })),
                        remark,
                        ipStr: hostList.map(({ ip }) => ip).join('\n'),
                    };
                },
                immediate: true,
            },
        },
        created () {
            Promise.all([
                this.fetchAppList(),
                this.fetchAllCloudArea(),
                this.fetchActionScope(),
            ]).finally(() => {
                this.isLoading = false;
            });
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
        },
        methods: {
            /**
             * @desc 业务列表
             */
            fetchAppList () {
                return AppManageService.fetchAppList()
                    .then((data) => {
                        this.appList = data.map(item => ({
                            ...item,
                            localKey: getScopeLocalKey(item),
                        }));
                        if (this.formData.scopeList.length < 1) {
                            const [
                                {
                                    scopeType,
                                    scopeId,
                                },
                            ] = data;
                            this.formData.scopeList = [{
                                type: scopeType,
                                id: scopeId,
                            }];
                        }
                        this.scopeLocalKeyList = this.formData.scopeList.map(item => `#${item.type}#${item.id}`);
                    });
            },
            /**
             * @desc 获取云区域列表
             */
            fetchAllCloudArea () {
                return WhiteIpService.getAllCloudArea()
                    .then((data) => {
                        this.cloudAreaList = data;
                        if (this.formData.cloudAreaId === '') {
                            this.formData.cloudAreaId = this.cloudAreaList[0].id;
                        }
                    });
            },
            /**
             * @desc 获取生效范围列表
             */
            fetchActionScope () {
                return WhiteIpService.getScope()
                    .then((data) => {
                        this.actionScope = data;
                    });
            },
            
            handleRangeChange (value) {
                if (value.length > 0) {
                    this.$refs.whiteIpForm.clearError('actionScopeIdList');
                }
            },

            submit () {
                return this.$refs.whiteIpForm.validate()
                    .then(() => {
                        const params = { ...this.formData };
                        if (params.id < 1) {
                            delete params.id;
                        }
                        params.scopeList = this.scopeLocalKeyList.map((scopeLocalKey) => {
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

    .input {
        width: 495px;
    }

    .scope-checkbox {
        margin-right: 30px;
    }
</style>
