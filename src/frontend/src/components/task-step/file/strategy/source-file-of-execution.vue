<!--
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
-->

<template>
  <source-file
    :data="fileSources"
    :field="field"
    from="execute"
    mode="onlyHost"
    @on-change="handleSourceFileChange" />
</template>
<script>
  import SourceFileVO from '@domain/variable-object/source-file';

  import SourceFile from '../../common/source-file';

  export default {
    components: {
      SourceFile,
    },
    inheritAttrs: false,
    props: {
      field: {
        type: String,
        required: true,
      },
      formData: {
        type: Object,
        default: () => ({}),
      },
    },
    data() {
      return {
        fileSources: [],
      };
    },
    watch: {
      formData: {
        handler(formData) {
          this.fileSources = Object.freeze(formData[this.field].map((fileItem) => {
            const fileSource = new SourceFileVO(fileItem);
            fileSource.loaded = 1;
            fileSource.uploadStatus = 'success';
            return fileSource;
          }));
        },
        immediate: true,
      },
    },
    methods: {
      handleSourceFileChange(payload) {
        const originFileList = payload.map(fileItem => ({
          fileHash: fileItem.fileHash,
          fileLocation: fileItem.fileLocation,
          fileType: fileItem.fileType,
          fileSize: `${fileItem.fileSize}`,
          host: fileItem.host,
          account: fileItem.account,
          id: fileItem.id,
          fileSourceId: fileItem.fileSourceId,
        }));
        this.$emit('on-change', this.field, Object.freeze(originFileList));
      },
    },
  };
</script>
