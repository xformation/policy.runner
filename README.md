# policy.runner #

### What is this repository for? ###
Repository to index any type of json entity to use as policy input doucments for compliance checking.
Also you can create and register new policies with scripting details to run and validate on items.
---
We have plan to extend this project to provide a new query language for policy varification and execution using some scripting and command-line utilities.

### How to import project for editing ###

* Import as maven project in your IDE

### Build, install and run application ###

To get started build the build the latest sources with Maven 3 and Java 8 
(or higher). 

	$ cd policy.runner
	$ mvn clean package -DSEARCH_URL=http://localhost:8092/ -DAWS_ENDPOINT=localhost:8000/eu_west_1

You can run this application as spring-boot app by following command:

	$ mvn spring-boot:run

Once done you can run the application by executing 

	$ java -jar target/policy.runner-exec.jar

### Open in browser ###
Run application using spring-boot:run and open url, you can input your English queries here to generate the output in json format.

	http://localhost:8098/

## Application api's documentation ##

### /queryParser

Api to parse input query into elasticsearch DSL query format.

	Method: POST
	Params:
		query		String		query in string format
	Response:
		json  object as elastic-search DSL query
		
