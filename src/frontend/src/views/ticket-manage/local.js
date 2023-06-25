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
  namespace: 'ticket',
  message: {
    新建: 'New',
    名称: 'Name',
    创建人: 'Created by',
    '搜索 ID、名称、描述、创建人、更新人...': 'Search by  Name / Description / Created by / Last modified By...',
    凭证ID: 'ID',
    凭证名称: 'Name',
    类型: {
      label: 'Type',
      colHead: 'Type',
    },
    被引用: {
      label: 'Related',
      colHead: 'Related',
    },
    描述: 'Description',
    创建时间: 'Created at',
    更新人: 'Last modified by',
    更新时间: 'Last modified at',
    操作: 'Actions',
    删除: 'Delete',
    用户名: 'Username',
    密码: 'Password',
    AppId: 'AppID',
    SecretKey: 'SecretKey',
    新建凭证: 'New Ticket',
    编辑凭证: 'Editing Ticket',
    单一密码: 'Password only',
    '用户名+密码': 'Username + Password',
    单一SecretKey: 'SecretKey only',
    'AppID+SecretKey': 'AppID + SecretKey',
    提交: 'Submit',
    取消: 'Cancel',
    编辑: 'Edit',
    凭证名称必填: 'Ticket name is required',
    用户名必填: 'Username is required',
    密码必填: 'Password is required',
    AppID必填: 'AppID is required',
    SecretKey必填: 'SecretKey is required',
    删除成功: 'Delete successfully',
    创建成功: 'Create successfully',
    更新成功: 'Update successfully',
    '确定删除该凭证？': 'Are you sure to delete it?',
    '正在被文件源使用的凭证无法删除，如需删除请先解除引用关系。': 'If the ticket is related to some file source, you need to remove it first.',
    文件源别名: 'File source alias',
    '搜索文件源别名...': 'Search file source...',
    文件源标识: 'ID',
    状态: 'States',
    保存: 'Save',
    '接入点异常，暂时不可用': 'All AP are abnormal, file-source temporarily unavailable.',
  },
};
