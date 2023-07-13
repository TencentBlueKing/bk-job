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
    新建: 'New',
    语法检测表达式: {
      col: 'Statement Regex.',
      label: 'Statement Regex.',
    },
    规则说明: {
      col: 'Explanation',
      label: 'Explanation',
    },
    脚本类型: {
      col: 'Script type',
      label: 'Script type',
    },
    动作: {
      col: 'Action',
      label: 'Action',
    },
    操作: 'Actions',
    '规则的排序越靠前，表示检测优先级越高': 'The higher the order of the rules, the higher the detect priority',
    上移: 'Up',
    下移: 'Down',
    删除: 'Delete',
    编辑成功: 'Change has been saved.',
    上移成功: 'Change has been saved(move up).',
    下移成功: 'Change has been saved(move down).',
    删除成功: 'Rule has been deleted.',
    '确定删除该规则？': 'Are you sure ?',
    脚本编辑器中匹配该规则将不会再收到提醒: 'The editor will no longer be reminded if the rule has been deleted.',
    新增检测规则: 'New rule',
    扫描: 'Scan',
    拦截: 'Block',
    保存: 'Save',
    取消: 'Cancel',
    '【扫描】': 'SCAN',
    '命中规则的脚本执行任务仅会做记录，不会拦截': 'The script execution task that hits the rule will only be recorded and not be blocked',
    '【拦截】': 'BLOCK',
    '命中规则的脚本执行任务会被记录，并中止运行': 'The script execution task that hits the rule will be recorded and blocked',
    语法检测表达式不能为空: 'Statement Regex. is required',
    规则说明不能为空: 'Explanaction is required',
    脚本类型不能为空: 'Script type is required',
    请填写完整的语法检测表达式和说明: 'Statement Regex. and description not defined.',
    新增成功: 'New rule has been created',
    '搜索语法检测表达式，规则说明，脚本类型...': 'Search by Statement Regex. / Explanation / Script type / ...',
    语法检测表达式必填: 'Statement Regex. is required',
    规则说明必填: 'Statement explanation is required',
    脚本类型必填: 'Script type is required',
    动作必填: 'Action is required',
    创建人: 'Created by',
    更新人: 'Last modified by',
    更新时间: 'Last modified at',
    创建时间: 'Created at',
  },
};
