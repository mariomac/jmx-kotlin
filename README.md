Test it with a real tomcat:

```
docker run -d -e CATALINA_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=7199 -Dcom.sun.management.jmxremote.rmi.port=7199 -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=127.0.0.1" -p 7199:7199 tomcat:latest
```

Example input file:

```yaml
collect:
    - domain: test
      event_type: CatSample
      beans:
          - query: type=Cat,name=*
            attributes:
                - Name
```

(for a more real example: [tomcat.yml](tomcat.yml)

Example response: [my-tomcat.json](my-tomcat.json). You can compare it with the original [nri-jmx response](original-tomcat.json)
