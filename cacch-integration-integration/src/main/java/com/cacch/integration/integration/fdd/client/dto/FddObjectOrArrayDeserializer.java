package com.cacch.integration.integration.fdd.client.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 兼容法大大 {@code data} 字段既可能是对象也可能是数组的反序列化器
 *
 * @param <T> 元素类型
 * @author hongfu_zhou@cacch.com
 */
public class FddObjectOrArrayDeserializer<T> extends StdDeserializer<List<T>> implements ContextualDeserializer {

    private final JavaType elementType;

    /**
     * 无参构造（Jackson 反射用）
     */
    public FddObjectOrArrayDeserializer() {
        this(null);
    }

    private FddObjectOrArrayDeserializer(JavaType elementType) {
        super(List.class);
        this.elementType = elementType;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        JavaType wrapper = property.getType();
        JavaType content = wrapper.containedType(0);
        return new FddObjectOrArrayDeserializer<>(content);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (node == null || node.isNull()) {
            return Collections.emptyList();
        }
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        if (node.isArray()) {
            List<T> list = new ArrayList<>(node.size());
            for (JsonNode item : node) {
                list.add((T) mapper.convertValue(item, elementType));
            }
            return list;
        }
        if (node.isObject()) {
            T single = (T) mapper.convertValue(node, elementType);
            return single == null ? Collections.emptyList() : List.of(single);
        }
        return Collections.emptyList();
    }
}
