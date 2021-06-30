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
        名称: 'NAME',
        创建人: 'CREATED BY',
        '搜索 ID、名称、描述、创建人、更新人...': 'Search by  Name / Describe / Created By / Last Modify By...',
        凭证ID: 'ID',
        凭证名称: 'NAME',
        类型: {
            label: 'Type',
            colHead: 'TYPE',
        },
        被引用: {
            label: 'Related',
            colHead: 'RELATED',
        },
        描述: 'DESCRIPTION',
        创建时间: 'CREATED ON',
        更新人: 'LAST MODIFY BY',
        更新时间: 'LAST MODIFIED ON',
        操作: 'ACTIONS',
        删除: 'Delete',
        用户名: 'Username',
        密码: 'Password',
        AppId: 'AppID',
        SecretKey: 'SecretKey',
        新建凭证: 'New Ticket',
        编辑凭证: 'Editing Ticket',
        单一密码: 'Password Only',
        '用户名+密码': 'Username + Password',
        单一SecretKey: 'SecretKey Only',
        'AppID+SecretKey': 'AppID + SecretKey',
        提交: 'Commit',
        取消: 'Cancel',
        编辑: 'Edit',
        凭证名称必填: 'Ticket Name is required',
        用户名必填: 'Username is required',
        密码必填: 'Password is required',
        AppID必填: 'AppID is required',
        SecretKey必填: 'SecretKey is required',
        删除成功: 'Delete Successful',
        创建成功: 'Create Successful',
        更新成功: 'Update Successful',
        '确定删除该凭证？': 'Are you sure to delete it?',
        '正在被文件源使用的凭证无法删除，如需删除请先解除引用关系。': 'If the ticket is related to some File Source, you need to remove it first.',
        文件源别名: 'File source alias',
        '搜索文件源别名...': 'Search file source...',
        文件源标识: 'ID',
        状态: 'STATES',
        保存: 'Save',
        '接入点异常，暂时不可用': 'All AP are abnormal, file-source temporarily unavailable.',
    },
};
