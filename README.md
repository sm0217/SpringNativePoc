# SpringNativePoc


## To build docker image for Spring boot JVM

docker build -f DockerfileSpringJvm --tag=<Image Name>:<Version> .

## To build docker image for Spring boot Native Graalvm

docker build -f DockerfileSpringGraalvm --tag=<Image Name>:<Version> .

## Spinning up docker containers

docker-compose up mysql -d
docker-compose up springjvm springgraalvm quarkus -d

To run k6 test

1) Create a script.js file in your desired location. This location should be mounted in the docker compose file for k6 container.
2) Add the test depending on the use case. Sample given below to stress test Spring boot JVM image for 200 VU's. Ref - https://k6.io/docs/test-types/
   
```
import http from 'k6/http';
import {sleep} from 'k6';

export const options = {
  stages: [
    { duration: '5m', target: 100 }, // traffic ramp-up from 1 to 100 users over 5 minutes.
    { duration: '15m', target: 100 }, // stay at 100 users for 30 minutes
    { duration: '5m', target: 0 }, // ramp-down to 0 users
  ],
};
  
export default function () {
  const url1 = 'http://springjvm:8080/api/transactions';
  const payload1 = JSON.stringify({
    "accountFrom": "Account1",
    "accountTo": "Account1",
    "customerId": "123",
    "paymentAmount": 10
    });

  const params1 = {
    headers: {
      'Content-Type': 'application/json',
    },
  };
  http.get(`http://springjvm:8080/api/transactions/all`);
  http.post(url1, payload1, params1);

  sleep(1);
}

```
3) docker-compose run k6 run <Location>/script.js
