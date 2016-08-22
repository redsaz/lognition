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
          <h1>Not Found</h1>
          <p>
          Sorry, we couldn't find what you wanted. Would you like to:
          <ul>
            <li><a href="${base}/notes">View all</a> your notes?</li>
            <li><a href="${base}/notes/create">Create</a> a new note?</li>
            <li><a href="javascript:history.back()">Go Back</a>?</li>
          </ul>
        </div>
      </div>
</#escape>
