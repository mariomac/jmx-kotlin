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

Example response:

```json
{
  "name": "com.newrelic.jmx",
  "protocol_version": "3",
  "integration_version": "2.2.5",
  "data": [
    {
      "entity": {
        "name": "test",
        "type": "jmx-domain",
        "id_attributes": [
          {
            "Key": "host",
            "Value": "localhost"
          },
          {
            "Key": "port",
            "Value": "7199"
          }
        ]
      },
      "metrics": [
        {
          "Name": "Isidoro",
          "bean": "type=Cat,name=Isidoro",
          "displayName": "test",
          "domain": "test",
          "entityName": "domain:test",
          "event_type": "CatSample",
          "host": "localhost",
          "key:name": "Isidoro",
          "key:type": "Cat",
          "query": "type=Cat,name=*"
        },
        {
          "Name": "Heathcliff",
          "bean": "type=Cat,name=Heathcliff",
          "displayName": "test",
          "domain": "test",
          "entityName": "domain:test",
          "event_type": "CatSample",
          "host": "localhost",
          "key:name": "Heathcliff",
          "key:type": "Cat",
          "query": "type=Cat,name=*"
        }
      ],
      "inventory": {},
      "events": []
    }
  ]
}

```