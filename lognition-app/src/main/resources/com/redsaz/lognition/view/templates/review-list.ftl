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
    <script>
      function deleteReview(reviewId) {
          var xhr = new XMLHttpRequest();
          var url = '${base}/reviews/' + reviewId;
          xhr.open('DELETE', url, true);
          xhr.addEventListener('load', function() {
              window.location.href = "${base}/reviews";
          });
          xhr.addEventListener('error', function() {
              console.error("Could not delete reviewId=" + reviewId);
          });
          xhr.send();
      }
      function editReview(reviewId) {
          window.location.href='${base}/reviews/' + reviewId + '/edit';
      }
    </script>
    <h1>Reviews</h1>
    <div>
      <a href="reviews/create" class="pure-button"><i class="fa fa-file"></i> Create</a>
      <div class="fcm">
        <#list reviews as review>
        <div class="fcm-parent">
          <a class="fcm-child-item" href="${base}/reviews/#{review.id}/${review.uriName}">
            <span class="fcm-child-item-title">${review.name}</span> - ${review.description}
          </a>
          <span class="fcm-child-actions">
            <ul style="display: flex;">
              <li onclick="editReview(#{review.id})"><i class="fa fa-edit"></i></li>
              <li onclick="deleteReview(#{review.id})"><i class="fa fa-trash"></i></li>
            </ul>
          </span>
        </div>
        </#list>
      </div>
    </div>
    <script>
      Array.prototype.slice.call( document.getElementsByClassName("fcm-parent")).forEach(
        function(fcmparent) {
          fcmparent.addEventListener("mouseenter", function( event ) {
            event.target.getElementsByClassName("fcm-child-actions")[0].style.display = "flex";
          }, false);
          fcmparent.addEventListener("mouseleave", function( event ) {
            event.target.getElementsByClassName("fcm-child-actions")[0].style.display = "none";
          }, false);
        }
      );
    </script>
</#escape>
