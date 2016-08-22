<#--
 Copyright 2016 Redsaz <redsaz@gmail.com>.

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
          <h1 class="page-header">Notes</h1>

          <div class="table-responsive">
            <a href="notes/create" class="btn btn-default">Create</a>
            <table class="table table-striped">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Body</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                <#list notes as note>
                <tr>
                  <td><a href="notes/${note.id}">${note.title}</a></td>
                  <td>${note.body}</td>
                  <td>
                    <form action="${base}/notes/delete" method="POST">
                      <a href="notes/${note.id}/edit">
                        <span class="glyphicon glyphicon-pencil" aria-hidden="true"></span>
                        <span class="sr-only">Edit</span>
                      </a>
                      <input type="hidden" name="id" value="${note.id}"/>
                      <button type="submit" class="btn btn-link glyphicon glyphicon-trash"><span class="sr-only">Trash</span></button>
                    </form>
                  </td>
                </tr>
                </#list>
              </tbody>
            </table>
          </div>
        </div>
      </div>
</#escape>
