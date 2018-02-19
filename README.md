
# Anypoint Template: Salesforce and Database User Bidirectional Sync

+ [License Agreement](#licenseagreement)
+ [Use Case](#usecase)
+ [Considerations](#considerations)
	* [DB Considerations](#dbconsiderations)
	* [Salesforce Considerations](#salesforceconsiderations)
+ [Run it!](#runit)
	* [Running on premise](#runonopremise)
	* [Running on Studio](#runonstudio)
	* [Running on Mule ESB stand alone](#runonmuleesbstandalone)
	* [Running on CloudHub](#runoncloudhub)
	* [Deploying your Anypoint Template on CloudHub](#deployingyouranypointtemplateoncloudhub)
	* [Properties to be configured (With examples)](#propertiestobeconfigured)
+ [API Calls](#apicalls)
+ [Customize It!](#customizeit)
	* [config.xml](#configxml)
	* [businessLogic.xml](#businesslogicxml)
	* [endpoints.xml](#endpointsxml)
	* [errorHandling.xml](#errorhandlingxml)


# License Agreement <a name="licenseagreement"/>
Note that using this template is subject to the conditions of this [License Agreement](AnypointTemplateLicense.pdf).
Please review the terms of the license before downloading and using this template. In short, you are allowed to use the template for free with Mule ESB Enterprise Edition, CloudHub, or as a trial in Anypoint Studio.

# Use Case <a name="usecase"/>
I want to have my users synchronized between Salesforce and Database organizations

## Template overview 

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
  
The question for recent changes since a certain moment is nothing but a scheduler with a watermark defined.

# Considerations <a name="considerations"/>

**Note:** This particular Anypoint Template illustrates the synchronization use case between Salesforce and a Database, thus it requires a DB instance to work.
The Anypoint Template comes packaged with a SQL script to create the DB table that it uses. 
It is the user responsibility to use that script to create the table in an available schema and change the configuration accordingly.
The SQL script file can be found in [src/main/resources/User.sql](../master/src/main/resources/User.sql)

This template is customized for MySQL. To use it with different SQL implementation, some changes are necessary:

* update SQL script dialect to desired one
* replace MySQL driver library dependency to desired one in [POM](pom.xml)
* set database properties in `mule.*.properties` file

There are a couple of things you should take into account before running this template:

1. **Users cannot be deleted in Salesforce:** For now, the only thing to do regarding users removal is disabling/deactivating them, but this won't make the username available for new user.
2. **Each user needs to be associated to a Profile:** Salesforce's profiles are what define the permissions the user will have for manipulating data and other users. Each Salesforce account has its own profiles. Check out the next section to define a map between Profile Ids (from the source account to the ones in the target account and the other way around).

## DB Considerations <a name="dbconsiderations"/>

There may be a few things that you need to know regarding DB, in order for this template to work.

This Anypoint Template may be using date time/timestamp fields from the DB in order to do comparisons and take further actions.
While the template handles the time zone by sending all such fields in a neutral time zone, it can not handle **time offsets**.
We define as **time offsets** the time difference that may surface between date time/timestamp fields from different systems due to a differences in the system's internal clock.
The user of this template should take this in consideration and take the actions needed to avoid the time offset.

### As source of data

There are no particular considerations for this Anypoint Template regarding DB as data origin.
### DB as destination of data

There are no particular considerations for this Anypoint Template regarding DB as data destination.

## Salesforce Considerations <a name="salesforceconsiderations"/>

There may be a few things that you need to know regarding Salesforce, in order for this template to work.

In order to have this template working as expected, you should be aware of your own Salesforce field configuration.

### FAQ

 - Where can I check that the field configuration for my Salesforce instance is the right one?

    [Salesforce: Checking Field Accessibility for a Particular Field][1]

- Can I modify the Field Access Settings? How?

    [Salesforce: Modifying Field Access Settings][2]


[1]: https://help.salesforce.com/HTViewHelpDoc?id=checking_field_accessibility_for_a_particular_field.htm&language=en_US
[2]: https://help.salesforce.com/HTViewHelpDoc?id=modifying_field_access_settings.htm&language=en_US

### As source of data

If the user configured in the template for the source system does not have at least *read only* permissions for the fields that are fetched, then a *InvalidFieldFault* API fault will show up.

```
java.lang.RuntimeException: [InvalidFieldFault [ApiQueryFault [ApiFault  exceptionCode='INVALID_FIELD'
exceptionMessage='
Account.Phone, Account.Rating, Account.RecordTypeId, Account.ShippingCity
^
ERROR at Row:1:Column:486
No such column 'RecordTypeId' on entity 'Account'. If you are attempting to use a custom field, be sure to append the '__c' after the custom field name. Please reference your WSDL or the describe call for the appropriate names.'
]
row='1'
column='486'
]
]
```

### As destination of data

There are no particular considerations for this Anypoint Template regarding Salesforce as data destination.









# Run it! <a name="runit"/>
Simple steps to get Salesforce and Database User Bidirectional Sync running.


## Running on premise <a name="runonopremise"/>
In this section we detail the way you should run your Anypoint Template on your computer.


### Where to Download Mule Studio and Mule ESB
First thing to know if you are a newcomer to Mule is where to get the tools.

+ You can download Mule Studio from this [Location](http://www.mulesoft.com/platform/mule-studio)
+ You can download Mule ESB from this [Location](http://www.mulesoft.com/platform/soa/mule-esb-open-source-esb)


### Importing an Anypoint Template into Studio
Mule Studio offers several ways to import a project into the workspace, for instance: 

+ Anypoint Studio generated Deployable Archive (.zip)
+ Anypoint Studio Project from External Location
+ Maven-based Mule Project from pom.xml
+ Mule ESB Configuration XML from External Location

You can find a detailed description on how to do so in this [Documentation Page](http://www.mulesoft.org/documentation/display/current/Importing+and+Exporting+in+Studio).


### Running on Studio <a name="runonstudio"/>
Once you have imported you Anypoint Template into Anypoint Studio you need to follow these steps to run it:

+ Locate the properties file `mule.dev.properties`, in src/main/resources
+ Complete all the properties required as per the examples in the section [Properties to be configured](#propertiestobeconfigured)
+ Once that is done, right click on you Anypoint Template project folder 
+ Hover you mouse over `"Run as"`
+ Click on  `"Mule Application"`


### Running on Mule ESB stand alone <a name="runonmuleesbstandalone"/>
Complete all properties in one of the property files, for example in [mule.prod.properties] (../master/src/main/resources/mule.prod.properties) and run your app with the corresponding environment variable to use it. To follow the example, this will be `mule.env=prod`. 


## Running on CloudHub <a name="runoncloudhub"/>
While [creating your application on CloudHub](http://www.mulesoft.org/documentation/display/current/Hello+World+on+CloudHub) (Or you can do it later as a next step), you need to go to Deployment > Advanced to set all environment variables detailed in **Properties to be configured** as well as the **mule.env**.


### Deploying your Anypoint Template on CloudHub <a name="deployingyouranypointtemplateoncloudhub"/>
Mule Studio provides you with really easy way to deploy your Template directly to CloudHub, for the specific steps to do so please check this [link](http://www.mulesoft.org/documentation/display/current/Deploying+Mule+Applications#DeployingMuleApplications-DeploytoCloudHub)


## Properties to be configured (With examples) <a name="propertiestobeconfigured"/>
In order to use this Mule Anypoint Template you need to configure properties (Credentials, configurations, etc.) either in properties file or in CloudHub as Environment Variables. Detail list with examples:
### Application configuration
**Application configuration**

+ scheduler.frequency `10000`
+ scheduler.startDelay `100`
+ page.size `200`

**Database Connector configuration**

+ db.host `localhost`
+ db.port `3306`
+ db.user `user-name1`
+ db.password `user-password1`
+ db.name `dbname1`
+ db.watermark.default.expression `YESTERDAY`
+ db.integration.user.id `mule@localhost`


### Salesforce Connector configuration
+ sfdc.username `jorge.drexler@mail.com`
+ sfdc.password `Noctiluca123`
+ sfdc.securityToken `avsfwCUl7apQs56Xq2AKi3X`
+ sfdc.watermark.default.expression `2018-01-04T11:00:00.000Z`  
+ sfdc.integration.user.id `005n0000000T3QkAAK`

	**Note:** To find out the correct *sfdc.integration.user.id* value, please, refer to example project **Salesforce Data Retrieval** in [Anypoint Exchange](http://www.mulesoft.org/documentation/display/current/Anypoint+Exchange).

This property is an important one, as it configures what should be the start point of the synchronization. 

+ from.sfdc.to.db.profilesMap `['00r80000001CEiGAAW': '00e80000110CDfGMAX','00e30000000ifQyAAI': '00q70000000fiQyEZI']`  
+ from.db.to.sfdc.profilesMap `['00r80000001CEiGAAW': '00e80000110CDfGMAX','00e30000000ifQyAAI': '00q70000000fiQyEZI']`  

The meaning of the properties above is defined in the second consideration on [the previous section](#afewconsiderations)

# API Calls <a name="apicalls"/>
Salesforce imposes limits on the number of API Calls that can be made. Therefore calculating this amount may be an important factor to consider. The Anypoint Template calls to the API can be calculated using the formula:

***1 + X + X / 200***

Being ***X*** the number of Users to be synchronized on each run. 

The division by ***200*** is because, by default, Users are gathered in groups of 200 for each Upsert API Call in the commit step. Also consider that this calls are executed repeatedly every polling cycle.	

For instance if 10 records are fetched from origin instance, then 12 api calls will be made (1 + 10 + 1).


# Customize It!<a name="customizeit"/>
This brief guide intends to give a high level idea of how this Anypoint Template is built and how you can change it according to your needs.
As mule applications are based on XML files, this page will be organized by describing all the XML that conform the Anypoint Template.
Of course more files will be found such as Test Classes and [Mule Application Files](http://www.mulesoft.org/documentation/display/current/Application+Format), but to keep it simple we will focus on the XMLs.

Here is a list of the main XML files you'll find in this application:

* [config.xml](#configxml)
* [endpoints.xml](#endpointsxml)
* [businessLogic.xml](#businesslogicxml)
* [errorHandling.xml](#errorhandlingxml)


## config.xml<a name="configxml"/>
Configuration for Connectors and [Configuration Properties](http://www.mulesoft.org/documentation/display/current/Configuring+Properties) are set in this file. **Even you can change the configuration here, all parameters that can be modified here are in properties file, and this is the recommended place to do it so.** Of course if you want to do core changes to the logic you will probably need to modify this file.

In the visual editor they can be found on the *Global Element* tab.


## businessLogic.xml<a name="businesslogicxml"/>
This file holds the functional aspect of the template (points 2. to 4. described in the [template overview](#templateoverview)). Its main component is a [*Batch job*][8], and it includes *steps* for both executing the synchronization from Salesforce to Database, and the other way around.



## endpoints.xml<a name="endpointsxml"/>
This file should contain every inbound and outbound endpoint of your integration app. It is intended to contain the application API.
In this particular template, this file contains three Flows: 

The first one we'll call it **scheduler** flow. This one contains the Scheduler that will periodically trigger **querySalesforce** and **queryDatabase** flow based on value of syncState variable. The syncState variable is stored by using **SchedulerStatus** ObjectStore and updated before and after each executing the batch job process.

The second one we'll call it **queryDatabase** flow. This one contains watermarking logic that will be querying Database for updated/created Users that meet the defined criteria in the query since the last polling. The last invocation timestamp is stored by using Objectstore Component and updated after each Database query.

The third one we'll call it **querySalesforce** flow. This one contains watermarking logic that will be querying Salesforce for updated/created Users that meet the defined criteria in the query since the last polling. The last invocation timestamp is stored by using Objectstore Component and updated after each Salesforce query.



## errorHandling.xml<a name="errorhandlingxml"/>
This is the right place to handle how your integration will react depending on the different exceptions. 
This file holds a [Error Handling](http://www.mulesoft.org/documentation/display/current/Error+Handling) that is referenced by the main flow in the business logic.



