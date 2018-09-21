
# Anypoint Template: Salesforce and Database User Bidirectional Sync

# License Agreement
This template is subject to the conditions of the 
<a href="https://s3.amazonaws.com/templates-examples/AnypointTemplateLicense.pdf">MuleSoft License Agreement</a>.
Review the terms of the license before downloading and using this template. You can use this template for free 
with the Mule Enterprise Edition, CloudHub, or as a trial in Anypoint Studio.

# Use Case
I want to have my users synchronized between Salesforce and Database organizations.

## Template overview 

To keep Salesforce synchronized with the database:

1. Ask Salesforce:
	> *Which changes have there been since the last time I checked with you?*

2. For each of the updates fetched in Step 1, ask Database:
	> *Does the update received from A should be applied?*

3. If the database answer for Step 2 is *Yes*, then *upsert* (creating or updating depending each case) to the database with the change.

4. Repeat Steps 1 - 3 the other way around using the database as the source and Salesforce as the target.

 Repeat *ad infinitum*:

5. Ask Salesforce:
	> *What changes have there been since Step 1?*

And so on...
  
The question for recent changes from a certain moment is nothing but a scheduler with a watermark defined.

# Considerations

**Note:** This template illustrates the synchronization use case between Salesforce and a database, and requires a database instance to work.
The database comes packaged with a SQL script to create the database table that it uses. 
It is your responsibility to use the script to create the table in an available schema and change the configuration accordingly.
The SQL script file can be found in src/main/resources/User.sql

This template is customized for MySQL. To use it with different SQL implementation, some changes are necessary:

* Update the SQL script dialect to the desired one.
* Replace the MySQL driver library dependency to the desired one in the pom.xml file.
* Set the database properties in the `mule.*.properties` file.

Before running this template:

1. **Users cannot be deleted in Salesforce:** For now, the only thing to do regarding users removal is disabling and deactivating them, but this won't make the username available for new user.
2. **Each user needs to be associated to a Profile:** Salesforce's profiles are what define the permissions the user has for manipulating data and other users. Each Salesforce account has its own profiles. See the next section to define a map between Profile IDs (from the source account to the ones in the target account and the other way around).

## DB Considerations

To get this template to work:

This template may use date time or timestamp fields from the database to do comparisons and take further actions.
While the template handles the time zone by sending all such fields in a neutral time zone, it cannot handle time offsets.
We define time offsets as the time difference that may surface between date time and timestamp fields from different systems due to a differences in the system's internal clock.
Take this in consideration and take the actions needed to avoid the time offset.

### As a Data Source

There are no considerations with using a database as a data origin.
### As a Data Destination

There are no considerations with using a database as a data destination.

## Salesforce Considerations

Here's what you need to know about Salesforce to get this template to work.

### FAQ

- Where can I check that the field configuration for my Salesforce instance is the right one? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=checking_field_accessibility_for_a_particular_field.htm&language=en_US">Salesforce: Checking Field Accessibility for a Particular Field</a>
- Can I modify the Field Access Settings? How? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=modifying_field_access_settings.htm&language=en_US">Salesforce: Modifying Field Access Settings</a>

### As a Data Source

If the user who configured the template for the source system does not have at least *read only* permissions for the fields that are fetched, then an *InvalidFieldFault* API fault displays.

```
java.lang.RuntimeException: [InvalidFieldFault [ApiQueryFault [ApiFault  exceptionCode='INVALID_FIELD'
exceptionMessage='
Account.Phone, Account.Rating, Account.RecordTypeId, Account.ShippingCity
^
ERROR at Row:1:Column:486
No such column 'RecordTypeId' on entity 'Account'. If you are attempting to use a custom field, be sure to append the '__c' after the custom field name. Reference your WSDL or the describe call for the appropriate names.'
]
row='1'
column='486'
]
]
```

### As a Data Destination

There are no considerations with using Salesforce as a data destination.









# Run it!
Simple steps to get Salesforce and Database User Bidirectional Sync running.


## Running On Premises
In this section we help you run your template on your computer.


### Where to Download Anypoint Studio and the Mule Runtime
If you are a newcomer to Mule, here is where to get the tools.

