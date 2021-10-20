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
    <jb-form :model="formData" ref="varHostForm">
        <jb-form-item :label="$t('template.变量名称')">
            <bk-input v-model="formData.name" disabled />
        </jb-form-item>
        <jb-form-item :label="$t('template.变量值')">
            <section>
                <bk-button @click="handleShowChooseIp">
                    <Icon type="plus" />
                    {{ $t('template.选择主机') }}
                </bk-button>
                <bk-button style="margin-left: 10px;" v-if="isShowClear" @click="handleClear">
                    {{ $t('template.清空') }}
                </bk-button>
            </section>
            <server-panel
                class="view-server-panel"
                :host-node-info="formData.defaultTargetValue.hostNodeInfo"
                editable
                detail-fullscreen
                @on-change="handleHostChange" />
        </jb-form-item>
        <jb-form-item :label="$t('template.变量描述')">
            <bk-input v-model="formData.description" disabled type="textarea" :row="5" maxlength="100" />
        </jb-form-item>
        <jb-form-item>
            <bk-checkbox v-model="formData.required" disabled :true-value="1" :false-value="0">{{ $t('template.必填') }}</bk-checkbox>
        </jb-form-item>
        <choose-ip
            v-model="isShowChooseIp"
            :host-node-info="formData.defaultTargetValue.hostNodeInfo"
            @on-change="handleHostChange" />
    </jb-form>
</template>
<script>
    import TaskGlobalVariableModel from '@model/task/global-variable';
    import TaskHostNodeModel from '@model/task-host-node';
    import ChooseIp from '@components/choose-ip';
    import ServerPanel from '@components/choose-ip/server-panel';

    const getDefaultData = () => ({
        id: 0,
        delete: 0,
        // 变量名
        name: '',
        // 执行目标信息
        defaultTargetValue: {
            hostNodeInfo: {},
            variable: '',
        },
        // 变量描述
        description: '',
        // 必填 0-非必填 1-必填
        required: 0,
    });

    export default {
        name: 'VarHost',
        components: {
            ChooseIp,
            ServerPanel,
        },
        props: {
            data: {
                type: Object,
                default: () => ({}),
            },
        },
        data () {
            return {
                formData: {},
                isShowChooseIp: false,
            };
        },
        computed: {
            isShowClear () {
                return !TaskHostNodeModel.isHostNodeInfoEmpty(this.formData.defaultTargetValue.hostNodeInfo);
            },
        },
        watch: {
            data: {
                handler (data) {
                    this.formData = new TaskGlobalVariableModel(data);
                },
                immediate: true,
            },
        },
        methods: {
            handleHostChange (hostNodeInfo) {
                this.formData.defaultTargetValue.hostNodeInfo = Object.freeze(hostNodeInfo);
            },
            handleShowChooseIp () {
                this.isShowChooseIp = true;
            },

            handleClear () {
                const { hostNodeInfo } = new TaskHostNodeModel({});
                this.formData.defaultTargetValue.hostNodeInfo = hostNodeInfo;
            },
            submit () {
                return Promise.resolve({
                    ...this.formData,
                    type: TaskGlobalVariableModel.TYPE_HOST,
                });
            },

            reset () {
                this.formData = new TaskGlobalVariableModel(getDefaultData);
            },
        },
    };
</script>
<style lang="postcss" scoped>
    .view-server-panel {
        margin-top: 10px;
    }
</style>
