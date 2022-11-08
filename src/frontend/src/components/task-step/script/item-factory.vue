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
    <component
        :is="itemCom"
        ref="item"
        v-bind="$attrs"
        v-on="$listeners" />
</template>
<script>
    import ErrorHandle from '../common/error-handle';
    import ScriptName from '../common/name';
    import Rolling from '../common/rolling';
    import ScriptTimeout from '../common/timeout';

    import ExecuteAccount from './strategy/execute-account';
    import ExecuteTargetOfExecution from './strategy/execute-target-of-execution';
    import ExecuteTargetOfTemplate from './strategy/execute-target-of-template';
    import ScriptContent from './strategy/script-content';
    import ScriptParam from './strategy/script-param';
    import ScriptSourceOfExecution from './strategy/script-source-of-execution';
    import ScriptSourceOfTemplate from './strategy/script-source-of-template';

    export default {
        name: 'ExecuteScriptItemFactory',
        props: {
            name: {
                type: String,
                required: true,
            },
        },
        computed: {
            itemCom () {
                const comMap = {
                    scriptName: ScriptName,
                    scriptSourceOfExecution: ScriptSourceOfExecution,
                    scriptSourceOfTemplate: ScriptSourceOfTemplate,
                    errorHandle: ErrorHandle,
                    scriptContent: ScriptContent,
                    scriptParam: ScriptParam,
                    scriptTimeout: ScriptTimeout,
                    scriptAccount: ExecuteAccount,
                    executeTargetOfTemplate: ExecuteTargetOfTemplate,
                    executeTargetOfExecution: ExecuteTargetOfExecution,
                    rolling: Rolling,
                };
                if (!Object.prototype.hasOwnProperty.call(comMap, this.name)) {
                    return 'div';
                }
                return comMap[this.name];
            },
        },
    };
</script>
