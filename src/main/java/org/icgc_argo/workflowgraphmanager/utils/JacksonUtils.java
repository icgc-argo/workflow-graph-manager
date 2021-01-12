package org.icgc_argo.workflowgraphmanager.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import org.springframework.core.ParameterizedTypeReference;

import java.lang.reflect.Type;
import java.util.Map;

public class JacksonUtils {
  protected static final ObjectMapper MAPPER = new ObjectMapper();

  public static <T> T parse(
      @NonNull Map<String, Object> sourceMap, @NonNull ParameterizedTypeReference<T> typeRef) {
    return MAPPER.convertValue(
        sourceMap,
        new TypeReference<>() {
          public Type getType() {
            return typeRef.getType();
          }
        });
  }

  public static <T> T parse(@NonNull Map<String, Object> sourceMap, Class<T> clazz) {
      return MAPPER.convertValue(sourceMap, clazz);
  }
}
