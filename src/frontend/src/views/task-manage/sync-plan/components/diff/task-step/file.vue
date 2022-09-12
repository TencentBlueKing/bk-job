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
    <div>
        <div
            class="row"
            :class="diff.timeout">
            <span class="label">{{ $t('template.超时时长：') }}</span>
            <span class="value">{{ data.timeout }}(s)</span>
        </div>
        <div
            class="row"
            :class="diff.ignoreError">
            <span class="label">{{ $t('template.错误处理：') }}</span>
            <span class="value">{{ data.ignoreErrorText }}</span>
        </div>
        <div
            class="row"
            :class="diff.uploadSpeedLimit">
            <span class="label">{{ $t('template.上传限速：') }}</span>
            <span class="value">{{ data.uploadSpeedLimitText }}</span>
        </div>
        <div
            class="row"
            :class="diff.downloadSpeedLimit">
            <span class="label">{{ $t('template.下载限速：') }}</span>
            <span class="value">{{ data.downloadSpeedLimitText }}</span>
        </div>
        <div>
            <p class="label">
                {{ $t('template.文件来源：') }}
            </p>
            <file-source
                :id="id"
                :account="account"
                :data="data.originFileList" />
        </div>
        <div
            class="row"
            :class="diff.destinationFileLocation">
            <span class="label">{{ $t('template.目标路径：') }}</span>
            <span class="value">{{ data.destinationFileLocation }}</span>
        </div>
        <div
            class="row"
            :class="diff.transferMode">
            <span class="label">{{ $t('template.传输模式：') }}</span>
            <span class="value">{{ data.transferModeText }}</span>
        </div>
        <div
            class="row"
            :class="diff.executeAccount">
            <span class="label">{{ $t('template.执行帐号：') }}</span>
            <span class="value">{{ findName(data.executeAccount) }}</span>
        </div>
        <div
            class="row"
            :class="diff.destination">
            <span class="label">{{ $t('template.执行目标：') }}</span>
            <file-execute-target />
        </div>
    </div>
</template>
<script>
    import FileExecuteTarget from './components/file-execute-target';
    import FileSource from './components/file-source';

    export default {
        name: '',
        components: {
            FileSource,
            FileExecuteTarget,
        },
        props: {
            id: {
                type: Number,
                required: true,
            },
            data: {
                type: Object,
                required: true,
            },
            diff: {
                type: Object,
                default: () => ({}),
            },
            account: {
                type: Array,
                default: () => [],
            },
        },
        methods: {
            findName (accountId) {
                const account = this.account.find(_ => _.id === accountId);
                if (!account) {
                    return '-';
                }
                return account.alias;
            },
        },
    };
</script>
