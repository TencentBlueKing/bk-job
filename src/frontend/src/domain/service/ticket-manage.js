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

/* eslint-disable no-param-reassign */
import FileManageService from '@service/file-source-manage';

import TicketModel from '@model/ticket';

import TicketSource from '../source/ticket-manage';

export default {
  fetchList(params) {
    return TicketSource.getAll(params)
      .then(({ data }) => {
        data.data = data.data.map(ticket => new TicketModel(ticket));
        return data;
      });
  },
  fetchListWithRelate(params) {
    return TicketSource.getAll(params)
      .then(({ data }) => {
        data.data = data.data.map(ticket => new TicketModel(ticket));
        const ticketList = data.data;
        ticketList.forEach((ticket) => {
          FileManageService.fetchFileSourceList({
            credentialId: ticket.id,
            pageSize: 1,
          }).then((data) => {
            ticket.relatedNums = data.total;
            ticket.isRelatedLoading = false;
          });
        });
        return data;
      });
  },
  update(params) {
    return TicketSource.update(params);
  },
  remove(params) {
    return TicketSource.delete(params);
  },
};
