/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 *
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
*/

/* eslint-disable max-len */
export default {
    namespace: 'dangerousRule',
    message: {
        语法检测表达式: 'GRAMMAR REGEX',
        规则说明: 'EXPLANATION',
        脚本类型: 'SCRIPT TYPE',
        动作: 'ACTION',
        操作: 'ACTIONS',
        '规则的排序越靠前，表示检测优先级越高': 'The higher the order of the rules, the higher the detect priority',
        上移: 'Up',
        下移: 'Down',
        删除: 'Delete',
        编辑成功: 'Change has been saved.',
        上移成功: 'Change has been saved(Move Up).',
        下移成功: 'Change has been saved(Move Down).',
        删除成功: 'Rule has been deleted.',
        '确定删除该规则？': 'Are you sure ?',
        脚本编辑器中匹配该规则将不会再收到提醒: 'The editor will no longer be reminded if the rule has been deleted.',
        新增检测规则: 'New Rule',
        扫描: 'Scan',
        拦截: 'Block',
        保存: 'Save',
        取消: 'Cancel',
        '【扫描】': 'SCAN',
        '命中规则的脚本执行任务仅会做记录，不会拦截': 'The script execution task that hits the rule will only be recorded and not be blocked',
        '【拦截】': 'BLOCK',
        '命中规则的脚本执行任务会被记录，并中止运行': 'The script execution task that hits the rule will be recorded and blocked',
        语法检测表达式不能为空: 'Grammar regex is required',
        规则说明不能为空: 'Explanaction is required',
        脚本类型不能为空: 'Script type is required',
        请填写完整的语法检测表达式和说明: 'Grammar Regex and Description not defined.',
        新增成功: 'New rule has been created',
    },
};
