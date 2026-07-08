package com.cacch.integration.service.tencentmeeting.api.impl;

import com.cacch.integration.entity.tencentmeeting.QywxTxMeetingUserDO;
import com.cacch.integration.mapper.tencentmeeting.QywxTxMeetingUserMapper;
import com.cacch.integration.service.tencentmeeting.api.ITencentMeetingUserMappingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TencentMeetingUserMappingServiceImplTest {

    @Mock
    private QywxTxMeetingUserMapper qywxTxMeetingUserMapper;

    @InjectMocks
    private TencentMeetingUserMappingServiceImpl mappingService;

    @Test
    void resolveTxMeetingUserId_returnsMappedId() {
        QywxTxMeetingUserDO mapping = new QywxTxMeetingUserDO();
        mapping.setUserId("ZhouHongFu");
        mapping.setTxMeetingUserId("tx_user_001");
        when(qywxTxMeetingUserMapper.selectOne(any())).thenReturn(mapping);

        assertEquals("tx_user_001", mappingService.resolveTxMeetingUserId("ZhouHongFu"));
    }

    @Test
    void resolveTxMeetingUserId_nullWhenNotFound() {
        when(qywxTxMeetingUserMapper.selectOne(any())).thenReturn(null);

        assertNull(mappingService.resolveTxMeetingUserId("unknown"));
        assertNull(mappingService.resolveTxMeetingUserId(null));
        assertNull(mappingService.resolveTxMeetingUserId(" "));
    }
}
