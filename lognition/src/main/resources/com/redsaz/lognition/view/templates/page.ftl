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
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <#-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->
    <link rel="apple-touch-icon" sizes="180x180" href="/apple-touch-icon.png">
    <link rel="icon" type="image/png" sizes="32x32" href="/favicon-32x32.png">
    <link rel="icon" type="image/png" sizes="16x16" href="/favicon-16x16.png">
    <link rel="manifest" href="/site.webmanifest">
    <link rel="mask-icon" href="/safari-pinned-tab.svg" color="#5bbad5">
    <meta name="apple-mobile-web-app-title" content="Lognition">
    <meta name="application-name" content="Lognition">
    <meta name="msapplication-TileColor" content="#da532c">
    <meta name="theme-color" content="#ffffff">

    <title>${title} - lognition</title>

    <link href="${dist}/css/template.css" rel="stylesheet">

    <link href="${dist}/css/dygraph.css" rel="stylesheet">
    <link href="${dist}/css/chartist.min.css" rel="stylesheet">
    <link href="${dist}/css/chartist-plugin-tooltip.css" rel="stylesheet">
    <link href="${dist}/css/chartist-plugin-legend.css" rel="stylesheet">

    <#-- Bootstrap core CSS -->
    <link href="${dist}/css/bootstrap.min.css" rel="stylesheet">

    <#-- Custom styles for this template -->
    <link href="${dist}/css/dashboard.css" rel="stylesheet">

    <#-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
    <!--[if lt IE 9]>
      <script src="${dist}/js/html5shiv.min.js"></script>
      <script src="${dist}/js/respond.min.js"></script>
    <![endif]-->
  </head>

  <body>

    <div class="container-fluid">
    <nav class="navbar navbar-inverse navbar-fixed-top">
      <div class="container-fluid">
        <div class="navbar-header">
          <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar" aria-expanded="false" aria-controls="navbar">
            <span class="sr-only">Toggle navigation</span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </button>
          <a class="navbar-brand" href="/">lognition</a>
        </div>
        <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
          <ul class="nav navbar-nav">
            <li><a href="${base}/logs">Logs</a></li>
            <li><a href="${base}/reviews">Reviews</a></li>
            <#if brief??><li><a href="${base}/logs/${brief.id}/edit">Edit</a></li></#if>
          </ul>
        </div>
        <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
          <ul class="nav navbar-nav">
          </ul>
        </div>
        <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
          <ul class="nav navbar-nav">
          </ul>
        </div>
      </div>
    </nav>

    <div class="container-fluid">
<#include content>
    </div>

    <script src="${dist}/js/jquery.min.js"></script>
    <script src="${dist}/js/bootstrap.min.js"></script>
    <script src="${dist}/js/ie10-viewport-bug-workaround.js"></script>
    <script>
      $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
        window.dispatchEvent(new Event('resize'));
      });
    </script>
  </body>
</html>
</#escape>