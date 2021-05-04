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
      <div class="container">
        <form action="${base}/logs/${brief.id}" method="POST" enctype="multipart/form-data">
          <input type="text" class="form-control" name="name" placeholder="Name" value="${brief.name!}"/><br/>
          <textarea class="form-control" rows="10" name="notes">${brief.notes!}</textarea>
          <input type="text" class="form-control" name="labels" placeholder="Labels, space separated (ex: key1=value1 key2=value2)" value="${labelsText!}"/>
          <button type="submit" class="btn btn-primary">Save</button>
        </form>
      </div>
</#escape>
