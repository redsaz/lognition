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
        <form class="pure-form pure-form-stacked" action="${base}/logs/${brief.id}" method="POST" enctype="multipart/form-data">
          <fieldset>
            <div class="pure-g">
              <legend class="pure-u-1">Edit Log</legend>
              <label class="pure-u-1" for="name">Name</label>
              <input class="pure-u-1" type="text" id="name" name="name" placeholder="Log" value="${brief.name!}"/>
              <label class="pure-u-1" for="notes">Notes</label>
              <textarea class="pure-u-1" rows="10" id="notes" name="notes">${brief.notes!}</textarea>
              <label class="pure-u-1" for="labels">Labels</label>
              <input class="pure-u-1" type="text" id="labels" name="labels" placeholder="key1=value1 key2=value2" value="${labelsText!}"/>
              <button class="pure-button pure-button-primary pure-u-1 pure-u-sm-1-4 pure-u-lg-1-8" type="submit"><i class="fa fa-save"></i> Save</button>
            </div>
          </fieldset>
        </form>
</#escape>
