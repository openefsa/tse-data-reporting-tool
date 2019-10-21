<p align="center">
	<img src="http://www.efsa.europa.eu/profiles/efsa/themes/responsive_efsa/logo.png" alt="European Food Safety Authority"/>
</p>

# Transmissible spongiform encephalopathies tool
The TSE data reporting tool is an open source Java client tool developed for the members of the Scientific Network for Zoonoses monitoring. The tool allows countries to submit and edit their data and automatically upload them into the EFSA Data Collection Framework (DCF) as XML data files.

<p align="center">
    <img src="src/main/resources/icons/TSE_Splash.bmp" alt="TSE icon"/>
</p>

## Dependencies
All project dependencies are listed in the [pom.xml](https://github.com/openefsa/tse-data-reporting-tool/blob/master/pom.xml) file.

## Import the project
In order to import the project correctly into the integrated development environment (e.g. Eclipse), it is necessary to download the TSE together with all its dependencies.
The TSE and all its dependencies are based on the concept of "project object model" and hence Apache Maven is used for the specific purpose.
In order to correctly import the project into the IDE it is firstly required to create a parent POM Maven project (check the following [link](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html) for further information). 
Once the parent project has been created add the project and all the dependencies as "modules" into the pom.xml file as shown below: 

	<modules>

		<!-- tse modules -->
		<module>tse-data-reporting-tool</module>
		<module>efsa-rcl</module>
		<module>email-generator</module>
		<module>dcf-webservice-framework</module>
		<module>exceptions-manager</module>
		<module>http-manager</module>
		<module>http-manager-gui</module>
		<module>progress-bar</module>
		<module>sql-script-executor</module>
		<module>version-manager</module>
		<module>window-size-save-restore</module>
		<module>zip-manager</module>
		
	</modules>
	
Next, close the IDE and extract all the zip packets inside the parent project.
At this stage you can simply open the IDE and import back the parent project which will automatically import also the TSE tool and all its dependencies.

_Please note that the "SWT.jar" and the "Jface.jar" libraries must be downloaded and installed manually in the Maven local repository since are custom versions used in the tool ((install 3rd party jars)[https://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html]). 
Download the exact version by checking the TSE pom.xml file._

### Notes for developers
Please note that the "compact", "config" and "picklists" folders are used by the tool and therefore errors occur if missing.

