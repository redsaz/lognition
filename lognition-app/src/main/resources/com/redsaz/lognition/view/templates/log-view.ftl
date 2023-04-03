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
<div class="container">

      <div class="row">
        <div class="col-sm-12 col-md-12 main">
          <h2>${brief.name}</h2>
          <div>
            <#noescape>${notesHtml}</#noescape>
          </div>
          <div class="row">
            <div class="col-sm-12 col-md-12">
            <#list labels as l>
              <span>${l}</span>
            </#list>
            </div>
          </div>
        </div>
      </div>

      <div class="row">
      <ul class="nav nav-tabs" id="myTab" role="tablist">
        <li class="nav-item">
          <a class="nav-link active" id="summary-tab" data-toggle="tab" href="#summary" role="tab" aria-controls="summary" aria-selected="true" onclick="switchActiveNav('nav-link', this)">Summary</a>
        </li>
        <li class="nav-item">
          <a class="nav-link" id="responses-tab" data-toggle="tab" href="#responses" role="tab" aria-controls="responses" aria-selected="false" onclick="switchActiveNav('nav-link', this)">Responses</a>
        </li>
        <li class="nav-item">
          <a class="nav-link" id="histograms-tab" data-toggle="tab" href="#histograms" role="tab" aria-controls="histograms" aria-selected="false" onclick="switchActiveNav('nav-link', this)">Histograms</a>
        </li>
        <li class="nav-item">
          <a class="nav-link" id="percentiles-tab" data-toggle="tab" href="#percentiles" role="tab" aria-controls="percentiles" aria-selected="false" onclick="switchActiveNav('nav-link', this)">Percentiles</a>
        </li>
        <li class="nav-item">
          <a class="nav-link" id="timeseries-tab" data-toggle="tab" href="#timeseries" role="tab" aria-controls="timeseries" aria-selected="false" onclick="switchActiveNav('nav-link', this)">Timeseries</a>
        </li>
        <li class="nav-item">
          <a class="nav-link" id="responses-timeseries-tab" data-toggle="tab" href="#responses-timeseries" role="tab" aria-controls="responses-timeseries" aria-selected="false" onclick="switchActiveNav('nav-link', this)">Responses Timeseries</a>
        </li>
        <li class="nav-item">
          <a class="nav-link" id="error-timeseries-tab" data-toggle="tab" href="#error-timeseries" role="tab" aria-controls="error-timeseries" aria-selected="false" onclick="switchActiveNav('nav-link', this)">Error Timeseries</a>
        </li>
        <li class="nav-item">
          <a class="nav-link" id="error-percent-timeseries-tab" data-toggle="tab" href="#error-percent-timeseries" role="tab" aria-controls="error-percent-timeseries" aria-selected="false" onclick="switchActiveNav('nav-link', this)">Error% Timeseries</a>
        </li>
      </ul>
      </div>
  <div class="tab-content">
    <div class="tab-pane active" id="summary" role="tabpanel" aria-labelledby="summary-tab">
      <div class="container">
        <table class="table table-hover">
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
    </div>
    <div class="tab-pane" id="responses" role="tabpanel" aria-labelledby="responses-tab">
      <div class="container">
<#if (aggregateCodes)??>
        <table class="table table-hover">
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
    </div>
    <div class="tab-pane" id="histograms" role="tabpanel" aria-labelledby="histograms-tab">
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
    <div class="tab-pane" id="percentiles" role="tabpanel" aria-labelledby="percentiles-tab">
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
    <div class="tab-pane" id="timeseries" role="tabpanel" aria-labelledby="timeseries-tab">
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
    <div class="tab-pane" id="responses-timeseries" role="tabpanel" aria-labelledby="responses-timeseries-tab">
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
    <div class="tab-pane" id="error-timeseries" role="tabpanel" aria-labelledby="error-timeseries-tab">
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
    <div class="tab-pane" id="error-percent-timeseries" role="tabpanel" aria-labelledby="error-percent-timeseries-tab">
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
