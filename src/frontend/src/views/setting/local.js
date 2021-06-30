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
    namespace: 'setting',
    message: {
        通知设置: 'Notification',
        存储策略: 'History Setting',
        账号命名规则: 'Naming Rules',
        高危语句规则: 'High-risk Grammar',
        
        保存: 'Save',
        取消: 'Cancel',
        请填写完整的语法检测表达式和说明: 'Grammar Regex and Description not defined.',
        新增成功: 'New rule has been created',
        请输入命名规则: 'Please enter the rule expressions...',
        请输入命名规则提醒文案: 'Please enter the rule explanation...',
        恢复默认: 'Initialize',
        重置: 'Reset',
        账号命名规则保存成功: 'Naming Rules change has been saved.',
        用户可选择的通知渠道: 'User-selectable notification type',
        通讯黑名单: 'Ban List',
        '「通讯黑名单」的人员将不会接收到任何来自作业平台的消息': 'The user in ban-list will not able to select.',
        保存通知渠道失败: 'Saving notification type failed...',
        保存黑名单失败: 'Saving Ban-list failed...',
        保存成功: 'The change has been saved.',
        执行历史保留: 'History data will save',
        天: 'Days',
        保留天数必须大于0: 'Must be greater than 0',
        平台信息: 'Platform Information',
        '网页 Title 设置:': 'Web Title Setting:',
        平台名称: 'Platform Name',
        分隔符: 'Delimiter',
        作业平台: 'BlueKing JOB',
        平台名称必填: 'Platform name is required',
        分隔符必填: 'Delimiter is required',
        页脚信息设置: 'Footer Settings',
        联系方式: 'Contact Info.',
        版权信息: 'Copyright Info.',
        渠道: 'CHANNEL',
        模板: 'TEMPLATE',
        人工确认: 'Confirmation',
        执行成功: 'Success',
        执行失败: 'Failed',
        '定时任务-执行前': 'Cron - Before Launch',
        '定时任务-结束前': 'Cron - Before Ended',
        编辑模板: 'Edit',
        未设置: 'Unset',
        消息模板编辑: 'Message Template Edit',
        初始化: 'Initialize',
        '渠道类型：': 'Channel:  ',
        '消息类型：': 'Type:  ',
        '最近修改人：': 'Last Modified By:  ',
        '最近修改时间：': 'Last Modified On:  ',
        模板内容: 'Template Contents',
        模板内容必填: 'Template Contents is required',
        内置变量: 'Built-in Variables',
        邮件主题: 'Mail Subject',
        邮件主题必填: 'email subject is required',
        消息预览: 'Message Preview',
        发送: 'Send',
        发送成功: 'Message has been sent',
        '请输入接收消息预览的用户名（请确保接受人对应的账号配置正常）': 'Please type the names of the receivers...',
        内置变量列表: 'Built-in Variables List',
        通用变量: 'Common',
        作业变量: 'Job',
        定时任务变量: 'Cron',
        变量名称: 'Var Name',
        含义: 'Description',
        示例: 'Example',
        任务的ID: 'ID of the task',
        任务的名称: 'Name of the task',
        获取所有主机的设备型号: 'This is a deploy job plan...',
        '任务所属业务的 ID': 'Business ID of the task',
        任务所属业务的名称: 'Business Name of the task',
        王者荣耀: 'Twitter Project',
        任务的操作人: 'Operator of the task',
        任务开始时间: 'The task\'s launch time',
        任务的类型: 'Type of the task',
        '快速执行脚本 / 作业任务 / 定时任务 / ...': 'Job / Cron / ...',
        任务的详情链接: 'Access url of the task',
        任务当前步骤的名称: 'Name of the current step',
        这是检查配置文件语法的步骤: 'This step is plan to do...',
        任务当前步骤的类型: 'Type of the current step',
        '脚本执行 or 文件分发 or 人工确认': 'Script Execution  / Confirmation / ...',
        任务的步骤总数: 'Total step counts of the task',
        任务当前步骤的序号: 'Step number of the task',
        任务当前步骤执行耗时: 'Duration of the current step',
        任务执行总耗时: 'Total duration of the task',
        任务当前步骤执行失败数: 'Failed host count of current step',
        任务当前步骤执行成功数: 'Success host count of current step',
        任务的人工确认描述: 'Description of confirmation step',
        任务的确认步骤的干系人: 'Confirmer of confirmation step',
        请确认以上执行步骤是否执行正常: 'Make sure everything is okay...',
        定时关联的执行方案名: 'Job plan name of the Cron',
        定时关联的执行方案ID: 'Job plan ID of the Cron',
        这是一个测试的作业执行方案: 'This is a Cron job plan...',
        定时任务的执行策略: 'Repeat Frequency of the cron',
        '单次执行 or 周期执行': 'Run once or Round-robin',
        定时任务的执行时间: 'Launch time of the cron',
        定时任务提前通知时间: 'Time of notify before launch',
        请输入: 'Please Input',
        文件上传设置: 'Local File Update',
        本地文件上传大小限制: 'File Size Limit',
    },
};
