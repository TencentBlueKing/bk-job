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

export default {
  namespace: 'account',
  message: {
    新建: 'New',
    '搜索账号别名，名称，更新人...': 'Search by Account name / Alias / Last modified by ...',
    '账号 ID': 'ID',
    更新时间: 'Last modified at',
    操作: 'Actions',
    编辑: 'Edit',
    '确定删除该账号？': 'Are you sure?',
    '删除后不可恢复，请谨慎操作！': 'Caution! It cannot be restored after deletion.',
    删除: 'Delete',
    编辑账号: 'Edit',
    保存: 'Save',
    新建账号: 'New account',
    提交: 'Submit',
    系统账号: 'System',
    数据库账号: 'Database',
    删除成功: 'Delete successfully',
    数据库类型: 'Database',
    名称: 'Name',
    别名: 'Alias',
    '在出现同名账号的情况下，账号的别名显得格外重要...': 'In the case of an account with the same name, alias is needed...',
    密码: 'Password',
    输入密码: 'Type password...',
    确认密码: 'Password confirm ',
    输入确认密码: 'Confirm password...',
    端口: 'Port',
    输入确认端口: 'Type your database port...',
    依赖系统账号: 'Dependent system account',
    选择依赖系统账号: 'Select dependent system account',
    输入账号描述: 'Type the usage scenario description of the account here...',
    类型: 'Type',
    用途: 'Usage',
    名称必填: 'Account name is required',
    别名必填: 'Account alias is required',
    密码不一致: 'The passwords are not equal',
    密码不支持中文: 'Password does not support chinese',
    端口必填: 'Database port is required',
    依赖系统账号必填: 'Dependent system account is required',
    密码必填: 'Password is required',
    新建账号成功: 'New account has been created',
    '命名规则请求失败无法执行当前操作，请刷新页面': 'The naming convention settings request failed, please try to refresh current page.',
    编辑账号成功: 'Account has been modified',
    取消: 'Cancel',
    保存成功: 'The changes has been saved',

    账号别名: {
      label: 'Account alias',
      colHead: 'Account alias',
    },
    账号名称: {
      label: 'Name',
      colHead: 'Name',
    },
    描述: {
      label: 'Descriptions',
      colHead: 'Descriptions',
    },
    账号用途: {
      label: 'Usage',
      colHead: 'Usage',
    },
    账号类型: {
      label: 'Type',
      colHead: 'Type',
    },
    创建人: 'Created by',
    创建时间: 'Created at',
    更新人: {
      label: 'Last modified by',
      colHead: 'Last modified by',
    },
    将覆盖其它条件: 'override other conditions',
    重置: 'Reset',
  },
};
