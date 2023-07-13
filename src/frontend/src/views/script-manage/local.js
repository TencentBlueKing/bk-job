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
    '搜索脚本名称，类型，场景标签，更新人...': 'Search by Script Name / Type / Tag / Created by...',
    脚本语言: 'Type',
    线上版本: 'On-line',
    创建时间: 'Created at',
    操作: 'Actions',
    去执行: 'Launch',
    同步: 'Sync',
    '更新人：': 'Last modified by：',
    '更新时间：': 'Last modified at：',
    '确定删除该脚本？': 'Are you sure to delete it?',
    '注意！脚本内的所有版本也将被清除': 'Caution! All versions will also be deleted.',
    删除: 'Delete',
    类型: 'Type',
    脚本名称必填: 'Script name is required',
    删除成功: 'Script has been deleted.',
    '操作失败！请前往「版本管理」设置线上版后重试': 'Failed! No online version available.',
    '脚本正被作业引用中，无法删除': 'Script is related with jobs, cannot be delete.',
    显示被作业引用的次数: 'Show the related jobs list',
    '搜索名称，版本号': 'Search by Name or Version number...',
    作业模板: 'Template name',
    执行方案: 'Plan name',
    引用的版本号: 'Version no.',
    引用版本状态: 'Version status',
    版本状态: 'Status',
    名称: 'name',
    输入版本号: 'Type the version number...',
    版本日志: {
      label: 'Change log',
      colHead: 'Change log',
    },
    脚本内容: {
      label: 'Contents',
      colHead: 'Contents',
    },
    提交: 'Submit',
    取消: 'Cancel',
    立即同步: 'Synchronize now',
    全部重试: 'All retry',
    重试: 'Retry',
    完成: 'inish',
    同步作业模板: 'Synchronize job templates',
    版本号必填: 'Version number is required',
    '版本号已存在，请重新输入': 'Version number is already exist, please try another one...',
    操作成功: 'Successfully',
    '推荐按照该脚本逻辑提供的使用场景来取名...': 'Type your script name here...',
    标签对资源的分类管理有很大帮助: 'Select or type any tag you want...',
    在此处标注该脚本的备注和使用说明: 'Type the usage scenario description of this script here...',
    上传脚本: 'Upload',
    '标签名已存在，请重新输入': 'Tag name is already exist, please try another one...',
    脚本版本必填: 'Script version number is required',
    '最多仅可 200个字符': 'Up to 200 characters...',
    脚本类型不支持: 'Script type not supported',
    脚本内容不能为空: 'Script content is required',
    脚本描述: {
      label: 'Desc',
    },
    版本列表: 'Versions',
    '该脚本没有线上版本，无法执行': 'Failed! No online version available.',
    线上版: 'Online',
    版本管理: 'Versions',
    上传文件: 'Upload',
    版本对比: 'Diff',
    选择匹配的字段并输入关键字进行搜索: 'Select matching fields and enter keywords to search',
    更新时间: {
      label: 'Last modified at',
      colHead: 'Last modified at',
    },
    状态: 'Status',
    '确定上线该版本？': 'Are you sure ?',
    '上线后，之前的线上版本将被置为「已下线」状态，但不影响作业使用': 'After going online, the previous online version will be set to "offline", but it still can be execute.',
    上线: 'On',
    '确定禁用该版本？': 'Are you sure to ban it ?',
    '一旦禁用成功，不可恢复！且线上引用该版本的作业步骤都会无法执行，请务必谨慎操作！': 'Important! Once the version is banned, it cannot be restored! and the job steps that reference to this version will not be able to execute, so please be careful!',
    禁用: 'Ban',
    '确定删除该版本？': 'Are you sure to delete it ?',
    '删除后不可恢复，请谨慎操作！': 'Caution! It cannot be restored after deletion.',
    编辑: 'Edit',
    复制并新建: 'Copy new',
    描述: 'Desc',
    编辑脚本: 'Script editing',
    新建脚本: 'New script',
    查看脚本: 'View details',
    作业模板名称: 'Template name',
    步骤名称: 'Step name',
    已选: 'Selected',
    搜索作业模板: 'Search job template',
    '版本 ID': ' Version id',
    脚本名称: {
      label: 'Name',
      colHead: 'Name',
    },
    场景标签: {
      label: 'Tags',
      colHead: 'Tags',
    },
    被引用: {
      label: 'Related',
      colHead: 'Related',
    },
    创建人: {
      label: 'Created by',
      colHead: 'Created by',
    },
    版本号: {
      label: 'Version no.',
      colHead: 'Version no.',
    },
    更新人: {
      label: 'Last modified by',
      colHead: 'Last modified by',
    },
    '该脚本没有 “线上版本” 可执行，请前往版本管理内设置。': 'There is no "ON-LINE" version yet, go to "versions" page and set one.',
    已上线: 'ONLINE',
    未上线: 'STAND-BY',
    已下线: 'OFFLINE',
    已禁用: 'BANNED',
    '暂无关联作业，或已是当前版本。': 'No associated jobs, or it\'s already using current version.',
    '部分作业模板同步失败，请留意': 'Some of job template is sync failed',
    同步至: 'Update to',
    将覆盖其它条件: 'override other conditions',
    作业模板引用: 'Related by job template',
    执行方案引用: 'Related by job plan',
    引用脚本的作业模板: 'List of related job template',
    引用脚本的执行方案: 'List of related job plan',
    共: '',
    个: {
      result: 'totals',
    },
    请先选择作业模板步骤: 'Job template not selected yet',
    显示被执行方案引用的次数: 'Shows the number of times related by job template or plan',
    '是否需要进行执行方案同步？': 'Need to synchronize job plan ?',
    点击: 'Click',
    '“立即前往”': '"Go now"',
    '将进入关联的执行方案同步确认页面，点击': 'to enter the job plan synchronization confirmation page, ',
    '“暂时不用”': '"Later"',
    '结束本次操作并退出。': 'to end this operation and quit.',
    立即前往: 'Go now',
    暂时不用: 'Later',
    脚本内容检测失败: 'Script content detection failed',
    新建版本: 'New',
    确定: 'Submit',
    '当前已有 [未上线] 版本，': '「STAND-BY」version is exists',
    返回列表: 'go back to list',
    前往编辑: 'edit now',
    选择载入版本: 'Choose version to load',
    调试: 'Debug',
    标签: 'Tags',
    搜索结果为空: 'Search empty',
    清空搜索: 'Clear search',
    全部脚本: 'All',
    未分类: 'Non-tag',
    编辑标签: 'Multi-edit',
    范围: 'Total',
    个脚本: 'scripts selected',
    '勾选范围里，全部脚本使用': 'Applied in all selected scripts',
    '勾选范围里，有': '',
    个脚本使用: ' of the selected scripts applied it.',
    新建标签: 'New',
    编辑标签成功: 'New tag has been created',
    '已有[未上线]版本': '「STAND-BY」version is exists',
    新建版本成功: 'New script version has been created',
    标签名更新成功: 'Tag has been renamed',
    '标签名称：': 'Tag name: ',
    '标签描述：': 'Tag desc.: ',
  },
};
