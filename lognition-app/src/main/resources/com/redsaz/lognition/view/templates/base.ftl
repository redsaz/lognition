<#--
 Copyright 2023 Redsaz <redsaz@gmail.com>.

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
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <#-- The above meta tags *must* come first in the head; any other head content must come *after* these tags -->
  <link rel="apple-touch-icon" sizes="180x180" href="/apple-touch-icon.png">
  <link rel="icon" type="image/png" sizes="32x32" href="/favicon-32x32.png">
  <link rel="icon" type="image/png" sizes="16x16" href="/favicon-16x16.png">
  <link rel="manifest" href="/site.webmanifest">
  <link rel="mask-icon" href="/safari-pinned-tab.svg" color="#5bbad5">
  <meta name="apple-mobile-web-app-title" content="Lognition">
  <meta name="application-name" content="Lognition">
  <meta name="msapplication-TileColor" content="#da532c">
  <meta name="theme-color" media="(prefers-color-scheme: light)" content="white" />
  <meta name="theme-color" media="(prefers-color-scheme: dark)" content="black" />
  <meta name="color-scheme" content="dark light">

  <title>${title} - lognition</title>

<#list stylesheets! as sheet>
  <link href="${sheet}" rel="stylesheet">
</#list>

  <link href="${dist}/css/pure-min.css" rel="stylesheet">
  <link href="${dist}/css/grids-responsive-min.css" rel="stylesheet">
  <link href="${dist}/fontawesome/css/fontawesome.min.css" rel="stylesheet">
  <link href="${dist}/fontawesome/css/solid.min.css" rel="stylesheet">
  <link href="${dist}/css/app.css" rel="stylesheet">
  <script src="${dist}/js/theme.js"></script>
</head>
<body>
    <header class="custom-wrapper pure-g" id="menu">
        <div class="pure-u-1 pure-u-sm-4-24">
            <div class="pure-menu">
                <a href="${base}/" class="pure-menu-heading custom-brand">lognition</a>
                <a href="#" class="custom-toggle" id="toggle"><s class="bar"></s><s class="bar"></s></a>
            </div>
        </div>
        <div class="pure-u-1 pure-u-sm-16-24">
            <nav class="pure-menu pure-menu-horizontal custom-can-transform">
                <ul class="pure-menu-list">
                    <li class="pure-menu-item"><a href="${base}/logs" class="custom-menu-link pure-menu-link">Logs</a></li>
                    <li class="pure-menu-item"><a href="${base}/reviews" class="custom-menu-link pure-menu-link">Reviews</a></li>
                    <#if brief??><li class="pure-menu-item custom-menu-item"><a href="${base}/logs/#{brief.id}/edit" class="custom-menu-link pure-menu-link">Edit</a></li></#if>
                </ul>
            </nav>
        </div>
        <div class="pure-u-1 pure-u-sm-4-24">
            <div class="pure-menu pure-menu-horizontal custom-menu-3 custom-can-transform">
                <ul class="pure-menu-list">
                    <li class="pure-menu-item">
                        <a href="javascript:applyTheme('dark');" class="theme-selector theme-selector-auto custom-menu-link pure-menu-link"><i class="fa-solid fa-circle-half-stroke"></i> auto</a>
                        <a href="javascript:applyTheme('light');" class="theme-selector theme-selector-dark custom-menu-link pure-menu-link"><i class="fa-regular fa-moon"></i> dark</a>
                        <a href="javascript:applyTheme('auto');" class="theme-selector theme-selector-light custom-menu-link pure-menu-link"><i class="fa-regular fa-sun"></i> light</a>
                    </li>
                </ul>
            </div>
        </div>
    </header>
  <script>
  (function (window, document) {
  var menu = document.getElementById('menu'),
      rollback,
      WINDOW_CHANGE_EVENT = ('onorientationchange' in window) ? 'orientationchange':'resize';

  function toggleHorizontal() {
      menu.classList.remove('closing');
      [].forEach.call(
          document.getElementById('menu').querySelectorAll('.custom-can-transform'),
          function(el){
              el.classList.toggle('pure-menu-horizontal');
          }
      );
  };

  function toggleMenu() {
      // set timeout so that the panel has a chance to roll up
      // before the menu switches states
      if (menu.classList.contains('open')) {
          menu.classList.add('closing');
          rollBack = setTimeout(toggleHorizontal, 500);
      }
      else {
          if (menu.classList.contains('closing')) {
              clearTimeout(rollBack);
          } else {
              toggleHorizontal();
          }
      }
      menu.classList.toggle('open');
      document.getElementById('toggle').classList.toggle('x');
  };

  function closeMenu() {
      if (menu.classList.contains('open')) {
          toggleMenu();
      }
  }

  document.getElementById('toggle').addEventListener('click', function (e) {
      toggleMenu();
      e.preventDefault();
  });

  window.addEventListener(WINDOW_CHANGE_EVENT, closeMenu);
  })(this, this.document);
  </script>

  <main role="main" class="main">
<#include content>
  </main>

</body>
</html>
</#escape>
