<!--
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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
  <div v-if="isRender">
    <jb-form-item
      v-for="paramsItem in paramList"
      :key="paramsItem.name"
      :label="paramsItem.label"
      property="fileSourceInfoMap"
      :required="paramsItem.required"
      :rules="calcRules(paramsItem)">
      <bk-input
        :value="paramMap[paramsItem.name]"
        @change="value => handleChange(paramsItem.name, value)" />
    </jb-form-item>
  </div>
</template>
<script>
  import FileManageService from '@service/file-source-manage';

  export default {
    name: '',
    props: {
      fileSourceTypeCode: {
        type: String,
      },
      paramMap: {
        type: Object,
        required: true,
      },
    },
    data() {
      return {
        paramList: [],
      };
    },
    computed: {
      isRender() {
        return this.fileSourceTypeCode && this.paramList.length > 0;
      },
    },
    watch: {
      fileSourceTypeCode: {
        handler(fileSourceTypeCode) {
          if (fileSourceTypeCode) {
            this.fetchFileSourceParams();
          }
        },
        immediate: true,
      },
    },
    methods: {
      /**
       * @desc 获取文件来源参数
       *
       * paramMap 为空使用默认值
       */
      fetchFileSourceParams() {
        FileManageService.fetchParams({
          fileSourceTypeCode: this.fileSourceTypeCode,
        }).then((data) => {
          this.paramList = Object.freeze(data);
          if (Object.keys(this.paramMap).length < 1) {
            const paramMap = data.reduce((result, item) => {
              result[item.name] = item.default;
              return result;
            }, {});
            this.$emit('on-change', paramMap);
          }
        });
      },
      /**
       * @desc 验证规则
       * @param {Object} formItem 表单项数据
       */
      calcRules(formItem) {
        const rules = [];
        if (formItem.required) {
          rules.push({
            validator: fileSourceInfoMap => !!fileSourceInfoMap[formItem.name],
            message: `${formItem.label}必填`,
            trigger: 'blur',
          });
        }
        return rules;
      },
      /**
       * @desc 编辑文件来源参数
       * @param {String} name 参数名
       * @param {String} value 参数值
       */
      handleChange(name, value) {
        this.$emit('on-change', {
          ...this.paramMap,
          [name]: value,
        });
      },
    },
  };
</script>
