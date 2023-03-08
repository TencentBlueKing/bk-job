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
  <div>
    <plan-list ref="list" />
    <element-teleport v-if="templateName">
      <div style="padding-left: 10px; font-size: 12px; color: #63656e;">
        （{{ templateName }}）
      </div>
    </element-teleport>
  </div>
</template>
<script>
  import TaskManageService from '@service/task-manage';

  import PlanList from '../common/plan/list';

  export default {
    name: '',
    components: {
      PlanList,
    },
    data() {
      return {
        templateName: '',
      };
    },
    computed: {
      isSkeletonLoading() {
        return this.$refs.list.isLoading;
      },
    },
    created() {
      this.templateId = this.$route.params.templateId;
      this.fetchData();
    },
    methods: {
      /**
       * @desc 获取作业模板名
       */
      fetchData() {
        TaskManageService.taskDetail({
          id: this.templateId,
        }, {
          permission: 'page',
        }).then((data) => {
          this.templateName = data.name;
        });
      },
      /**
       * @desc 路由 回退
       */
      routerBack() {
        const { from } = this.$route.query;
        if (from === 'settingVar') {
          this.$router.push({
            name: 'settingVariable',
            params: {
              templateId: this.templateId,
              id: this.$route.query.active,
            },
          });
        } else if (from === 'debugPlan') {
          this.$router.push({
            name: 'debugPlan',
            params: {
              id: this.templateId,
            },
          });
        } else if (from === 'cronJob') {
          this.$router.push({
            name: 'cronList',
          });
        } else if (from === 'historyTask') {
          this.$router.push({
            name: 'historyTask',
            params: {
              id: this.$route.query.taskInstanceId,
            },
          });
        } else if (from === 'planList') {
          this.$router.push({
            name: 'planList',
          });
        } else {
          this.$router.push({
            name: 'templateDetail',
            params: {
              id: this.templateId,
            },
          });
        }
      },
    },
  };
</script>
