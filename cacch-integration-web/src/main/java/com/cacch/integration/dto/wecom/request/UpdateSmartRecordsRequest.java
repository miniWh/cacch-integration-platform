package com.cacch.integration.dto.wecom.request;

import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComRecordWriteItem;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 更新智能表格记录请求
 * @author hongfu_zhou@cacch.com
 */
@Data
public class UpdateSmartRecordsRequest {

    @NotEmpty
    private List<RecordUpdateItem> records;

    @Data
    public static class RecordUpdateItem {

        private String recordId;

        private Map<String, Object> values;
    }

    public List<WeComRecordWriteItem> toWriteItems() {
        return records.stream()
                .map(item -> WeComRecordWriteItem.builder()
                        .recordId(item.getRecordId())
                        .values(item.getValues())
                        .build())
                .collect(Collectors.toList());
    }
}
