Copyright 2016 Redsaz

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


! Endpoints

!! Design

All endpoints which are accessed via GET can also be accessed by browser. This is in contrast with other designs which split the API away from the browser endpoints (e.g. REST: "api/v1/notes" vs. Browser: "notes"). Additionally, there is no versioning in the endpoints either (e.g. "api/v1").

Instead, these are controlled by the content-type of the request. The current content type is application/x-embeddedrest-v1+json. (Yes, content-types do allow versioning behind it, for example application/x-embeddedrest-v1+json;version=1, which would allow the version to be optional, but this seems a bit too much and the version would be better as required.) https://en.wikipedia.org/wiki/Internet_media_type explains the rules behind it.

Watch the video for reasons why: https://yow.evener.com/yow-2011-1004/domain-driven-design-for-restful-systems-by-jim-webber-1047

HATEOAS is achieved with link headers, rather than in the message body.

!! List

GET /notes - Lists brief summaries of all of the notes contained in the system.

