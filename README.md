## 1. Run OpenMRS on port 8080 and MySQL on port 3306

Follow instructions here: https://github.com/openmrs/openmrs-distro-referenceapplication/tree/3.x

#### Open port `3306` + enable the bin log:
If running in Docker, you can do it by setting the `ports` and overriding the `command`:
```
    ports:
      - 3306:3306
    command: "mysqld --character-set-server=utf8 --collation-server=utf8_general_ci --log-bin --binlog-format=ROW"
```
to the `mysql` service definition in **docker-compose.yml** file.


## 2. Run SENAITE on port 8085
```
git clone https://github.com/senaite/senaite.docker
cd senaite.docker
docker build -t senaite .
docker run --rm --name senaite -p 8085:8080 senaite
```
Access http://localhost:8085/
and follow the steps to "Install SENAITE"

user/password `admin/admin`


## 3. Run OpenMRS EIP sender application on port 8086

#### Set the application-sender.properties
```
git clone https://github.com/openmrs/openmrs-eip/
cd openmrs-eip/
cp distribution/springboot_setup/sender/_application.properties app/src/main/resources/application-sender.properties
```
```
nano app/src/main/resources/application-sender.properties
```
```
server.port=8086

db-event.destinations=outbound-lims

camel.springboot.xmlRoutes=file:./distribution/springboot_setup/sender/routes/*.xml

debezium.db.user=root
debezium.db.password=mysql_root_password

debezium.offsetFilename=offset.debezium
debezium.historyFilename=history.debezium

senaite.baseUrl=http://localhost:8085/senaite
senaite.username=admin
senaite.password=admin

openmrs.username=admin
openmrs.password=your_admin_password

spring.openmrs-datasource.username=root
spring.openmrs-datasource.password=mysql_root_password
```

#### Optional: Send the log to console
```
nano app/src/main/resources/logback.xml
```
Add the following log appender
```
<appender name="console" class="ch.qos.logback.core.ConsoleAppender">
  <encoder>
      <pattern>%-5level %date [%thread] %logger{8}:%line %msg%n</pattern>
  </encoder>
</appender>
<root level="info">
    <appender-ref ref="console" />
</root>
```

#### Run the sender app
```
mvn clean install
java -jar -Dspring.profiles.active=sender app/target/openmrs-eip-app-1.0-SNAPSHOT.jar

```
## 4. Add SENAITE routes to it

```
rsync -av routes/ ../openmrs-eip/distribution/springboot_setup/sender/routes/
```
