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

import BackupSource from '../source/backup';

export default {
  fetchInfo() {
    return BackupSource.getData()
      .then(({ data }) => ({
        importJob: data.importJob || [],
        exportJob: data.exportJob || [],
      }));
  },
  export(params) {
    return BackupSource.export(params)
      .then(({ data }) => data);
  },
  fetchExportInfo(params) {
    return BackupSource.getExportById(params)
      .then(({ data }) => data);
  },
  exportDelete(params) {
    return BackupSource.deleteExportById(params)
      .then(({ data }) => data);
  },
  updateExportComplete(params) {
    return BackupSource.exportComplete(params)
      .then(({ data }) => data);
  },
  import(params) {
    return BackupSource.import(params)
      .then(({ data }) => data);
  },
  fetchImportInfo(params) {
    return BackupSource.getImportById(params)
      .then(({ data }) => data);
  },
  checkImportPassword(params) {
    return BackupSource.checkImportPassword(params)
      .then(({ data }) => data);
  },
  uploadImportFile(params, payload) {
    return BackupSource.putImportFile(params, payload)
      .then(({ data }) => data);
  },
  fetchExportFile(params) {
    return BackupSource.getImportFile(params);
  },
};
