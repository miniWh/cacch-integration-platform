package com.cacch.integration.integration.tencentmeeting.adapter;

import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingSmartMinutesResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TencentMeetingSmartMinutesAdapterTest {

    @Test
    void resolveTodoSourceText_prefersTodoField() {
        TencentMeetingSmartMinutesResponse response = new TencentMeetingSmartMinutesResponse();
        TencentMeetingSmartMinutesResponse.MeetingMinute minute = new TencentMeetingSmartMinutesResponse.MeetingMinute();
        minute.setMinute("## 会议摘要\n摘要");
        minute.setTodo("## 会议待办\n- 帮助受灾群众");
        response.setMeetingMinute(minute);

        assertEquals("## 会议待办\n- 帮助受灾群众", TencentMeetingSmartMinutesAdapter.resolveTodoSourceText(response));
        assertEquals("## 会议摘要\n摘要", TencentMeetingSmartMinutesAdapter.resolveMinuteText(response));
    }

    @Test
    void resolveTodoSourceText_fallbackToMinuteWhenTodoEmpty() {
        TencentMeetingSmartMinutesResponse response = new TencentMeetingSmartMinutesResponse();
        TencentMeetingSmartMinutesResponse.MeetingMinute minute = new TencentMeetingSmartMinutesResponse.MeetingMinute();
        minute.setMinute("## 会议待办\n- 展示了中国的军事实力");
        response.setMeetingMinute(minute);

        assertEquals("## 会议待办\n- 展示了中国的军事实力",
                TencentMeetingSmartMinutesAdapter.resolveTodoSourceText(response));
    }

    @Test
    void resolveTodoSourceText_nullWhenEmpty() {
        assertNull(TencentMeetingSmartMinutesAdapter.resolveTodoSourceText(null));
        assertNull(TencentMeetingSmartMinutesAdapter.resolveMinuteText(new TencentMeetingSmartMinutesResponse()));
    }
}
