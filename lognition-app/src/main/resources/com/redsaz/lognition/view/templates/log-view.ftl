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
        <script src="${dist}/js/app.js"></script>
        <h2>${brief.name}</h2>
        <div>
          <#noescape>${notesHtml}</#noescape>
        </div>
        <div>
        <#list labels as l>
          <span>${l}</span>
        </#list>
        </div>

        <div class="pure-menu pure-menu-horizontal">
            <ul class="pure-menu-list">
                <li class="pure-menu-item pure-menu-selected"><a href="#summary" class="pure-menu-link">Summary</a></li>
                <li class="pure-menu-item"><a href="#responses" class="pure-menu-link">Responses</a></li>
                <li class="pure-menu-item"><a href="#histograms" class="pure-menu-link">Histograms</a></li>
                <li class="pure-menu-item"><a href="#percentiles" class="pure-menu-link">Percentiles</a></li>
                <li class="pure-menu-item"><a href="#timeseries" class="pure-menu-link">Timeseries</a></li>
                <li class="pure-menu-item"><a href="#responses-timeseries" class="pure-menu-link">Responses Timeseries</a></li>
                <li class="pure-menu-item"><a href="#error-timeseries" class="pure-menu-link">Error Timeseries</a></li>
                <li class="pure-menu-item"><a href="#error-percent-timeseries" class="pure-menu-link">Error% Timeseries</a></li>
            </ul>
        </div>

        <div id="summary" class="content-pane">
            <table class="pure-table pure-table-striped">
                <thead>
                <tr>
                    <th class="left-align">Label</th>
                    <th class="right-align"># Samples</th>
                    <th class="right-align">Min</th>
                    <th class="right-align">25% line</th>
                    <th class="right-align">50% line</th>
                    <th class="right-align">75% line</th>
                    <th class="right-align">90% line</th>
                    <th class="right-align">95% line</th>
                    <th class="right-align">99% line</th>
                    <th class="right-align">Max</th>
                    <th class="right-align">Average</th>
                    <th class="right-align">Total Response Bytes</th>
                    <th class="right-align"># Errors</th>
                    <th class="right-align">Error%</th>
                </tr>
                </thead>
                <tbody>
            <#list aggregates as a>
                <tr>
                    <th class="left-align">${sampleLabels[a?index]}</th>
                    <td class="right-align">${a.numSamples}</td>
                    <td class="right-align">${a.min}</td>
                    <td class="right-align">${a.p25}</td>
                    <td class="right-align">${a.p50}</td>
                    <td class="right-align">${a.p75}</td>
                    <td class="right-align">${a.p90}</td>
                    <td class="right-align">${a.p95}</td>
                    <td class="right-align">${a.p99}</td>
                    <td class="right-align">${a.max}</td>
                    <td class="right-align">${a.avg}</td>
                    <td class="right-align">${a.totalResponseBytes}</td>
                    <td class="right-align">${a.numErrors}</td>
                    <td class="right-align">${a.errorRatio?string["0.00%;; multiplier=100"]}</td>
                </tr>
            </#list>
                </tbody>
            </table>
        </div>

        <div id="responses" class="content-pane">
<#if (aggregateCodes)??>
            <table class="pure-table pure-table-striped">
              <thead>
              <tr>
                <th class="left-align">Label</th>
<#list aggregateCodes as code>
                <th class="right-align">${code}</th>
</#list>
              </tr>
              </thead>
              <tbody>
<#list aggregateCodeCounts as a>
              <tr>
                <th class="left-align">${sampleLabels[a?index]}</th>
<#list a.getCounts()[0] as codeCount>
                <td class="right-align">${codeCount}</td>
</#list>
              </tr>
</#list>
              </tbody>
            </table>
<#else>
            Response statistics were not collected for this log.
</#if>
        </div>

        <div id="histograms" class="content-pane">
          <#list histogramGraphs as h>
          <div class="row">
            <div class="col-sm-12 col-md-12">
              <div class="graph loggraph">
                <div id="histogramgraphdiv${h?index}" style="width: 100%"></div>
              </div>
            </div>
          </div>
          </#list>
        </div>

        <div id="percentiles" class="content-pane">
          <#list percentileGraphs as p>
          <div class="row">
            <div class="col-sm-12 col-md-12">
              <div class="graph loggraph">
                <div id="percentilegraphdiv${p?index}" style="width: 100%"></div>
              </div>
            </div>
          </div>
          </#list>
        </div>

        <div id="timeseries" class="content-pane">
          <#list graphs as graph>
          <div class="row">
            <div class="col-sm-12 col-md-12">
              <div class="graph loggraph">
                <div id="graphdiv${graph?index}" style="width: 100%"></div>
              </div>
            </div>
          </div>
          </#list>
        </div>

        <div id="responses-timeseries" class="content-pane">
          <#list timeseriesCodeCountsGraphs as graph>
          <div class="row">
            <div class="col-sm-12 col-md-12">
              <div class="graph loggraph">
                <div id="codecountsgraphdiv${graph?index}" style="width: 100%"></div>
              </div>
            </div>
          </div>
          <#else>
          <div class="container">
            Response timeseries were not collected for this log.
          </div>
          </#list>
        </div>

        <div id="error-timeseries" class="content-pane">
          <#list errorTimeseriesGraphs as etg>
          <div class="row">
            <div class="col-sm-12 col-md-12">
              <div class="graph loggraph">
                <div id="errorTimeseriesdiv${etg?index}" style="width: 100%"></div>
              </div>
            </div>
          </div>
          </#list>
        </div>

        <div id="error-percent-timeseries" class="content-pane">
          <#list errorPercentTimeseriesGraphs as eptg>
          <div class="row">
            <div class="col-sm-12 col-md-12">
              <div class="graph loggraph">
                <div id="errorPercentTimeseriesdiv${eptg?index}" style="width: 100%"></div>
              </div>
            </div>
          </div>
          </#list>
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
        <#list timeseriesCodeCountsGraphs as graph>
          <script>
            <#noescape>${graph}</#noescape>
          </script>
        </#list>
        <#list errorTimeseriesGraphs as etg>
          <script>
            <#noescape>${etg}</#noescape>
          </script>
        </#list>
        <#list errorPercentTimeseriesGraphs as eptg>
          <script>
            <#noescape>${eptg}</#noescape>
          </script>
        </#list>
</#escape>
