# Distributed transaction - Saga

## Problem
How would you model distributed transaction where steps (transactions) are sequential, and current state depends on before?

Example: you are buying tickets to cinema. If you select places and next during payment you run out of money, reserved seats should return to the common pool and be available.

Business logic responsible for booking seats is handled by different process from payment.
Components are deployed on different hosts, possibly different availability zones.
Saga to the rescue: [link](http://www.cs.cornell.edu/andru/cs711/2002fa/reading/sagas.pdf)

## Big picture

### Successfull saga
![successful](https://raw.githubusercontent.com/torczuk/distributed-transaction-saga/master/img/successful_saga.png)

### Failed saga
![failed](https://raw.githubusercontent.com/torczuk/distributed-transaction-saga/master/img/failed_saga.png)

## Structure & modules

```
...
├── booking                                           - component responsible for starting saga
├── domain
├── infrastructure                                    - boilerplate infra code for all components
├── order                                             - component responsible for place an order
├── payment                                           - component responsible for place an payment
└── system-tests                                      - end to end tests: happy path & fail cases scenarios
```

Transactional components - `booking`,  `order` and `payment` communicate using apache kafka by sending an event
* created
* confirmed
* cancelled

Any cancell event should rollback saga. It means - if payment can not compleated it emites `cancel event` and previous commited transactions, in this case order and booking, will be **rollbacked**

In order to simplify *side effect* as much as possible **transaction** in this example is just creation a json file on disc

## Testing

Run this command before
```bash
mkdir -p /tmp/test/db/bookings \
         /tmp/test/db/orders \
         /tmp/test/db/payments
```


Solution contains different types of tests. Unit, integration and system tests. Last two categories of tests use kafka infrastructure inside docker.
Tests for `booking`, `order` and `payment` components are starting docker-compose before execution. Please take a look at the each `docker-compose.yml` in related module for more details.

`system-tests` module is dedicated to verify *saga* implementation against topology of components.
It means, kafka, zookeeper and all three components are started together.
Sample test looks like below.

#### all components are up and running
```kotlin
    @SystemTest
    fun `distributed transaction should run successfully when all components are up and running`() {
        logContainers()

        val transactionId = uuid()
        val response = POST("http://$bookingHost:$bookingPort/api/v1/bookings/$transactionId")

        await("booking is confirmed").pollDelay(ONE_SECOND).atMost(ONE_MINUTE).until {
            val statuses = GET("http://$bookingHost:$bookingPort/${location(response.body)}")
            log.info("status for {}: {}", transactionId, statuses.body)
            isConfirmed(statuses.body, transactionId)
        }
    }
```


#### one component is unavailable
```kotlin
    @SystemTest
    fun `should book successfully order even when payment component is not available for defined number of time`() {
        docker.pause("system_test_payment")
        logContainers()

        val transactionId = uuid()
        val response = POST("http://$bookingHost:$bookingPort/api/v1/bookings/$transactionId")
        simulateUnavailability("system_test_payment")

        await("booking is confirmed").pollDelay(ONE_SECOND).atMost(ONE_MINUTE).until {
            val statuses = GET("http://$bookingHost:$bookingPort/${location(response.body)}")
            log.info("status for {}: {}", transactionId, statuses.body)
            isConfirmed(statuses.body, transactionId)
        }
    }
```

#### payment failed - transaction must be rollbacked
```
TODO
```


#### Test all together
```
./gradlew clean \
         :booking:build \
         :order:build \
         :payment:build \
         systemTest --info
```

or one by one

##### Booking
```
./gradlew clean :booking:build --info
```

##### Order
```
./gradlew clean :order:build --info
```

##### Payment
```
./gradlew clean :payment:build --info
```

#### System test

All components must be build before running this comand
```
./gradlew systemTest --info
```


## Trade offs
* Ports during tests are not randomized
