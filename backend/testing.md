# Testing
## API Testing

### Local Couchbase
You should run local Couchbase for dev testing:
1. Install Docker (https://docs.docker.com/install/).
1. Execute ```couchbase-run.sh```.
1. You can login to Couchbase console http://localhost:8091/ui/index.html with 
   login ```Adminstrator``` and password ```password```

### Test mode
You should run application in test mode using JVM parameter ```testmode```:
```
java -jar ideaelection-xx.jar -Dtestmode=on
``` 

This mode enables features which are useful for testing, but very dangerous in production:
* HTTP Basic Authentication (in addition to OAuth2). The application allows any credentials
which have password equals to a username.  
* Passwords are exposed in Config Rest API (/configs). 
 
 