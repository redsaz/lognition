
Logs
======


List all log briefs
-------------------

    curl -v http://localhost:8080/logs


Get single log brief
--------------------

    curl -v -H "Accept: application/x-lognition-v1-logbrief+json" http://localhost:8080/logs/0


Upload single log
-----------------

To upload a log, POST to /logs. Optionally, the parameters name, notes, and labels can be set. Labels can be separated by a comma or a +. Labels with an equals should use "%3D".

    curl -v -H "Accept: application/x-lognition-v1-logbrief+json" -H "Content-Type: application/octet-stream" --data-binary @'example.jtl' 'http://localhost:8080/logs?name=Example+Log&notes=This+is+a+log&labels=example1,example2%3Dbob'

This way will submit a multipart/formdata to lognition, which submits the file, name, notes, and labels:

    curl -v --form 'content=@/home/user/Downloads/example.jtl;filename=example.jtl' --form 'name=Example JTL' --form 'notes=Run at 500 reqs/sec for 1 hour.' --form 'labels=example' http://localhost:8080/logs


Delete single log
-----------------

    curl -v -X DELETE http://localhost:8080/logs/101


