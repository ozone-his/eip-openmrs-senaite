## 1. Run OpenMRS

Follow instructions here: https://github.com/openmrs/openmrs-distro-referenceapplication/tree/3.x


## 2. Run SENAITE (on port 8085)
```
git clone https://github.com/senaite/senaite.docker
cd senaite.docker
docker build -t senaite .
docker run --rm --name senaite -p 8085:8080 senaite
```

## 3. Run OpenMRS EIP sender application
Set the application-sender.properties with
```
git clone https://github.com/openmrs/openmrs-eip/
cd openmrs-eip/
cp distribution/springboot_setup/sender/_application.properties app/src/main/resources/application-sender.properties
```
```
nano app/src/main/resources/application-sender.properties
db-event.destinations=outbound-lims
```
Run the sender app
```
mvn clean install
cd distribution/springboot_setup/sender
java -jar -Dspring.profiles.active=sender ../../../app/target/openmrs-eip-app-1.0-SNAPSHOT.jar

```
## 4. Add SENAITE routes to it

```
rsync -av routes/ ../openmrs-eip/distribution/springboot_setup/sender/routes/
```
