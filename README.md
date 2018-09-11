# Template: Salesforce and Database User Bidirectional Sync

Bi-directionally synchronizes user data between Salesforce and a database system like MySQL or any JDBC protocol supporting DB system. 

![d2afb21c-1dd4-4299-9a49-3c27107b9b47-image.png](https://exchange2-file-upload-service-kprod.s3.us-east-1.amazonaws.com:443/d2afb21c-1dd4-4299-9a49-3c27107b9b47-image.png)

This template configures the fields to be synchronized, how they map, and criteria on when to trigger the synchronization. This template can trigger either using a polling mechanism or can be easily modified to work with Salesforce outbound messaging to better utilize Salesforce API calls. This template leverages watermarking functionality to ensure that only the most recent items are synchronized and batch to effectively process many records at a time. A database table schema is included to make testing this template easier.

# License Agreement

Using this template is subject to the conditions of this <a href="https://github.com/mulesoft/template-sfdc2db-user-bidirectional-sync/blob/4.1/AnypointTemplateLicense.pdf">License Agreement</a>.
Review the terms of the license before downloading and using this template. In short, you are allowed to use the template for free with Mule Enterprise Edition, CloudHub, or as a trial in Anypoint Studio.

**Use Case:** I want to have my users synchronized between Salesforce and Database organizations.

### Overview 

To keep Salesforce synchronized with Database, the integration behavior can be summarized as follows:

1. Ask Salesforce:
	> *Which changes have there been since the last time I got in touch with you?*

2. For each of the updates fetched in the previous Step 1, inquire from the database:
	> *Does the update received from A should be applied?*

3. If the answer from the database in Step 2 is *Yes*, then *upsert* (create or update depending each particular case) database with the belonging change.

4. Repeat Steps 1 to 3 the other way around using the database as the source and Salesforce as the target.

 Repeat *ad infinitum*:

5. Ask Salesforce:
	> *Which changes have there been since the question I've made in the Step 1?*

And so on...
  
The question for recent changes since a certain moment is nothing but a scheduler with a watermark defined.

### Considerations

**Note:** This template illustrates the synchronization use case between Salesforce and a Database, thus it requires a database instance to work.
The template comes packaged with a SQL script to create the database table that it uses. 
It is the user responsibility to use that script to create the table in an available schema and change the configuration accordingly.
The SQL script file can be found in the src/main/resources/User.sql file.

This template is customized for MySQL. To use it with different SQL implementation, some changes are necessary:

* Update the SQL script dialect to the desired one.
* Replace MySQL driver library dependency to desired one in the pom.xml file.
* Set the database properties in the `mule.*.properties` file.

Before running this template:

1. **Users cannot be deleted in Salesforce:** For now, the only thing to do regarding users removal is disabling/deactivating them, but this won't make the username available for new user.
2. **Each user needs to be associated to a Profile:** Salesforce profiles are what define the permissions the user has for manipulating data and other users. Each Salesforce account has its own profiles. Check out the next section to define a map between Profile IDs from the source account to the ones in the target account and the other way around.

### Database Considerations

There may be a few things that you need to know regarding the database for this template to work.

This template may be using date time/timestamp fields from the DB in order to do comparisons and take further actions.
While the template handles the time zone by sending all such fields in a neutral time zone, it can not handle **time offsets**.
We define as **time offsets** the time difference that may surface between date time/timestamp fields from different systems due to a differences in the system's internal clock.
The user of this template should take this in consideration and take the actions needed to avoid the time offset.

### As a Data Source

There are no particular considerations for this template regarding DB as data origin.

### Database as a Destination

There are no particular considerations for this template regarding DB as data destination.

### Salesforce Considerations

There may be a few things that you need to know regarding Salesforce, in order for this template to work.

In order to have this template working as expected, you should be aware of your own Salesforce field configuration.

### Salesforce FAQ

- Where can I check that the field configuration for my Salesforce instance is the right one? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=checking_field_accessibility_for_a_particular_field.htm&language=en_US">Salesforce: Checking Field Accessibility for a Particular Field</a>

- Can I modify the Field Access Settings? How? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=modifying_field_access_settings.htm&language=en_US">Salesforce: Modifying Field Access Settings</a>

### Salesforce As the Data Source

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

### Importing the Template into Studio

In Studio, click the Exchange X icon in the upper left of the taskbar, log in with your
Anypoint Platform credentials, search for the template, and click **Open**.

### Running on Studio

After opening your template in Anypoint Studio, follow these steps to run it:

+ Locate the properties file `mule.dev.properties`, in src/main/resources
+ Complete all the properties required as per the examples in the section "Properties to be configured".
+ Right click your template project folder.
+ Hover your mouse over `Run as`.
+ Click `Mule Application (configure)`.
+ Inside the dialog, select Environment and set the variable `mule.env` to the value `dev`.
+ Click `Run`.


### Running on Mule Standalone

Complete all properties in one of the property files, for example in mule.prod.properties and run your app with the corresponding environment variable to use it. To follow the example, this is `mule.env=prod`. 


## Running on CloudHub

While creating your application on CloudHub (or you can do it later as a next step), you need to go to Deployment > Advanced to set all environment variables detailed in "Properties to Configure" as well as the **mule.env**.


### Deploying Your Template on CloudHub

In Studio, right click your project name in Package Explorer and select
Anypoint Platform > Deploy on CloudHub.


## Properties to Configure

In order to use this Mule template you need to configure properties (credentials, configurations, etc.) either in properties file or in CloudHub as Environment Variables. 

### Application Configuration

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


### Salesforce Connector Configuration

+ sfdc.username `polly.hedra@mail.com`
+ sfdc.password `Multi999Dimensional`
+ sfdc.securityToken `avsfwCUl7apQs56Xq2AKi3X`
+ sfdc.watermark.default.expression `2018-01-04T11:00:00.000Z`  
+ sfdc.integration.user.id `005n0000000T3QkAAK`

	**Note:** To find out the correct *sfdc.integration.user.id* value, refer to example project **Salesforce Data Retrieval** in Anypoint Exchange.

This property is an important one, as it configures what should be the start point of the synchronization. 

```
+ from.sfdc.to.db.profilesMap `['00r80000001CEiGAAW': '00e80000110CDfGMAX','00e30000000ifQyAAI': '00q70000000fiQyEZI']`  
+ from.db.to.sfdc.profilesMap `['00r80000001CEiGAAW': '00e80000110CDfGMAX','00e30000000ifQyAAI': '00q70000000fiQyEZI']`  
```

The meaning of the properties above is defined in the second consideration in the previous section.

# API Calls

Salesforce imposes limits on the number of API Calls that can be made. Therefore calculating this amount may be an important factor to consider. The template calls to the API can be calculated using the formula:

***1 + X + X / 200***

Being ***X*** the number of Users to be synchronized on each run. 

The division by ***200*** is because, by default, Users are gathered in groups of 200 for each Upsert API Call in the commit step. Also consider that this calls are executed repeatedly every polling cycle.	

For instance if 10 records are fetched from origin instance, then 12 api calls will be made (1 + 10 + 1).


### Customize It!

This brief guide intends to give a high level idea of how this template is built and how you can change it according to your needs.
As Mule applications are based on XML files, this page describes the XML that apply to this template.

More files can be found such as test classes and Mule application files, but to keep it simple we focus on these XML files:

- config.xml
- businessLogic.xml
- endpoints.xml
- errorHandling.xml


### config.xml

Configuration for Connectors and Configuration Properties are set in this file. Even you can change the configuration here, all parameters that can be modified here are in properties file, and this is the recommended place to do it so. Of course if you want to do core changes to the logic you probably need to modify this file.

In the visual editor they can be found on the *Global Element* tab.

### businessLogic.xml

This file holds the functional aspect of the template (points 2. to 4. described in the previous "Overview" section. The main component is a Batch job, and it includes *steps* for both executing the synchronization from Salesforce to Database, and the other way around.

### endpoints.xml

This file should contain every inbound and outbound endpoint of your integration app. It is intended to contain the application API.
In this particular template, this file contains three Flows: 

The first one we call **scheduler** flow. This one contains the Scheduler that  periodically triggers the **querySalesforce** and **queryDatabase** flows based on value of syncState variable. The syncState variable is stored by using **SchedulerStatus** ObjectStore and updated before and after each executing the batch job process.

The second one we call **queryDatabase** flow. This one contains watermarking logic for querying the database for updated and created users that meet the defined criteria in the query since the last polling. The last invocation timestamp is stored by using Objectstore Component and updated after each database query.

The third one we call **querySalesforce** flow. This one contains watermarking logic for querying Salesforce for updated and created Users that meet the defined criteria in the query since the last polling. The last invocation timestamp is stored by using Objectstore Component and updated after each Salesforce query.

### errorHandling.xml

This is the right place to handle how your integration reacts depending on the different exceptions. 
This file provides error handling that is referenced by the main flow in the business logic.






