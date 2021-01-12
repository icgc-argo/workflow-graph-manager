package org.icgc_argo.workflowgraphmanager.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.icgc_argo.workflowgraphmanager.model.base.Message;
import org.icgc_argo.workflowgraphmanager.utils.JacksonUtils;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class GraphRun extends Message {

  private String runId;

//  private String run; // todo: Make Run an entity

  @SneakyThrows
  public static GraphRun parse(@NonNull Map<String, Object> sourceMap) {
    return JacksonUtils.parse(sourceMap, GraphRun.class);
  }
}