+ [Download Anypoint Studio](https://www.mulesoft.com/platform/studio)
+ [Download Mule runtime](https://www.mulesoft.com/lp/dl/mule-esb-enterprise)


### Importing a Template into Studio
In Studio, click the Exchange X icon in the upper left of the taskbar, log in with your
Anypoint Platform credentials, search for the template, and click **Open**.


### Running on Studio
After you import your template into Anypoint Studio, follow these steps to run it:

+ Locate the properties file `mule.dev.properties`, in src/main/resources.
+ Complete all the properties required as per the examples in the "Properties to Configure" section.
+ Right click the template project folder.
+ Hover your mouse over `Run as`
+ Click `Mule Application (configure)`
+ Inside the dialog, select Environment and set the variable `mule.env` to the value `dev`
+ Click `Run`


### Running on Mule Standalone
Complete all properties in one of the property files, for example in mule.prod.properties and run your app with the corresponding environment variable. To follow the example, this is `mule.env=prod`. 


## Running on CloudHub
While creating your application on CloudHub (or you can do it later as a next step), go to Runtime Manager > Manage Application > Properties to set the environment variables listed in "Properties to Configure" as well as the **mule.env**.


### Deploying your Anypoint Template on CloudHub
Studio provides an easy way to deploy your template directly to CloudHub, for the specific steps to do so check this


## Properties to Configure
To use this template, configure properties (credentials, configurations, etc.) in the properties file or in CloudHub from Runtime Manager > Manage Application > Properties. The sections that follow list example values.
### Application Configuration
**Application Configuration**

+ scheduler.frequency `10000`
+ scheduler.startDelay `100`
+ page.size `200`

**Database Connector Configuration**

+ db.host `localhost`
+ db.port `3306`
+ db.user `user-name1`
+ db.password `user-password1`
+ db.name `dbname1`
+ db.watermark.default.expression `YESTERDAY`
+ db.integration.user.id `mule@localhost`


### Salesforce Connector Configuration

+ sfdc.username `polly.hedra@mail.com`
+ sfdc.password `Noctiluca123`
+ sfdc.securityToken `avsfwCUl7apQs56Xq2AKi3X`
+ sfdc.watermark.default.expression `2018-11-04T11:00:00.000Z`  
+ sfdc.integration.user.id `005n0000000T3QkAAK`

	**Note:** To find the correct *sfdc.integration.user.id* value, refer to example project **Salesforce Data Retrieval** in Anypoint Exchange.

This property is an important one, as it configures what should be the starting point for the synchronization. 

+ from.sfdc.to.db.profilesMap `['00r80000001CEiGAAW': '00e80000110CDfGMAX','00e30000000ifQyAAI': '00q70000000fiQyEZI']`  
+ from.db.to.sfdc.profilesMap `['00r80000001CEiGAAW': '00e80000110CDfGMAX','00e30000000ifQyAAI': '00q70000000fiQyEZI']`  

The meaning of these properties is defined in the second consideration in the previous section.

# API Calls
Salesforce imposes limits on the number of API Calls that can be made. Therefore calculating this amount may be an important factor to consider. The Anypoint Template calls to the API can be calculated using the formula:

***1 + X + X / 200***

Being ***X*** the number of Users to be synchronized on each run. 

The division by ***200*** is because, by default, users are gathered in groups of 200 for each Upsert API Call in the commit step. Also consider that these calls are executed repeatedly every polling cycle.	

For instance if 10 records are fetched from origin instance, then 12 API calls are made (1 + 10 + 1).


# Customize It!
This brief guide intends to give a high level idea of how this template is built and how you can change it according to your needs.
As Mule applications are based on XML files, this page describes the XML files used with this template.

More files are available such as test classes and Mule application files, but to keep it simple, we focus on these XML files:

* config.xml
* businessLogic.xml
* endpoints.xml
* errorHandling.xml


## config.xml
Configuration for connectors and configuration properties are set in this file. Even change the configuration here, all parameters that can be modified are in properties file, which is the recommended place to make your changes. However if you want to do core changes to the logic, you need to modify this file.

In the Studio visual editor, the properties are on the *Global Element* tab.


## businessLogic.xml
This file holds the functional aspect of the template (points 2. to 4. described in the [template overview](#templateoverview)). Its main component is a [*Batch job*][8], and it includes *steps* for both executing the synchronization from Salesforce to database, and the other way around.



## endpoints.xml
This file should contain every inbound and outbound endpoint of your integration app. It contains the application API.
In this template, this file contains three flows: 

The first **scheduler** flow contains a scheduler that periodically triggers the **querySalesforce** and **queryDatabase** flow based on the value of the syncState variable. The syncState variable is stored by using **SchedulerStatus** ObjectStore and updated before and after executing the batch job process.

The second **queryDatabase** flow one contains watermarking logic for querying the database for updated or created Users that meet the defined criteria in the query since the last polling. The last invocation timestamp is stored with the Objectstore component and updated after each database query.

The third **querySalesforce** flow contains watermarking logic for querying Salesforce for updated or created Users that meet the defined criteria in the query since the last polling. The last invocation timestamp is stored with the Objectstore component and updated after each Salesforce query.



## errorHandling.xml
This is the right place to handle how your integration reacts depending on the different exceptions. 
This file provides error handling that is referenced by the main flow in the business logic.




