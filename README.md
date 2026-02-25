UMLS Terminology Server
=========================

This is a generic terminology server back end project.

This project hosts a basic UI that calls a set of REST APIs built around 
a UMLS data model. The API is fully documented with Swagger (http://swagger.io)


A reference deployment of the system exists here:
https://umls.terminology.tools/

Project Structure
-----------------
This is a flattened, single-module web application project.

* `src/main/java`: Application source code, encompassing models, JPA entities, REST services, and algorithms.
* `src/main/resources`: Configuration properties and default data.
* `src/main/webapp`: The basic UI and JavaScript resources.
* `src/test/java`: Unit and integration tests (classes ending in `*IT.java`).
* `src/test/resources`: Test configurations.

Auxiliary paths:
* `admin`: Admin tools (as Maven plugins/POMs).
* `config`: Sample config files and data for environments.

Documentation
-------------
Find comprehensive documentation here: http://wiki.terminology.tools/confluence/display/UTS/UMLS+Terminology+Server+Home

License
-------
See the included LICENSE.txt file.

Database Setup (MySQL)
----------------------
1. Install and start MySQL.
2. Create the main database and configure your connection properties.
3. The JPA framework (Hibernate) is configured to automatically create/update the schema during startup if `hibernate.hbm2ddl.auto` is configured correctly, or you can use the admin tools to load the schema.

Running the Application
-----------------------
This project generates a `.war` file intended for deployment onto **Tomcat 10**.

### IntelliJ IDEA (Smart Tomcat)
1. Install the "Smart Tomcat" plugin.
2. Add a new Run/Debug Configuration -> "Smart Tomcat".
3. Set the Tomcat Server path to your downloaded Tomcat 10 installation.
4. Set "Context path" to `/` or `/umls-server-rest`.
5. Set "Deployment Directory" to `src/main/webapp` (or your `target` exploded war directory).
6. Run the configuration to start the server.

### Eclipse (Tomcat Plugin)
1. Add Tomcat 10 to your Servers tab (Window > Preferences > Server > Runtime Environments).
2. Right-click the project -> Properties -> Project Facets. Ensure "Dynamic Web Module" is checked.
3. Right-click the server in the Servers tab -> "Add and Remove..." -> add this project.
4. Start the server from the Servers tab.