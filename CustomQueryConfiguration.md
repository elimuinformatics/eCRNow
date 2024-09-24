# 1. Introduction to Custom Query and Default Query #

The eCRNow App invokes FHIR APIs to retrieve data from the EHR to examine, evaluate and create the eICR.
The APIs used by the eCRNow App are externalized and can be configured.

By default a query (API request) is provided for each data element to be extracted from the EHR as part of the ERSD file.
This query is called a "DEFAULT QUERY".

An example to extract conditions is shown below:

										"id": "conditions",
                                        "extension":
                                        [
                                            {
                                                "url": "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-fhirquerypattern-extension",
                                                "valueString": "Condition?patient=Patient/{{context.patientId}}"
                                            }
                                        ],
                                        
The eCRNow App retrieves the Condition data from the EHR and populates the conditions list using the query provided in the valueString.

However if you want to optimize the query and only retrieve Conditions which are active, the following query would be better suited. 

conditions=/Condition?patient=Patient/{{context.patientId}}&clinical-status=http://terminology.hl7.org/CodeSystem/condition-clinical|active

In this example, we are retrieving only active Condition Resources and populating the variable called conditions.

This kind of configured query is called a "CUSTOM QUERY".

The custom queries are helpful to avoid large amounts of data and limit the number of API calls. For example, you may include medication resources
with all MedicationRequest queries, or you can filter data by dates and times etc. 

# 2. Context Variables: #

In order to make custom queries work the following context variables are supported currently.

patientId of the launchPatient.
encounterId for which the patient was launched in the eCRNow App.
encounterStartDate for timeboxing the start point for the data.
encounterEndDate for timeboxing the end point for the data.

The following are some examples of queries with these context variables.

patient=/Patient/{{context.patientId}}
encounter=/Encounter/{{context.encounterId}}
conditions=/Condition?patient=Patient/{{context.patientId}}&clinical-status=http://terminology.hl7.org/CodeSystem/condition-clinical|active
labTests=/Observation?patient=Patient/{{context.patientId}}&category=http://terminology.hl7.org/CodeSystem/observation-category|laboratory&date=ge{{context.encounterStartDate}

# 3. Configuring the custom queries for ERSDv2 # 

In order to specify the custom queries, the following variable is used

custom-query.directory

The directory is the location where custom query files can be placed.
One custom query file can be created for each ERSD file.

The file naming convention should conform to the following:

[Bundle Id of the ERSD file ]-[Bundle.meta.versionId].queries

For e.g for the following custom query file should be created for the Bundle info shown below.

rctc-release-2022-07-13-Bundle-rctc-1.queries

Sample_ERSD.json
{
    "resourceType": "Bundle",
    "id": "rctc-release-2022-07-13-Bundle-rctc",  ### This is the Bundle Id
    "meta":
    {
        "versionId": "1",                         ### This is the version Id
        "lastUpdated": "2022-07-18T21:32:25.000+00:00",
        "source": "#fsOSEUhB1HVhhCy9"
    },
    ...
}

# 4. Configuring the custom queries for ERSDv3 # 

In order to specify the custom queries, the following variable is used

custom-query.directory

The directory is the location where custom query files can be placed.
One custom query file can be created for each ERSD file.

The file naming convention should conform to the following:

[Bundle Id of the ERSD file ]-[SpecificationLibrary.meta.versionId|SpecificationLibrary.version].queries

## Library.meta.versionId ##

The Library resource used is the SpecificationLibrary present in the ERSD.
The SpecificationLibrary is identified using hte Meta.profile element which should be equal to "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-specification-library"
The Library.meta.versionId is "3.0.0" below.

Sample SpecificationLibrary resource from ERSD: 
{
"fullUrl": "http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary",
            "resource": {
                "description": "Defines the asset-collection library containing the US Public Health specification assets.",
                "experimental": false,
                "id": "SpecificationLibrary",
                "meta": {
                    "profile": [
                        "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-specification-library"
                    ],
                    "versionId": "3.0.0"     ### This is the version Id
                },
}

## Library.version ##

The Library resource used is the SpecificationLibrary present in the ERSD.
The SpecificationLibrary is identified using hte Meta.profile element which should be equal to "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-specification-library"
The Library.version is "3.0.0" below.

Sample SpecificationLibrary resource from ERSD: 
{
"fullUrl": "http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary",
            "resource": {
                "description": "Defines the asset-collection library containing the US Public Health specification assets.",
                "experimental": false,
                "id": "SpecificationLibrary",
                "meta": {
                    "profile": [
                        "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-specification-library"
                    ]
                },
		"version" : "3.0.0"
}

## Bundle Id ##

The Bundle Id for the ERSD json below is "rctc-release-2022-07-13-Bundle-rctc"

Sample_ERSD.json
{
    "resourceType": "Bundle",
    "id": "rctc-release-2022-07-13-Bundle-rctc",  ### This is the Bundle Id
    "meta":
    {
        "versionId": "1",                         
        "lastUpdated": "2022-07-18T21:32:25.000+00:00",
        "source": "#fsOSEUhB1HVhhCy9"
    },
    ...
}


For the above ERSD and Library resource found in the ERSD, the following custom query file should be created.

rctc-release-2022-07-13-Bundle-rctc-3.0.0.queries



# 5. Leveraging _include parameter in custom queries to improve performance

In order to trigger and create an eICR, the eCRNow App makes FHIR queries to the EHR and receives data for processing.
In doing so for certain types of Resources, related resource data has to be extracted from the EHR. These related resources are 
extracted using the Resource Id, after retrieving the references to the resources from the initial query.

For e.g the eCRNow App makes an initial query to retrieve the Encounter Resource using the query: /Encounter?patient=123
Subsequently, the eCRNow App uses the data from the Encounter resource (namely the Practitioner references,
location references, organization references) and retrieves them individually using the id of the resource.

This pattern above is in-efficient and can be optimized using custom queries by the following:

/Encounter?patient=123&_include=Location,Organization,Practitioner reducing the #of calls between the eCRNowApp and the EHR.

The same pattern can be used to fetch

DiagnosticReports with Observation results
Medication[Request,Administration,Dispense] with Medication references. 

It is strongly encouraged to use the customization capabilities to reduce the # of the queries between the eCRNow App and the EHR.


