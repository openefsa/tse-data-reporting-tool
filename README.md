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
Once the parent project has been created add the project and all the dependencies as "modules" into the pom.xml file as shown below (comment not used modules): 

	<project xmlns="http://maven.apache.org/POM/4.0.0"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
		<modelVersion>4.0.0</modelVersion>
		<groupId>openefsa</groupId>
		<artifactId>all-projects</artifactId>
		<version>${global.version}</version>
		<packaging>pom</packaging>

		<properties>
			<maven.compiler.source>1.8</maven.compiler.source>
			<maven.compiler.target>1.8</maven.compiler.target>
			<project.build.sourceEncoding>Cp1252</project.build.sourceEncoding>
			<global.version>1.0.0</global.version>
		</properties>

		<modules>

			<!-- catalogue browser modules -->
			<module>catalogue-browser</module>
			<module>catalogue-xml-to-xlsx</module>
			<module>open-xml-reader</module>

			<!-- tse modules -->
			<module>tse-data-reporting-tool</module>
			<module>efsa-rcl</module>
			<module>email-generator</module>

			<!-- cb/tse shared modules -->
			<module>dcf-webservice-framework</module>
			<module>exceptions-manager</module>
			<module>http-manager</module>
			<module>http-manager-gui</module>
			<module>progress-bar</module>
			<module>sql-script-executor</module>
			<module>version-manager</module>
			<module>window-size-save-restore</module>
			<module>zip-manager</module>

			<!-- spare modules -->
			<module>catalogue-xml-to-xlsx-gui</module>
			<module>external-installer</module>
			<module>internal-installer</module>
			<module>interpreting-and-checking-tool</module>
			<module>interpreting-and-checking-tool-2.0</module>
			<module>xlsx-new-to-old-mtx</module>

		</modules>

		<dependencyManagement>
			<dependencies>
				<dependency>
					<groupId>swt</groupId>
					<artifactId>swt</artifactId>
					<version>3.7.0.x64</version> <!-- <version>3.7.0.x64</version> -->
				</dependency>
				<dependency>
					<groupId>jface</groupId>
					<artifactId>jface</artifactId>
					<version>3.7.0</version>
				</dependency>
				<dependency>
					<groupId>org.apache.poi</groupId>
					<artifactId>poi</artifactId>
					<version>[4.0.0,)</version>
				</dependency>
				<dependency>
					<groupId>org.apache.poi</groupId>
					<artifactId>poi-ooxml</artifactId>
					<version>[4.0.0,)</version>
				</dependency>
				<dependency>
					<groupId>org.apache.poi</groupId>
					<artifactId>poi-ooxml-schemas</artifactId>
					<version>[4.0.0,)</version>
				</dependency>
				<dependency>
					<groupId>commons-io</groupId>
					<artifactId>commons-io</artifactId>
					<version>[2.6,)</version>
				</dependency>
				<dependency>
					<groupId>org.apache.logging.log4j</groupId>
					<artifactId>log4j-api</artifactId>
					<version>[2.12.1,)</version>
				</dependency>
				<dependency>
					<groupId>org.apache.logging.log4j</groupId>
					<artifactId>log4j-core</artifactId>
					<version>[2.12.1,)</version>
				</dependency>
				<dependency>
					<groupId>org.eclipse.core</groupId>
					<artifactId>org.eclipse.core.commands</artifactId>
					<version>[3.6.0,)</version>
				</dependency>
				<dependency>
					<groupId>org.junit.jupiter</groupId>
					<artifactId>junit-jupiter</artifactId>
					<version>[5.5.2,)</version>
				</dependency>
			</dependencies>
		</dependencyManagement>

		<build>
			<plugins>
				<plugin>
					<groupId>org.codehaus.mojo</groupId>
					<artifactId>license-maven-plugin</artifactId>
					<version>2.0.0</version>
					<executions>
						<execution>
							<id>add-third-party</id>
							<goals>
								<goal>add-third-party</goal>
							</goals>
							<configuration>
								<excludedGroups>openefsa</excludedGroups>
								<excludedGroups>swt</excludedGroups>
								<excludedGroups>jface</excludedGroups>
								<outputDirectory>${project.basedir}</outputDirectory>
							</configuration>
						</execution>
					</executions>
				</plugin>
			</plugins>
		</build>
	</project>
	
Next, close the IDE and extract all the zip packets inside the parent project.
At this stage you can simply open the IDE and import back the parent project which will automatically import also the TSE tool and all its dependencies.

_Please note that the "SWT.jar" and the "Jface.jar" libraries must be downloaded and installed manually in the Maven local repository since are custom versions used in the tool ((install 3rd party jars)[https://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html]). 
Download the exact version by checking the TSE pom.xml file._

### Notes for developers
Please note that the "compact", "config" and "picklists" folders are used by the tool and therefore errors occur if missing.

