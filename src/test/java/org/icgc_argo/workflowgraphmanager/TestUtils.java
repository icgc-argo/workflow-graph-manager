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

package org.icgc_argo.workflowgraphmanager;

import static java.lang.String.format;
import static org.icgc_argo.workflowgraphmanager.utils.JacksonUtils.readValue;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.val;
import org.icgc_argo.workflow_graph_lib.utils.JacksonUtils;

public class TestUtils {
  public static InputStream loadResource(String path) {
    return TestUtils.class.getResourceAsStream(
        format("/org/icgc_argo/workflowgraphmanager/%s", path));
  }

  public static void loadK8sWithBaseResourcesAnd(KubernetesClient client, String resourcePath) {

    // clear all pods
    client.pods().delete();

    val pods =
        ((List<Map<String, Object>>) readValue(loadResource(resourcePath), Map.class).get("items"))
            .stream().map(podJson -> JacksonUtils.convertValue(podJson, Pod.class));

    // load all configmaps
    List.of(
            "fixtures/configmaps/align-node-config.json",
            "fixtures/configmaps/align-node-two-config.json",
            "fixtures/configmaps/align-node-three-config.json")
        .forEach(
            configMapName -> {
              // load config map
              client.configMaps().create(readValue(loadResource(configMapName), ConfigMap.class));
            });

    // read decoy pods
    val decoyPodsJson = readValue(loadResource("fixtures/pods/decoy-pods.json"), Map.class);

    // Merge pods with decoy stream and load mock K8s
    Stream.concat(pods, ((List<Map<String, Object>>) decoyPodsJson.get("items")).stream())
        .map(podJson -> JacksonUtils.convertValue(podJson, Pod.class))
        .forEach(pod -> client.pods().create(pod));
  }
}
