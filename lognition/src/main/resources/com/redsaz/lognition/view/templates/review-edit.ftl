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
      <div class="row">
        <div class="col-sm-12 col-md-12 main">
          <form action="${base}/reviews/${review.id}" method="POST" enctype="multipart/form-data">
            <input type="text" class="form-control" name="name" placeholder="Name" value="${review.name!}"/><br/>
            <textarea class="form-control" rows="10" name="description">${review.description!}</textarea>
            <input type="text" class="form-control" name="body" placeholder="Label Selector (ex: key1 in (value1, value2)" value="${review.body!}"/>
            <button type="submit" class="btn btn-primary">Save</button>
          </form>
        </div>
      </div>
</#escape>
