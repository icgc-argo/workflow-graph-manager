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

package org.icgc_argo.workflowgraphmanager.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.icgc_argo.workflowgraphmanager.repository.model.GraphNodeConfig;
import org.springframework.core.ParameterizedTypeReference;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Map;

// TODO: Move these into Graph-LIB
@Slf4j
public class JacksonUtils {
  protected static final ObjectMapper MAPPER = new ObjectMapper();

  public static <T> T parse(
      @NonNull Map<String, Object> sourceMap,
      @NonNull ParameterizedTypeReference<T> toValueParameterizedType) {
    return MAPPER.convertValue(
        sourceMap,
        new TypeReference<>() {
          public Type getType() {
            return toValueParameterizedType.getType();
          }
        });
  }

  public static <T> T parse(@NonNull Map<String, Object> sourceMap, Class<T> toValueType) {
    return MAPPER.convertValue(sourceMap, toValueType);
  }

  public static <T> T parse(@NonNull Object obj, @NonNull TypeReference<T> type) {
    return MAPPER.convertValue(obj, type);
  }

  @SneakyThrows
  public static <T> T readValue(InputStream src, Class<T> valueType) {
    return MAPPER.readValue(src, valueType);
  }

  @SneakyThrows
  public static GraphNodeConfig jsonStringToNodeConfig(String jsonString) {
    try {
      return MAPPER.readValue(jsonString, GraphNodeConfig.class);
    } catch (JsonProcessingException e) {
      log.error("Error parsing json to node config: ", e);
      throw e;
    }
  }
}
