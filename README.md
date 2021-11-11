## Bank Account App
A simple application mimicking common use cases of a bank account.

## Prerequisites
* JDK 11
* **(optional)** Gradle or use the included gradle wrapper

## Installing and launching

* Checkout `git clone https://github.com/auztbd/bank-account-app.git` and import project in your IDE
* Open terminal, and navigate to the project root directory
* Run `docker-compose up` to start RabbitMQ
* Start the application in your IDE or run `./gradlew bootRun` in terminal
* Send requests to `http://localhost:8085/api/`
* RabbitMQ dashboard can be accessed at `http://localhost:15672/`
* H2 database console can be accessed at `http://localhost:8085/h2/`

Have fun and ❤️ Kotlin! 
