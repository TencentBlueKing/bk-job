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
    namespace: 'script',
    message: {
        新建: 'New',
        '搜索脚本名称，类型，场景标签，更新人...': 'Search by Script Name / Type / Tag / Created By...',
        脚本语言: 'TYPE',
        线上版本: 'ON-LINE',
        创建时间: 'CREATED ON',
        操作: 'ACTIONS',
        去执行: 'Launch',
        同步: 'Sync',
        '更新人：': 'Last Modify By：',
        '更新时间：': 'LAST MODIFIED ON: ',
        '确定删除该脚本？': 'Are you sure to delete it?',
        '注意！脚本内的所有版本也将被清除': 'Caution! All Versions will also be deleted.',
        删除: 'Delete',
        类型: 'Type',
        脚本名称必填: 'Script name is required',
        删除成功: 'Script has been deleted.',
        '操作失败！请前往「版本管理」设置线上版后重试': 'Failed! No online version available.',
        '脚本正被作业引用中，无法删除': 'Script is related with Jobs, cannot be delete.',
        显示被作业引用的次数: 'Show the related Jobs list',
        '搜索名称，版本号': 'Search by Name or Version number...',
        作业模板: 'TEMPLATE NAME',
        执行方案: 'PLAN NAME',
        引用的版本号: 'VERSION No.',
        引用版本状态: 'VERSION STATUS',
        版本状态: 'Status',
        名称: 'NAME',
        输入版本号: 'Type the version number...',
        版本日志: 'ChangeLog',
        脚本内容: 'Contents',
        提交: 'Commit',
        取消: 'Cancel',
        立即同步: 'Synchronize Now',
        全部重试: 'All retry',
        重试: 'Retry',
        完成: 'inish',
        同步作业模版: 'Synchronize job templates',
        版本号必填: 'Version number is required',
        '版本号已存在，请重新输入': 'Version number is already exist, please try another one...',
        操作成功: 'Successful',
        '推荐按照该脚本逻辑提供的使用场景来取名...': 'Type your script name here...',
        标签对资源的分类管理有很大帮助: 'Select or type any tag you want...',
        在此处标注该脚本的备注和使用说明: 'Type the usage scenario description of this script here...',
        上传脚本: 'Upload',
        '标签名已存在，请重新输入': 'Tag name is already exist, please try another one...',
        脚本版本必填: 'Script version number is required',
        '最多仅可 200个字符': 'Up to 200 characters...',
        脚本类型不支持: 'Script type not supported',
        脚本内容不能为空: 'Script content is required',
        '脚本名称：': 'Name: ',
        '场景标签：': 'Tags: ',
        '描述：': 'Desc: ',
        '创建人：': 'Created By: ',
        '创建时间：': 'Created On: ',
        版本列表: 'Versions',
        '该脚本没有线上版本，无法执行': 'Failed! No Online version available.',
        线上版: 'Online',
        版本管理: 'Versions',
        上传文件: 'Upload',
        版本对比: 'Diff',
        '直接输入 版本号 或 更新人 进行全局模糊搜索': 'Search by Version No. or Modified By...',
        更新时间: 'LAST MODIFIED ON',
        状态: 'STATUS',
        '确定上线该版本？': 'Are you sure ?',
        '上线后，之前的线上版本将被置为「已下线」状态，但不影响作业使用': 'After going Online, the previous online version will be set to "Offline", but it still can be execute.',
        上线: 'On',
        '确定禁用该版本？': 'Are you sure to ban it ?',
        '一旦禁用成功，线上引用该版本的作业脚本步骤都会执行失败，请务必谨慎操作！': 'IMPORTANT! once the version is banned, the job steps that quote to this version will fail to execute, please be careful!',
        禁用: 'Ban',
        '确定删除该版本？': 'Are you sure to delete it ?',
        '删除后不可恢复，请谨慎操作！': 'Caution! It cannot be restored after deletion.',
        编辑: 'Edit',
        复制并新建: 'Copy New',
        描述: 'Desc',
        编辑脚本: 'Script Editing',
        新建脚本: 'New Script',
        查看脚本: 'View Details',
        作业模板名称: 'Template Name',
        同步作业模板: 'Sync Template',
        步骤名称: 'Step Name',
        已选: 'Selected',
        搜索作业模板: 'Search job template',
        '版本 ID': ' VERSION ID',
        脚本名称: {
            label: 'Name',
            colHead: 'NAME',
        },
        场景标签: {
            label: 'Tags',
            colHead: 'TAGS',
        },
        被引用: {
            label: 'Related',
            colHead: 'RELATED',
        },
        创建人: {
            label: 'Created By',
            colHead: 'CREATED BY',
        },
        版本号: {
            label: 'Version No.',
            colHead: 'VERSION NO.',
        },
        更新人: {
            label: 'Last Modify By',
            colHead: 'LAST MODIFIED BY',
        },
        '该脚本没有 “线上版本” 可执行，请前往版本管理内设置。': 'There is no "ON-LINE" version yet, go to "Versions" page and set one.',
        已上线: 'Online',
        未上线: 'Stand-by',
        已下线: 'Offline',
        已禁用: 'Banned',
        所有关联作业模板已是当前版本: 'Associated Job Templates are already using current version',
        '部分作业模板同步失败，请留意': 'Some of Job Template is sync failed',
        同步至: 'Update to',
        将覆盖其它条件: 'override other conditions',
        作业模版引用: 'Related by Job Template',
        执行方案引用: 'Related by Job Plan',
        引用脚本的作业模版: 'List of Related Job Template',
        引用脚本的执行方案: 'List of Related Job Plan',
        共: '',
        个: {
            result: 'totals',
        },
        请先选择作业模版步骤: 'Job Template not selected yet',
        显示被执行方案引用的次数: 'Shows the number of times related by Job Template or plan',
    },
};
