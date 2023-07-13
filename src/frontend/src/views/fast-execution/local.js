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
  namespace: 'execution',
  message: {
    分发文件: 'File transfer',
    基本信息: 'Information',
    任务名称: 'Task name',
    '取一个便于记忆的任务名，方便后续在历史记录中快速定位...': 'Take a memorable task name for fast follow-up in historical records',
    上传限速: 'Upload speed limit',
    下载限速: 'Download speed limit',
    文件来源: 'Source',
    传输目标: 'Destination',
    执行: 'Launch',
    重置: 'Reset',
    最近结果: 'R.E',
    快速执行分发文件: 'File transfer',
    您有未保存的源文件: 'Source file not save yet...',
    继续执行: 'Continue',
    去保存: 'Save',
    脚本名称: 'Script name',
    快速执行脚本: 'Script execute',
    源文件可能出现同名: 'Source files may have same filename',
    '多文件源传输场景下容易出现同名文件覆盖的问题，你可以在目标路径中使用 [源服务器IP] 的变量来尽可能规避风险。': 'Files may have the risk of overwriting with the same name in multi-file source, using built-in variable like [FILESRCIP] in the Dst. path to avoid risks as much as possible.',
    '已知悉，确定执行': 'I\'m SURE, KEEP GOING',
    源和目标服务器相同: '',
    '检测到文件传输源和目标服务器是同一批，若是单台建议使用本地 cp 方式效率会更高，请问你是否确定参数无误？': 'The source and target hosts for file transfer are the same batch. If it is a single local transfers, using the "cp" command will be more efficient. Are you sure the parameters provided are correct?',
    '是的，确定无误': 'YES, I\'m SURE',
    '好的，我调整一下': 'NO, HOLD ON',
  },
};
