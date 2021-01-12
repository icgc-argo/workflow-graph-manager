package org.icgc_argo.workflowgraphmanager.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.icgc_argo.workflowgraphmanager.utils.JacksonUtils;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class GraphLog {

  private String id;

  private String graphMessageId;

  private String log;

  private String queue;

  private String node;

  private Pipeline pipeline;

  private Long timestamp;

  @SneakyThrows
  public static GraphLog parse(@NonNull Map<String, Object> sourceMap) {
    return JacksonUtils.parse(sourceMap, GraphLog.class);
  }
}
