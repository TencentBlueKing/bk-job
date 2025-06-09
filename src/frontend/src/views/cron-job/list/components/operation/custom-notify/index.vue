<template>
  <div class="custom-notify-box">
    <bk-form-item
      :label="$t('cron.消息通知')"
      layout="inline"
      required>
      <bk-radio-group
        :value="formData.notifyType"
        @change="handleNotifyTypeChange">
        <bk-radio :value="NOTIFY_TYPE_DEFAULT">
          {{ $t('cron.继承业务设置') }}
        </bk-radio>
        <bk-radio
          style="margin-left: 40px"
          :value="NOTIFY_TYPE_CUSTOM">
          {{ $t('cron.自定义') }}
        </bk-radio>
      </bk-radio-group>
    </bk-form-item>
    <template v-if="formData.notifyType === NOTIFY_TYPE_CUSTOM">
      <jb-form-item
        :label="$t('cron.通知对象')"
        layout="inline"
        property="customNotifyUser"
        required
        :rules="customNotifyUserRules">
        <jb-user-selector
          class="input"
          :filter-list="['JOB_EXTRA_OBSERVER']"
          :placeholder="$t('cron.输入通知对象')"
          :role="formData.customNotifyUser.roleList"
          :user="formData.customNotifyUser.userList"
          @on-change="handleApprovalUserChange" />
      </jb-form-item>
      <jb-form-item
        :label="$t('cron.通知方式')"
        layout="inline"
        property="customNotifyChannel"
        required
        :rules="customNotifyChannelRules"
        style="margin-bottom: 0;">
        <table class="notify-way-table input">
          <thead>
            <th style="width: 95px;">
              {{ $t('cron.状态') }}
            </th>
            <th>{{ $t('cron.通知方式') }}</th>
          </thead>
          <tbody>
            <tr
              v-for="channelItem in formData.customNotifyChannel"
              :key="channelItem.executeStatus">
              <td>{{ channelItem.executeStatus === 'SUCCESS' ? $t('cron.执行成功') : $t('cron.执行失败') }}</td>
              <td>
                <notify-channel
                  :value="channelItem.channelList"
                  @change="(value) => handleNotifyChannelChange(value, channelItem)" />
              </td>
            </tr>
          </tbody>
        </table>
      </jb-form-item>
    </template>
  </div>
</template>
<script setup>
  import _ from 'lodash';

  import JbUserSelector from '@components/jb-user-selector';

  import { useI18n } from '@/i18n';

  import NotifyChannel from './components/notify-channel.vue';

  const NOTIFY_TYPE_DEFAULT = 1;
  const NOTIFY_TYPE_CUSTOM = 2;

  const props = defineProps({
    formData: {
      type: Object,
      required: true,
    },
  });

  const emits = defineEmits(['on-change']);

  const { t } = useI18n();


  const customNotifyUserRules = [
    {
      validator: () => _.some(Object.values(props.formData.customNotifyUser), item => item.length > 0),
      message: t('cron.通知对象必填'),
      trigger: 'change',
    },
  ];

  const customNotifyChannelRules = [
    {
      validator: () => _.some(props.formData.customNotifyChannel, item => item.channelList.length > 0),
      message: t('cron.通知方式必填'),
      trigger: 'change',
    },
  ];
  const handleNotifyTypeChange = (value) => {
    emits('on-change', {
      notifyType: value,
    });
  };

  const handleApprovalUserChange = (userList, roleList) =>  {
    emits('on-change', {
      customNotifyUser: {
        userList,
        roleList,
      },
    });
  };
  const handleNotifyChannelChange = (value, channel) => {
    const notifyChannel = _.cloneDeep(props.formData.customNotifyChannel);
    const currentChannel = notifyChannel.find(item => item.executeStatus === channel.executeStatus);
    if (currentChannel) {
      currentChannel.channelList = value;
      emits('on-change', {
        customNotifyChannel: notifyChannel,
      });
    }
  };
</script>
<style lang='postcss'>
  .custom-notify-box{
    .notify-way-table {
      width: 100%;
      background: #fff;
      border: 1px solid #dcdee5;

      th,
      td {
        height: 42px;
        padding-left: 16px;
        font-size: 12px;
        text-align: left;
        border-left: 1px solid #dcdee5;
      }

      th {
        font-weight: normal;
        color: #313238;
        background: #fafbfd;
      }

      td {
        color: #63656e;
        border-top: 1px solid #dcdee5;
      }
    }
  }
</style>


