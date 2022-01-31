<#--
 Copyright 2018 Redsaz <redsaz@gmail.com>.

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
        <h2>${review.name}</h2>
        <div>
          <#noescape>${descriptionHtml}</#noescape>
        </div>
        <h3>Applicable Logs</h3>
        <div class="table-responsive">
          <table class="table table-striped">
            <thead>
              <tr>
                <th>Name</th>
                <th>Notes</th>
              </tr>
            </thead>
            <tbody>
              <#list briefs as brief>
              <tr>
                <td><a href="${base}/logs/${brief.id}/${brief.uriName}">${brief.name}</a></td>
                <td><a href="${base}/logs/${brief.id}/${brief.uriName}">${brief.notes}</a></td>
              </tr>
              </#list>
            </tbody>
          </table>
        </div>
      </div>
      <div class="container">
        <ul class="nav nav-tabs" role="tablist">
        <#list reviewGraphs as g>
          <li role="presentation" class="nav-item"><a class="graph-nav nav-link<#if g?is_first> active</#if>" href="#${g.urlName}" aria-controls="${g.name}" role="tab" data-toggle="tab" onclick="switchActiveNav('graph-nav', this)">${g.name}</a></li>
        </#list>
        <#if imageAttachments?has_content>
          <#list imageAttachments as a>
          <li role="presentation" class="nav-item"><a class="graph-nav nav-link" href="#attachment-${a?counter}" aria-controls="attachment-${a?counter}" role="tab" data-toggle="tab" onclick="switchActiveNav('graph-nav', this)">${a.name}</a></li>
          </#list>
        </#if>
        <#if attachments?has_content>
          <li role="presentation" class="nav-item"><a class="graph-nav nav-link" href="#attachments" aria-controls="attachments" role="tab" data-toggle="tab" onclick="switchActiveNav('graph-nav', this)">Attachments</a></li>
        </#if>
        </ul>
        <div class="tab-content" style="width: 100%">
        <#list reviewGraphs as g>
          <div role="tabpanel" class="tab-pane <#if g?is_first>active</#if>" id="${g.urlName}" style="width: 100%">
            <h2>${g.name}</h2>
            <div class="graph" style="width: 100%">
              <div class="ct-chart" id="graphdiv${g?index}" style="width: 100%;<#if g.height??> height: ${g.height}</#if>"></div>
            </div>
          </div>
        </#list>
        <#if imageAttachments?has_content>
          <#list imageAttachments as a>
          <div role="tabpanel" class="tab-pane" id="attachment-${a?counter}" style="width: 100%">
            <h2>${a.name}</h2>
            <div class="attachment" style="width: 100%">
              <img src="attachments/${a.path}"/>
            </div>
          </div>
          </#list>
        </#if>
        <#if attachments?has_content>
          <div role="tabpanel" class="tab-pane" id="attachments" style="width: 100%">
            <h2>Attachments</h2>
            <div class="table-responsive">
              <table class="table table-striped">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Path</th>
                    <th>Description</th>
                    <th>Type</th>
                  </tr>
                </thead>
                <tbody>
                <#list attachments as a>
                  <tr>
                    <td><a href="attachments/${a.path}">${a.name}</a></td>
                    <td><a href="attachments/${a.path}">${a.path}</a></td>
                    <td>${a.description}</td>
                    <td>${a.mimeType}</td>
                  </tr>
                </#list>
                </tbody>
              </table>
            </div>
          </div>
        </#if>
        </div>
      </div>
      <script src="${dist}/js/chartist.min.js"></script>
      <script src="${dist}/js/chartist-plugin-tooltip.min.js"></script>
      <script src="${dist}/js/chartist-plugin-legend.min.js"></script>
      <script src="${dist}/js/dygraph.min.js"></script>
      <#list reviewGraphs as g>
        <script>
          <#noescape>${g.chartHtml}</#noescape>
        </script>
      </#list>
</#escape>
