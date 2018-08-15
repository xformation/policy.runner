# policy.runner

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

## Application api's documentation ##

### /search/setIndexMapping

Api to create a new index in elastic if not index not exists. Also add the index mappings for new entity. We can call it to update then existing index mappings too using isUpdate field.

	Method: POST
	Params:
		cls	*			String 	fully qualified name of entity class
		mappings*		String		json object string for mappings of document
		isUpdate		Boolean	send true if you would like to update the existing index mappings.
	Response:
		true if new index and mapping get updated successfully in elasticsearch.
		
