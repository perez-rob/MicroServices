# Config Server Tutorial

This tutorial steps you through setting up a Spring Cloud Config Server and the creation of a simple web service that utilizes the Spring Cloud Config Server for its configuration settings.

You will need:

* A public (read only) GitHub repository to store application configuration files.
* Spring Initializr (start.spring.io)
* IntelliJ Community Edition

## System Design

![image-20190619122849437](images/cloud-config-system.png)

All web service configuration files are stored in the Git repo (in our case, the Git repo will be on GitHub). The Config Server sits in front of the Git repo and provides access to the confguration files. Config Server clients (such as the Web Service) contact the Config Server on start up and ask for their configuration files. The configuration settings are then used by the Web Service as it starts up and while it is running.

## Building the System

We will build this system in three steps:

1. Create the Git repo.
2. Create the Spring Cloud Config Server.
3. Create the client Web Service.

### Step 1: Create the Git Repo

Our first step is to create a git repository to hold the configuration files the clients of our Config Server. Create a **public read-only** repository on GitHub. In a production environment you would limit access to the repository through an authentication/authorization mechanism but we will give read access to everyone for this tutorial. 

Create a new repo called **firstname-lastname-cloud-config-repo**, where **firstname** and **lastname** are your first name and last name respectively. Note the URL for this repo, you will use it in the next step when setting up the Spring Cloud Config Server. Your URL will look something like this:

```https://github.com/john-doe/john-doe-cloud-config-repo```

### Step 2: Create the Spring Cloud Config Server

Our next step is to create the Spring Cloud Config Server. We'll use the Spring Initializr to create our project. Go to ```start.spring.io``` and enter the following information:

* Group = com.trilogyed
* Artifact = cloud-config-service
* Dependencies = Config Server

Download the project, copy it into your working directory, and open the project in IntelliJ.

The Spring Initializr adds the required starter dependencies to our POM file for a Config Server, but we must add some annotations and some entires to the ```application.properties``` file to make our Config Server operational.

#### 2.1: Annotate the Main Application Class

 Open the main application class (```com.trilogyed.cloudconfgservice.CloudConfigServiceApplication.java```) and add the ```@EnableConfigServer``` annotation to the class. Your code should look like this:

```java
@SpringBootApplication
@EnableConfigServer
public class CloudConfigServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudConfigServiceApplication.class, args);
	}

}
```

This annotation tells spring to enable the features in the Config Server starter dependencies. This essentially turns our application into a Config Server.

#### 2.2: Add Entries to Properties File

Now we must set the port on which our Config Server will listen and tell it which Git repo it should use as to store configuration files for client applications.

Open ```src/main/resources/application.properties``` and add the following entries:

```java
server.port=9999
spring.cloud.config.server.git.uri=<URL FOR YOUR GITHUB REPO HERE>
```

Where the URL is for the repo that you created in the first step of this tutorial. 

The value for ```server.port``` is arbitrary.

### Step 3: Create the Client Web Service

Our final step is to create a new web service that will use our Config Server. Our service is a simple "hello world" service with a small twist: the web service will read the value for its greeting from configuration served up by our Config Server.

We'll build this project in four steps:

1. Create the project with Spring Initializr.
2. Create the configuration file, and check it into the Git repo.
3. Configure our Hello World service to use the Config Server.
4. Write the Controller that serves up the greeting.

#### 3.1: Create the Project

Now we will use the Spring Initializr to create our Hello Service. Go to ```start.spring.io``` and enter the following information:

- Group = com.trilogyed
- Artifact = hello-cloud-service
- Dependencies = Config Client, Spring Web Starter, Spring Boot Actuator

Download the project, copy it into your working directory, and open the project in IntelliJ.

#### 3.2: Create the Hello Service Configuration File

Now we will create the properties file that will hold the configuration setting for the Hello Service. This file will take precedence over the ```application.properties``` file that we have been using for configuration settings. The Config Server matches properties files to their respective services by name: the name of the configuration file must match the application name (we'll set the application name in the next step).

Create a file called ```hello-cloud-service.properties```, add the following entries, and check it into your configuration repo.

```java
# this is the port on which our hello-cloud-service will run
server.port=7979

# allow for RefreshScope
management.endpoints.web.exposure.include=*

# the hello-cloud-service will use this as the greeting that it serves up
officialGreeting="Greetings from the Hello Cloud Service!!! We're glad you're here!!!!!!"
```

As indicated by the comments, our service (hello-cloud-service) will run on port 7979 (this value is arbitrary). The next entry is needed to enable us to force our service to re-read its configuration without having to restart (we'll talk more about that later in the tutorial). Finally, we have an entry for our officialGreeting. This is the value that our service will read and return as its "hello".

#### 3.3: Configure the Hello Service to Use the Config Server

Although we are moving our application's configuration settings to the Config Server, we must have minimal configuration within the service itself. We do this through the ```application.properties``` file. This file will contain settings for our application name and the location of the Config Server. Add the following to ```application.properties```:

```java
# This file has just enough information so that our application can find the configuration
# service and its configuration settings.

# This name must match the name of the properties file for this application
# in the configuration repository. we are looking for a file called hello-cloud-config.properties
spring.application.name=hello-cloud-service

# This is the url to the configuration service that we will use to get our configuration
spring.config.import=configserver:http://localhost:9999
```

Important things to note:

* The ```spring.application.name``` property value must match the name of the properties file we checked into Git.
* The host and port we use for the ```spring.config.import``` must match the host and port values on which our Config Server is running.

#### 3.4: Create the Controller

The final task is to create a Controller with one endpoint. Create a file called ```com.trilogyed.hellocloudservice.controller.HelloCloudServiceController.java```.  Add the following code:

```java
@RestController
@RefreshScope
public class HelloCloudServiceController {

    @Value("${officialGreeting}")
    private String officialGreeting;

    @RequestMapping(value="/hello", method = RequestMethod.GET)
    public String helloCloud() {

        return officialGreeting;
    }
}

```

Things to note about the code:

* We use the ```@RefreshScope``` annotation. This annotation goes hand in hand with the setting in our configuration file. With these two things set, we can send an empty POST request (using Postman, for example) to the```http://localhost:7979/actuator/refresh``` endpoint of our hello-cloud-service application, which will cause the application to fetch its configuration file from the Config Server and re-read it.
* The ```@value``` annotation allows us to read a value from our configuration file. In this case, we are reading the ```officialGreeting``` property from our configuation properties file. The value read from the config file is automatically assigned to the ```officialGreeting``` property of our controller.

## Running the System

We must start up both services in order to run our system. We must start the cloud-config-service first and then start the hello-cloud-service because the latter relies on the former for its configuration.

Start the services in the order suggested and then visit http://localhost:7979/hello. The following should be displayed in your browser:

"Greetings from the Hello Cloud Service!!! We're glad you're here!!!!!!"

## Modifying the Configuration File

Finally, we will demonstrate how to use the ```@RefreshScope``` feature to read in changes to the configuration without restarting the hello-cloud-service.

1. Modify the hello-cloud-service.properties file by setting the ```officialGreeting``` property value to ```This is the NEW greeting!``` 
2. Send a POST request to ```http://localhost:7979/actuator/refresh``` using Postman. The body of the post should be an empty JSON object. Your Postman request should look like this:

![image-20190620192927630](images/postman-refresh-request.png)

3. Open your browser and visit ```localhost:7979/hello```. The application should display the new greeting.