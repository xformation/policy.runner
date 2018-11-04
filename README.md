# policy.runner #

### What is this repository for?

#### Project features:

	- Create new Rules with english query checks
	- Create new policies with one or more rules.
	- Sample page to show-case the use of query translator from simple English query to elastic DSL query.
	- Sample page to execute the policies and show search result format.

#### Features to implement:

	- Store policy execution results in dynamodb.
	- Create reporting module to collect sample reports.

### Pre-requisite  to run policy-runner application.

	- Build and install commons-libraries
	- Build and install initservice
	- Build and install mt-dynamodb (parent/Third-Party/mt-dynamodb)
	- Download local-dynamodb
	- Run dynamodb with command:

		$java -Djava.library.path=./DynamoDBLocal_lib -jar DynamoDBLocal.jar -sharedDb

	- Download and install Elastic-search 2.4.5 and start it.
	- Build, install and Run searchservice.
	- Update policy-runner application.properties for correct urls.
	- Start policy runner application.

### Create source entries into elastic search for policy query execution.

	- curl POST http://localhost:9200/indx_source/indx_type_source -d '{ "test": {"key1":"value1", "key2":"value2"} }' -H "Content-Type: application/json"

### Web-interface uses:

![Auto-Complete View](https://github.com/xformation/policy.runner/blob/master/images/Autocomplete.png)


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

### /executor

MVC controller path to load PolicyExecutor page for application.

	Method: GET
	Response:
		api redirect to index page.

### /policy

MVC controller path to load create new Policy page for application.

	Method: GET
	Response:
		api redirect to index page.

### /rule

MVC controller path to load create new Rule page for application.

	Method: GET
	Response:
		api redirect to index page.

### /translate

API to translate the input query string into elastic DSL query.

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

Api to provide auto complete suggestions for input string

	Method: POST
	Params:
		query		String		query to search into entity fields.
	Response:
		List  list of matching field names

### /execute

Api to execute a policy by its id and generate response with matching elastic document ids list.

	Method: POST
	Params:
		policyId		String		policy id.
	Response:
		List  list of PolicyRuleResult

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

### /rule/listAll

List all the entities from repository

	Method: GET
	Params:
	Response:
		List<Rule>

### /rule/{id}

API to load entity by id

	Method: GET
	Params:
	Response:
		Rule

### /rule/delete/{id}

API to delete an entity by id

	Method: POST
	Params:
	Response:
		String Success message or json error message.

### /rule/delete

API to delete an entity

	Method: POST
	Params:
		entity		request.body	Json object
	Response:
		String Success message or json error message.

### /rule/create

API to create an entity

	Method: POST
	Params:
		entity		request.body	Json object
	Response:
		Policy as json object.
		
### /rule/update

API to update an entity

	Method: POST
	Params:
		entity		request.body	Json object
	Response:
		Policy as json object.
