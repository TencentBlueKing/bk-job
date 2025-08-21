package com.tencent.bk.job.manage.service.impl.notify;

import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.common.paas.cmsi.ICmsiClient;
import com.tencent.bk.job.common.paas.model.NotifyChannelDTO;
import com.tencent.bk.job.common.paas.model.OpenApiTenant;
import com.tencent.bk.job.common.paas.user.IUserApiClient;
import com.tencent.bk.job.manage.dao.notify.AvailableEsbChannelDAO;
import com.tencent.bk.job.manage.model.dto.notify.AvailableEsbChannelDTO;
import com.tencent.bk.job.manage.service.globalsetting.GlobalSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class NotifyChannelInitService {

    private final IUserApiClient userMgrApiClient;
    private final ICmsiClient cmsiApiClient;
    private final GlobalSettingsService globalSettingsService;
    private final AvailableEsbChannelDAO availableEsbChannelDAO;

    @Value("${job.manage.notify.default.channels.available:mail,weixin,rtx}")
    private final String defaultAvailableNotifyChannelsStr = "mail,weixin,rtx";

    @Autowired
    public NotifyChannelInitService(IUserApiClient userMgrApiClient,
                                    ICmsiClient cmsiApiClient,
                                    GlobalSettingsService globalSettingsService,
                                    AvailableEsbChannelDAO availableEsbChannelDAO) {
        this.userMgrApiClient = userMgrApiClient;
        this.cmsiApiClient = cmsiApiClient;
        this.globalSettingsService = globalSettingsService;
        this.availableEsbChannelDAO = availableEsbChannelDAO;
    }

    @JobTransactional(transactionManager = "jobManageTransactionManager")
    public void initAllTenantDefaultNotifyChannels() {
        log.info("init default notify channels");
        List<OpenApiTenant> tenantList = userMgrApiClient.listAllTenant();
        tenantList.forEach(tenant ->
            tryToInitDefaultNotifyChannelsWithSingleTenant(tenant.getId())
        );
    }

    @JobTransactional(transactionManager = "jobManageTransactionManager")
    public void tryToInitDefaultNotifyChannelsWithSingleTenant(String tenantId) {
        if (globalSettingsService.isNotifyChannelConfiged(tenantId)) {
            // 当前租户已初始化过消息通知渠道，无需再配置
            return;
        }

        List<NotifyChannelDTO> notifyChannelDTOList = cmsiApiClient.getNotifyChannelList(tenantId);
        if (notifyChannelDTOList == null) {
            log.error("Fail to get tenant: {} notify channels from esb, null", tenantId);
            return;
        }
        saveDefaultNotifyChannelsToDb(tenantId, notifyChannelDTOList);
    }

    @JobTransactional(transactionManager = "jobManageTransactionManager")
    public void saveDefaultNotifyChannelsToDb(String tenantId, List<NotifyChannelDTO> notifyChannelDTOList) {
        globalSettingsService.setNotifyChannelConfiged(tenantId);
        availableEsbChannelDAO.deleteAllChannelsByTenantId(tenantId);
        for (NotifyChannelDTO notifyChannelDTO : notifyChannelDTOList) {
            // 租户内通知渠道不可用
            if (!notifyChannelDTO.isEnabled()) {
                continue;
            }
            Set<String> defaultAvailableChannelCodeSet = new HashSet<>(
                Arrays.asList(defaultAvailableNotifyChannelsStr.split(",")));

            if (!defaultAvailableChannelCodeSet.contains(notifyChannelDTO.getType())) {
                continue;
            }

            availableEsbChannelDAO.insertAvailableEsbChannel(
                new AvailableEsbChannelDTO(
                    notifyChannelDTO.getType(),
                    true,
                    "admin",
                    LocalDateTime.now(),
                    tenantId
                )
            );
        }
    }
}
