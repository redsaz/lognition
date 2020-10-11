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
          <a class="nav-link active" id="summary-tab" data-toggle="tab" href="#summary" role="tab" aria-controls="summary" aria-selected="true">Summary</a>
        </li>
        <li class="nav-item">
          <a class="nav-link" id="responses-tab" data-toggle="tab" href="#responses" role="tab" aria-controls="responses" aria-selected="false">Responses</a>
        </li>
        <li class="nav-item">
          <a class="nav-link" id="histograms-tab" data-toggle="tab" href="#histograms" role="tab" aria-controls="histograms" aria-selected="false">Histograms</a>
        </li>
        <li class="nav-item">
          <a class="nav-link" id="percentiles-tab" data-toggle="tab" href="#percentiles" role="tab" aria-controls="percentiles" aria-selected="false">Percentiles</a>
        </li>
        <li class="nav-item">
          <a class="nav-link" id="timeseries-tab" data-toggle="tab" href="#timeseries" role="tab" aria-controls="timeseries" aria-selected="false">Timeseries</a>
        </li>
        <li class="nav-item">
          <a class="nav-link" id="responses-timeseries-tab" data-toggle="tab" href="#responses-timeseries" role="tab" aria-controls="responses-timeseries" aria-selected="false">Responses Timeseries</a>
        </li>
        <li class="nav-item">
          <a class="nav-link" id="error-timeseries-tab" data-toggle="tab" href="#error-timeseries" role="tab" aria-controls="error-timeseries" aria-selected="false">Error Timeseries</a>
        </li>
        <li class="nav-item">
          <a class="nav-link" id="error-percent-timeseries-tab" data-toggle="tab" href="#error-percent-timeseries" role="tab" aria-controls="error-percent-timeseries" aria-selected="false">Error% Timeseries</a>
        </li>
      </ul>
      </div>
  <div class="tab-content">
    <div class="tab-pane active" id="summary" role="tabpanel" aria-labelledby="summary-tab">
      <div class="container">
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
      </div>
    </div>
    <div class="tab-pane" id="responses" role="tabpanel" aria-labelledby="responses-tab">
      <div class="container">
        <table class="table table-hover">
          <thead>
          <tr>
            <th>Label</th>
<#list aggregateCodes as code>
            <th>${code}</th>
</#list>
          </tr>
          </thead>
          <tbody>
<#list aggregateCodeCounts as a>
          <tr>
            <th>${sampleLabels[a?index]}</th>
<#list a.getCounts()[0] as codeCount>
            <td>${codeCount}</td>
</#list>
          </tr>
</#list>
          </tbody>
        </table>
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
