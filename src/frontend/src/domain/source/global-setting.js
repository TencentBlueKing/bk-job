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

import Request from '@utils/request';

class GlobalSetting {
  constructor() {
    this.module = '/job-manage/web/globalSettings';
  }

  // 获取账号命名规则
  getAllNameRule(params, payload = {}) {
    return Request.get(`${this.module}/account/nameRules`, {
      payload,
    });
  }

  // 设置账号命名规则
  updateNameRules(params = {}) {
    return Request.post(`${this.module}/account/setNameRules`, {
      params,
    });
  }

  // 获取执行历史保留时间
  getHistroyExpire(params, payload = {}) {
    return Request.get(`${this.module}/history/expireTime`, {
      payload,
    });
  }

  // 设置执行历史保留时间
  updateHistroyExpire(params = {}) {
    return Request.post(`${this.module}/history/expireTime`, {
      params,
    });
  }

  // 获取通知渠道列表及生效状态
  getAllNotifyChannel(params, payload = {}) {
    return Request.get(`${this.module}/notify/listChannels`, {
      payload,
    });
  }

  // 超级管理员设置启用的通知渠道
  updateNotifyChannel(params = {}) {
    return Request.post(`${this.module}/notify/setAvailableChannels`, {
      params,
    });
  }

  // 获取现有通知黑名单用户列表
  getAllUserBlacklist(params, payload = {}) {
    return Request.get(`${this.module}/notify/users/blacklist`, {
      payload,
    });
  }

  // 设置通知黑名单
  updateUserBlacklist(params = {}, payload = {}) {
    return Request.post(`${this.module}/notify/users/blacklist`, {
      params,
      payload,
    });
  }

  // 根据用户英文名前缀拉取用户列表
  getUserByName(params = {}) {
    return Request.get(`${this.module}/users/list`, {
      params,
    });
  }

  // 修改平台信息
  updateTitleAndFooterConfig(params = {}) {
    return Request.post(`${this.module}/titleFooter`, {
      params,
    });
  }

  // 获取带默认值的Title与Footer
  getTitleAndFooterWithDefault(params, payload = {}) {
    return Request.get(`${this.module}/titleFooterWithDefault`, {
      payload,
    });
  }

  // 查询各渠道消息模板配置状态
  getAllNotifyChannelConfig(params, payload = {}) {
    return Request.get(`${this.module}/notify/channelTemplate/configStatus`, {
      payload,
    });
  }

  // 消息模板详情
  getChannelTemplate(params = {}, payload = {}) {
    return Request.get(`${this.module}/notify/channelTemplate/detail`, {
      params,
      payload,
    });
  }

  // 保存消息模板
  updateNotifyTemplate(params = {}) {
    return Request.post(`${this.module}/notify/channelTemplate`, {
      params,
    });
  }

  // 消息发送预览
  sendNotifyPreview(params = {}) {
    return Request.post(`${this.module}/notify/channelTemplate/send`, {
      params,
    });
  }

  // 获取文件上传设置
  getFileUpload(params = {}) {
    return Request.get(`${this.module}/file/upload`, {
      params,
    });
  }

  // 设置文件上传设置
  saveFileUpload(params = {}) {
    return Request.post(`${this.module}/file/upload`, {
      params,
    });
  }
}

export default new GlobalSetting();
