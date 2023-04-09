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
        <script src="${dist}/js/app.js"></script>
        <h2>${review.name}</h2>
        <div>
          <#noescape>${descriptionHtml}</#noescape>
        </div>
        <h3 style="margin-top: 3em">Applicable Logs</h3>
        <div class="fcm" style="margin-bottom: 3em">
          <#list briefs as brief>
          <div class="fcm-parent">
            <a class="fcm-child-item" href="${base}/logs/${brief.id}/${brief.uriName}">
              <span class="fcm-child-item-title">${brief.name}</span> - ${brief.notes}
            </a>
          </div>
          </#list>
        </div>

        <div class="pure-menu pure-menu-horizontal">
            <ul class="pure-menu-list">
            <#list reviewGraphs as g>
                <li class="pure-menu-item<#if g?is_first> pure-menu-selected</#if>"><a href="#${g.urlName}" class="pure-menu-link">${g.name}</a></li>
            </#list>
            <#if imageAttachments?has_content>
              <#list imageAttachments as a>
                <li class="pure-menu-item"><a href="#attachment-${a?counter}" class="pure-menu-link">${a.name}</a></li>
              </#list>
            </#if>
            <#if attachments?has_content>
                <li class="pure-menu-item"><a href="#attachments" class="pure-menu-link">Attachments</a></li>
            </#if>
            </ul>
        </div>

        <#list reviewGraphs as g>
          <div class="content-pane" id="${g.urlName}" style="width: 100%">
            <h2>${g.name}</h2>
            <div class="graph" style="width: 100%">
              <div class="ct-chart" id="graphdiv${g?index}" style="width: 100%;<#if g.height??> height: ${g.height}</#if>"></div>
            </div>
          </div>
        </#list>
        <#if imageAttachments?has_content>
          <#list imageAttachments as a>
          <div class="content-pane" id="attachment-${a?counter}" style="width: 100%">
            <h2>${a.name}</h2>
            <div class="attachment" style="width: 100%">
              <img src="attachments/${a.path}"/>
            </div>
          </div>
          </#list>
        </#if>
        <#if attachments?has_content>
          <div class="content-pane" id="attachments" style="width: 100%">
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
