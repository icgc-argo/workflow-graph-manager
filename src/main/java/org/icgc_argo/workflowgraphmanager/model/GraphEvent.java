package org.icgc_argo.workflowgraphmanager.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import org.icgc_argo.workflowgraphmanager.model.base.Message;
import org.icgc_argo.workflowgraphmanager.utils.JacksonUtils;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class GraphEvent extends Message {
  private String analysisId;

  private String analysisState;

  private String analysisType;

  private String studyId;

  private String experimentalStrategy;

  private List<String> donorIds;

  private AnalysisFile files;

//  private String analysis; // todo: Make Analysis an entity

  @SneakyThrows
  public static GraphEvent parse(@NonNull Map<String, Object> sourceMap) {
    return JacksonUtils.parse(sourceMap, GraphEvent.class);
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  private static class AnalysisFile {
    private String dataType;
  }
}
