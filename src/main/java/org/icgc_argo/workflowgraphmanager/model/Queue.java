package org.icgc_argo.workflowgraphmanager.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.icgc_argo.workflowgraphmanager.utils.JacksonUtils;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Queue {
  private String id;

  private String exchange;

  private Pipeline pipeline;

  private Node node;

  private List<String> messages;

  private List<GraphLog> logs;

  @SneakyThrows
  public static Queue parse(@NonNull Map<String, Object> sourceMap) {
    return JacksonUtils.parse(sourceMap, Queue.class);
  }
}
