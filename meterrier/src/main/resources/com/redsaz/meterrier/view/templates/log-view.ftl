<#--
 Copyright 2017 Redsaz <redsaz@gmail.com>.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
<#escape x as x?html>
      <div class="row">
        <div class="col-sm-12 col-md-12 main">
          <h2>${brief.id}</h2>
          <p>${brief}</p>
          <table class="table table-hover">
            <thead>
            <tr>
              <th>Label</th>
              <th># Samples</th>
              <th>Min</th>
              <th>25% line</th>
              <th>50% line</th>
              <th>75% line</th>
              <th>90% line</th>
              <th>95% line</th>
              <th>99% line</th>
              <th>Max</th>
              <th>Average</th>
              <th>Total Response Bytes</th>
              <th># Errors</th>
            </tr>
            </thead>
            <tbody>
          <#list aggregates as a>
            <tr>
              <th>${sampleLabels[a?index]}</th>
              <td>${a.numSamples}</td>
              <td>${a.min}</td>
              <td>${a.p25}</td>
              <td>${a.p50}</td>
              <td>${a.p75}</td>
              <td>${a.p90}</td>
              <td>${a.p95}</td>
              <td>${a.p99}</td>
              <td>${a.max}</td>
              <td>${a.avg}</td>
              <td>${a.totalResponseBytes}</td>
              <td>${a.numErrors}</td>
            </tr>
          </#list>
            </tbody>
          </table>
          <#list histogramGraphs as h>
            <div id="histogramgraphdiv${h?index}"></div>
          </#list>
          <#list percentileGraphs as p>
            <div id="percentilegraphdiv${p?index}"></div>
          </#list>
          <#list graphs as graph>
            <div id="graphdiv${graph?index}"></div>
          </#list>
        </div>
      </div>

      <script src="${dist}/js/dygraph.min.js"></script>
      <#list histogramGraphs as h>
        <script>
          <#noescape>${h}</#noescape>
        </script>
      </#list>
      <#list percentileGraphs as p>
        <script>
          <#noescape>${p}</#noescape>
        </script>
      </#list>
      <#list graphs as graph>
        <script>
          <#noescape>${graph}</#noescape>
        </script>
      </#list>
</#escape>
