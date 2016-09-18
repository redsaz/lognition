
Logs
======


List all log briefs
-------------------

curl -v http://localhost:8080/logs


Get single log brief
--------------------

url -v -H "Accept: application/x-debitage-v1-logbrief+json" http://localhost:8080/logs/0


Upload single log
-----------------

curl -v -H "Accept: application/x-debitage-v1-logbrief+json" -H "Content-Type: application/octet-stream" -X POST --data-binary @'example.jtl' http://localhost:8080/logs


Delete single log
-----------------

curl -v -X DELETE http://localhost:8080/logs/101


