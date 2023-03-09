@Library(value='jenkins-pipeline-library@master', changelog=false) _
pipelineRDPCWorkflowGraphManager(
    buildImage: "adoptopenjdk/openjdk11:jdk-11.0.7_10-alpine-slim",
    dockerRegistry: "ghcr.io",
    dockerRepo: "icgc-argo/workflow-graph-manager",
    gitRepo: "icgc-argo/workflow-graph-manager",
    testCommand: "./mvnw test -ntp",
    helmRelease: "workflow-graph-manager"
)
