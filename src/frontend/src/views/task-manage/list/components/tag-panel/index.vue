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
  <div
    v-bkloading="{ isLoading }"
    class="template-list-tag-panel">
    <tab-item
      :id="1"
      :count="totalCount"
      icon="business-manage"
      :name="$t('template.全部作业')"
      tooltips-disabled
      :value="classesId"
      @on-select="handleClassesSelect" />
    <tab-item
      :id="2"
      :count="unclassifiedCount"
      icon="unclassified"
      :name="$t('template.未分类')"
      tooltips-disabled
      :value="classesId"
      @on-select="handleClassesSelect" />
    <tab-item
      :id="3"
      :count="needUpdateCount"
      icon="update"
      :name="$t('template.待更新')"
      tooltips-disabled
      :value="classesId"
      @on-select="handleClassesSelect" />
    <div class="line" />
    <template v-for="item in list">
      <tab-item
        v-if="item.relatedTaskTemplateNum > 0"
        :id="item.id"
        :key="item.id"
        can-edit
        :count="item.relatedTaskTemplateNum"
        :description="item.description"
        :name="item.name"
        :tag-list="list"
        :value="tagId"
        @on-edit="handleEdit"
        @on-select="handleSelect" />
    </template>
  </div>
</template>
<script>
  import TagManageService from '@service/tag-manage';
  import TaskManageService from '@service/task-manage';

  import TabItem from './tab-item';

  import I18n from '@/i18n';

  export default {
    name: 'RenderTagTabItem',
    components: {
      TabItem,
    },
    props: {
      active: {
        type: [
          String,
          Number,
        ],
        default: '',
      },
    },
    data() {
      return {
        isLoading: false,
        classesId: 1,
        tagId: 0,
        list: [],
        countMap: {},
        isNumberLoading: false,
      };
    },
    computed: {
      totalCount() {
        return this.countMap.total || 0;
      },
      unclassifiedCount() {
        return this.countMap.unclassified || 0;
      },
      needUpdateCount() {
        return this.countMap.needUpdate || 0;
      },
    },
    created() {
      this.init();
    },
    mounted() {
      this.parseDefaultValueFromURL();
    },
    methods: {
      /**
       * @desc 获取tag列表
       */
      fetchTagList() {
        return TagManageService.fetchWholeList();
      },
      /**
       * @desc 获取tag的使用数量
       */
      fetchTagTemplateNum() {
        return TaskManageService.fetchTagCount();
      },
      init() {
        this.isLoading = true;
        Promise.all([
          this.fetchTagList(),
          this.fetchTagTemplateNum(),
        ]).then(([tagList, countMap]) => {
          this.countMap = Object.freeze(countMap);
          const list = [];
          tagList.forEach((tag) => {
            tag.relatedTaskTemplateNum = countMap.tagCount[tag.id] || 0;
            list.push(tag);
          });
          this.list = Object.freeze(list);
          this.$emit('on-init', list);
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
      /**
       * @desc 解析url中的默认tag
       */
      parseDefaultValueFromURL() {
        let classesId = 1;
        if (this.$route.query.type) {
          classesId = ~~this.$route.query.type || 1;
          this.handleClassesSelect(classesId);
          return;
        }

        if (this.$route.query.panelTag) {
          const currentTagId = parseInt(this.$route.query.panelTag, 10);
          if (currentTagId > 0) {
            this.classesId = 0;
            this.handleSelect(currentTagId);
          }
        }
      },
      /**
       * @desc 分类切换
       * @param {Number} id 分类id
       */
      handleClassesSelect(id) {
        if (this.classesId === id) {
          return;
        }

        this.classesId = id;
        this.tagId = 0;
        this.$emit('on-change', {
          type: this.classesId,
          panelTag: '',
        });
      },
      /**
       * @desc tag切换
       * @param {Number} id 分类id
       */
      handleSelect(id) {
        if (id === this.tagId) return;
        this.tagId = id;
        this.classesId = 0;
        this.$emit('on-change', {
          type: '',
          panelTag: this.tagId,
        });
      },
      /**
       * @desc 编辑tag
       * @param {Object} payload 标签数据
       *
       * 编辑成功需要刷新标签数据
       */
      handleEdit(payload) {
        TagManageService.updateTag(payload)
          .then(() => {
            this.messageSuccess(I18n.t('template.标签名更新成功'));
            this.fetchTagList();
          });
      },
    },
  };
</script>
<style lang='postcss' scoped>
  .template-list-tag-panel {
    display: flex;
    flex-direction: column;
    min-height: 50%;
    padding: 24px 0;

    .line {
      height: 1px;
      margin: 10px 0;
      background: #f0f1f5;
    }
  }
</style>
