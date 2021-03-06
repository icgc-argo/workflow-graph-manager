/*
 * Copyright (c) 2021 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc_argo.workflowgraphmanager.graphql.security;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.spring.web.reactive.GraphQLInvocation;
import graphql.spring.web.reactive.GraphQLInvocationData;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// See original code:
// https://github.com/graphql-java/graphql-java-spring/blob/v1.0/graphql-java-spring-webflux/src/main/java/graphql/spring/web/reactive/components/DefaultGraphQLInvocation.java
// This class is a work around for this issue:
// https://github.com/graphql-java/graphql-java-spring/issues/8
// The problem at a high level is that the graphql-java engine looses the ReactiveSecurityContext
// when it executes async.
// This work around, adds the security context to the graphql execution context so it's not lost and
// can be used for auth check.
// This only occurs if auth is enabled.

@Component
@Primary
@Slf4j
@Profile("secure")
public class SecurityContextAddedInvocation implements GraphQLInvocation {
  @Autowired GraphQL graphQL;

  @Override
  public Mono<ExecutionResult> invoke(
      GraphQLInvocationData invocationData, ServerWebExchange serverWebExchange) {
    ExecutionInput.Builder executionInputBuilder =
        ExecutionInput.newExecutionInput()
            .query(invocationData.getQuery())
            .operationName(invocationData.getOperationName())
            .variables(invocationData.getVariables());

    Mono<ExecutionInput> customizedExecutionInputMono =
        addReactiveSecurityContextToExecutionInput(executionInputBuilder);
    return customizedExecutionInputMono.flatMap(
        customizedExecutionInput ->
            Mono.fromCompletionStage(graphQL.executeAsync(customizedExecutionInput)));
  }

  @SneakyThrows
  public Mono<ExecutionInput> addReactiveSecurityContextToExecutionInput(
      ExecutionInput.Builder executionInputBuilder) {
    log.debug("Adding Reactive Security Context To Execution Input");
    Mono<SecurityContext> securityContextMono = ReactiveSecurityContextHolder.getContext();

    return securityContextMono
        .map(securityContext -> executionInputBuilder.context(securityContext).build())
        .switchIfEmpty(Mono.just(executionInputBuilder.build()));
  }
}
