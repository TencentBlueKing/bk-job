<template>
  <bk-user-selector
    v-if="!isLoading"
    v-model="selectedUsers"
    :api-base-url="apiBaseUrl"
    multiple
    :placeholder="placeholder"
    :tenant-id="tenantId"
    :user-group="roleList"
    :user-group-name="t('角色')"
    @change="handleChange" />
</template>
<script setup>
  import { computed, ref, shallowRef, watch } from 'vue';

  import NotifyService from '@service/notify';
  import QueryGlobalSettingService from '@service/query-global-setting';
  import UserService from '@service/user';

  import { makeMap } from '@utils/assist';

  import BkUserSelector from '@blueking/bk-user-selector/vue2';

  import { useI18n } from '@/i18n';

  import '@blueking/bk-user-selector/vue2/vue2.css';

  const props = defineProps({
    // 已选 user
    user: {
      type: Array,
      default: () => [],
    },
    // 已选 role
    role: {
      type: Array,
      default: () => [],
    },
    // 支持选择角色
    showRole: {
      type: Boolean,
      default: true,
    },
    // 排除待选的角色
    excludeRoleList: {
      type: Array,
      default: () => [],
    },
    placeholder: {
      type: String,
    },
  });

  const emits = defineEmits(['on-change']);


  const { t } = useI18n();

  const isLoading = ref(true);
  const tenantId = ref('');
  const apiBaseUrl = ref('');

  const selectedUsers = ref([]);

  const roleList = shallowRef([]);

  const roleIdMap = computed(() => roleList.value.reduce((result, item) => Object.assign(result, {
    [item.id]: item.name,
  }), {}));

  watch(() => props.showRole, () => {
    if (!props.showRole) {
      roleList.value = [];
      return;
    }
    NotifyService.fetchRoleList()
      .then((data) => {
        const excludeRoleMap = makeMap(props.excludeRoleList);
        roleList.value = data.reduce(item => ({
          id: item.code,
          name: item.name,
          hidden: excludeRoleMap[item.code],
        }));
      });
  }, {
    immediate: true,
  });

  watch(() => [props.user, props.role], () => {
    selectedUsers.value = [...props.role, ...props.user];
  }, {
    immediate: true,
  });

  Promise.all([
    QueryGlobalSettingService.fetchRelatedSystemUrls(),
    UserService.fetchUserInfo(),
  ]).then(([relatedSystemUrls, useInfo]) => {
    tenantId.value = useInfo.tenantId;
    apiBaseUrl.value = relatedSystemUrls.BK_USER_WEB_API_ROOT_URL;
  })
    .finally(() => {
      isLoading.value = false;
    });


  const handleChange = (valueList) => {
    const role = [];
    const user = [];
    valueList.forEach((item) => {
      if (roleIdMap.value[item]) {
        role.push(item);
      } else {
        user.push(item);
      }
    });

    emits('on-change', user, role);
  };
</script>
