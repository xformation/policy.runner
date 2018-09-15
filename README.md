# policy.runner #

### What is this repository for?

Repository to index any type of json entity to use as policy input documents for compliance checking.
Also you can create and register new policies with scripting details to run and validate on items.

We have plan to extend this project to provide a new query language for policy verification and execution using some scripting and command-line utilities.

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

### /

MVC controller path to load index page for application.

	Method: GET
	Response:
		api redirect to index page.

### /translate

MVC API to translate the input query string into elastic DSL query.

	Method: POST
	Params:
		body		JSON		{ query: "query to search into entity fields" }.
	Response:
		JSON  elastic-search DSL query in json format.

### /queryParser

Api to parse input query into elasticsearch DSL query format.

	Method: POST
	Params:
		query		String		query in string format
	Response:
		json  object as elastic-search DSL query
		
### /suggestKey

Api to parse input query into elastic-search DSL query format.

	Method: POST
	Params:
		query		String		query to search into entity fields.
	Response:
		List  list of matching field names

### /policy/listAll

List all the entities from repository

	Method: GET
	Params:
	Response:
		List<Policy>

### /policy/{id}

API to load entity by id

	Method: GET
	Params:
	Response:
		Policy

### /policy/delete/{id}

API to delete an entity by id

	Method: POST
	Params:
	Response:
		String Success message or json error message.

### /policy/delete

API to delete an entity

	Method: POST
	Params:
		entity		request.body	Json object
	Response:
		String Success message or json error message.

### /policy/create

API to create an entity

	Method: POST
	Params:
		entity		request.body	Json object
	Response:
		Policy as json object.
		
### /policy/update

API to update an entity

	Method: POST
	Params:
		entity		request.body	Json object
	Response:
		Policy as json object.
		