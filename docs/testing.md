# Testing
## API Testing

### Run Couchbase
You should run local Couchbase for developing and testing:
1. Install Docker (https://docs.docker.com/install/).
1. Run Couchbase Community edition in docker container: 
   ```scripts/couchbase-run.sh``` 
   It pulls and runs image or starts a stopped container.
1. Initialize couchbase cluster and setup application bucket and indices:
   ```scripts/couchbase-init-test.sh```
   
### Run application
Build and execute the application in test mode. 
In ```backend``` directory:
1. Build jar:
   ```./gradlew bootJar```
1. Run application in test mode:
    ```java -Dtestmode=on -jar build/libs/backend-*.jar```   

### Run test
In the backend-test directory run gradle:
```./gradlew test``` 

### What is the test mode
This mode enables features which are useful for testing, but very dangerous in the production:
* HTTP Basic Authentication (in addition to OAuth2). The application allows any credentials
which have password equals to a username. If user name ends with `_group_admin` user gets `GROUP_ADMIN`
authority (role). In another case authority is `USER`.
* Passwords are exposed in Config Rest API (/configs). 
 
 