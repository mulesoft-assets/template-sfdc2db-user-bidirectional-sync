# Anypoint Template: Salesforce to Database bi-directional user sync

+ [License Agreement](#licenseagreement)
+ [Use case](#usecase)
+ [Template overview](#templateoverview)
+ [Run it!](#runit)
    * [A few Considerations](#afewconsiderations)
    * [Properties to be configured](#propertiestobeconfigured)
    * [Running on CloudHub](#runoncloudhub)
    * [Running on premise](#runonopremise)
+ [Customize It!](#customizeit)
    * [config.xml](#configxml)
    * [endpoints.xml](#endpointsxml)
    * [businessLogic.xml](#businesslogicxml)
    * [errorHandling.xml](#errorhandlingxml)


# License Agreement <a name="licenseagreement"/>
Note that using this template is subject to the conditions of this [License Agreement](AnypointTemplateLicense.pdf).
Please review the terms of the license before downloading and using this template. In short, you are allowed to use the template for free with Mule ESB Enterprise Edition, CloudHub, or as a trial in Anypoint Studio.

## Use case <a name="usecase"/>

I want to have my users synchronized between Salesforce and Database organizations

## Template overview <a name="templateoverview"/>

Let's say we want to keep Salesforce synchronized with Database. Then, the integration behavior can be summarized just with the following steps:

1. Ask Salesforce:
> *Which changes have there been since the last time I got in touch with you?*

2. For each of the updates fetched in the previous step (1.), ask Database:
> *Does the update received from A should be applied?*

3. If Database answer for the previous question (2.) is *Yes*, then *upsert* (create or update depending each particular case) Database with the belonging change

4. Repeat previous steps (1. to 3.) the other way around (using Database as source and Salesforce as the target)

 Repeat *ad infinitum*:

5. Ask Salesforce:
> *Which changes have there been since the question I've made in the step 1.?*

And so on...
  
  
The question for recent changes since a certain moment is nothing but a [poll inbound][1] with a [watermark][2] defined.


# Run it! <a name="runit"/>

In order to have the template up and running just complete the two following steps:

 1. Read [a few considerations](#afewconsiderations)
 2. [Configure the application properties](#propertiestobeconfigured)
 3. Run it! ([on premise](#runonopremise) or [in Cloudhub](#runoncloudhub))

**Note:** This particular Anypoint Template illustrate the synchronization use case between SalesForce and a Database, thus it requires a DB instance to work.
The Anypoint Template comes packaged with a SQL script to create the DB table that uses. 
It is the user responsibility to use that script to create the table in an available schema and change the configuration accordingly.
The SQL script file can be found in [src/main/resources/sfdc2jdbc.sql](../master/src/main/resources/sfdc2jdbc.sql)

This template is customized for MySQL. To use it with different SQL implementation, some changes are necessary:

* update SQL script dialect to desired one
* replace MySQL driver library dependency to desired one in [POM](pom.xml)
* replace attribute `driverClassName` of `db:generic-config` element with class name of desired JDBC driver in [src/main/app/config.xml](../master/src/main/app/config.xml)
* update JDBC URL in `mule.*.properties` file

## A few Considerations <a name="afewconsiderations" />

There are a couple of things you should take into account before running this kick:

1. **Users cannot be deleted in SalesForce:** For now, the only thing to do regarding users removal is disabling/deactivating them, but this won't make the username available for a new user.
2. **Each user needs to be associated to a Profile:** SalesForce's profiles are what define the permissions the user will have for manipulating data and other users. Each SalesForce account has its own profiles. Check out the next section to define a map between Profile Ids (from the source account to the ones in the target account and the other way around).

## Properties to be configured<a name="propertiestobeconfigured"/>

### Application configuration
+ polling.frequency `10000`  
This are the milliseconds (also different time units can be used) that will run between two different checks for updates in Salesforce and Database

+ watermark.default.expression `2014-02-25T11:00:00.000Z`  
This property is an important one, as it configures what should be the start point of the synchronization.The date format accepted in SFDC Query Language is either *YYYY-MM-DDThh:mm:ss+hh:mm* or you can use Constants. [More information about Dates in SFDC][3]

### SalesForce Connector configuration
+ sfdc.username `jorge.drexler@mail.com`
+ sfdc.password `Noctiluca123`
+ sfdc.securityToken `avsfwCUl7apQs56Xq2AKi3X`
+ sfdc.url `https://login.salesforce.com/services/Soap/u/28.0`
+ sfdc.integration.user.id `005n0000000T3QkAAK`

### Database Connector configuration for
+ db.jdbcUrl `jdbc:mysql://localhost:3306/mule?user=mule&password=mule`
+ db.integration.user.id `mule@localhost`

+ from.sfdc.to.db.profilesMap `['00r80000001CEiGAAW': '00e80000110CDfGMAX','00e30000000ifQyAAI': '00q70000000fiQyEZI']`  
+ from.db.to.sfdc.profilesMap `['00r80000001CEiGAAW': '00e80000110CDfGMAX','00e30000000ifQyAAI': '00q70000000fiQyEZI']`  
The meaning of the properties above is defined in the second consideration on [the previous section](#afewconsiderations)
 
## Running on CloudHub <a name="runoncloudhub"/>

Running the template on CloudHub is as simple as follow the 4 steps detailed on the following documetation page: 
  
> [http://www.mulesoft.org/documentation/display/current/Hello+World+on+CloudHub](http://www.mulesoft.org/documentation/display/current/Hello+World+on+CloudHub)

## Running on premise <a name="runonopremise"/>
Once all properties are filled in one of the template property files (for example in [mule.prod.properties][4]) the template can be run by just choosing an enviromet and follow the steps detailed in the link placed below:

> [http://www.mulesoft.org/documentation/display/current/Hello+World+on+CloudHub](http://www.mulesoft.org/documentation/display/current/Deploying+Mule+Applications)

# Customize It!<a name="customizeit"/>
This brief guide intends to give a high level idea of how this template is built and how you can change it according to your needs.
As mule applications are based on XML files, the idea is describing each XML included in the template.
Of course more files will be found such as Test Classes and [Mule Application Files][5], but to keep it simple we will focus on the XMLs.

Here is a list of the main XML files you'll find in this application:

* [config.xml](#configxml)
* [endpoints.xml](#endpointsxml)
* [businessLogic.xml](#businesslogicxml)
* [errorHandling.xml](#errorhandlingxml)


## config.xml<a name="configxml"/>
This file holds the configuration for Connectors and [Properties Place Holders][6]. 
Although you can update the configuration properties here, we highly recommend to keep them parameterized and modified them in the belonging property files.

For this particular template, what you will find in the config file is
* the configuration for the Salesforce and Database instances that are being synced
* the property place holder configuration

In order to find the mentioned configuration, you should check out the [*Global Element* tab][7].


## endpoints.xml<a name="endpointsxml"/> 
This file should contain every inbound and outbound endpoint of your integration app. It is intended to contain the application API.
In this particular template, this file contains a couple of poll inbound endpoints that query Salesforce and database for updates using watermark as mentioned before.

## businessLogic.xml<a name="businesslogicxml"/>
This file holds the functional aspect of the template (points 2. to 4. described in the [template overview](#templateoverview)). Its main component is a [*Batch job*][8], and it includes *steps* for both executing the synchronization from Salesforce to Database, and the other way around.


## errorHandling.xml<a name="errorhandlingxml"/>
This is the right place to handle how your integration will react depending on the different exceptions. 
This file holds a [Choice Exception Strategy][9] that should be referenced by any flow included in the business logic.


  [1]: http://www.mulesoft.org/documentation/display/current/Poll+Reference
  [2]: http://blogs.mulesoft.org/data-synchronizing-made-easy-with-mule-watermarks/
  [3]: http://www.salesforce.com/us/developer/docs/officetoolkit/Content/sforce_api_calls_soql_select_dateformats.htm
  [4]: https://github.com/mulesoft/template-sfdc2sfdc-user-bidirectional-sync/blob/master/src/main/resources/mule.prod.properties
  [5]: http://www.mulesoft.org/documentation/display/current/Application+Format
  [6]: http://www.mulesoft.org/documentation/display/current/Configuring+Properties
  [7]: http://www.mulesoft.org/documentation/display/current/Global+Elements
  [8]: http://www.mulesoft.org/documentation/display/current/Batch+Processing
  [9]: http://www.mulesoft.org/documentation/display/current/Choice+Exception+Strategy
  
  
