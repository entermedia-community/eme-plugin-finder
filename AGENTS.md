# AGENTS.md

## How to customize EME

### Overview

EME-LIB is a custom Java base framework that is deployable using a script called resources/bin/eme.sh. The embded Tomcat library runs on https://localhost:8080 by default. It is expected NGINX will proxy the port 8080 in a production environmernt. 

The two most important folders are:

1. EME-LIB: typically this is checked out into the users git folder from github: https://github.com/entermedia-community/eme-lib

2. Website can be stored in any folder name. By running: eme-lib/resources/bin/eme.sh init /usr/share/eme/MyServerName 


### Prerequisites

- Install java and dependencies. See .agents/SKILL.md 

### Codebase map

- `plugins` Contains extention points that are mounted in the web server using symbolic links  
- `plugins/eme` This is the main entry point for the web site and theme of the site. It can be reached by going to http://localhost:8080/eme
- `plugins/system/lib` The main jar libraries that the system uses to process web pages etc
- `plugins/system/code` This handles html web page requests and user security
- `plugins/finder/html/find` This is the app contains all the components needed to organize and media library and for data entry
- `plugins/community/html/default` This is a community template to help render projects and goal tracking
- `plugins/finder/code` Contains the bulk of the java code that controls the system

### Web app conventions

VSS has a launcher that will build the java source code automatically into MyServerName/bin folder that tomcat uses to launch. 

- FallBackDirectory Is the idea that when a file it needed and is not found that it will look in the parent folder. This effectively gives EME and html object inheritance and the idea of parent child html folders. The main fallback in the app is /eme -> /finder/find  -> /community/default. All HTML files will be found within this fallback tree
- _site.xconf This is an XML text file located in a folder. The properties are applied to any file path in that folder and sub-folders.
- .xconf Each page can have its own xconf file that will be merged with the _site.xconf in it's folder on any parent folders
- catalog/data All the default tables, fields, views that are needed for data entry and searching the embeded elastic database
- layouts A layout file is an html file that can be layered on top of one another like an onion. Each level of layout renders some html. Finally the original page that was requested is rendered in the middle of the final layout. Layouts provide decorations such as side menus or headers/footers. Layouts are specified by including an <inner-layout /> tag within the folder or files .xconf file

### Package and feature boundaries

- There are no external dependecies outside of eme-lib
- plugins/openedit Is used to enable a small toolbar on the site. This toolbar will allow users to view and edit the layout and include structure
- plugins/manager folder that is used when users want to have more than one database. It is not needed for normal operation
- plugin/components - This folder is used by web pages to find common javascript libraries. The eme app users a /eme/components -> /components fallback to add additional javascript it needs.  
- plugin/mediadb Contains the JSON REST APi services. All available API calls can be found in eme-lib/plugins/catalog/html/data/lists/endpoint/*.xml files
- Each eme-lib can spawn multiple Websites using the eme.sh command. A website is where the user will customize his site. The website will contain his database
- Website/data This is where any database and file uploads will be saved
- Website/webapp This is where the plugins will be linked and used by tomcat to render the html of the website to the user directly

Placement decision tree:

1. If the change is a existing html file then the browser will see the change on reload
2. If the change is editing an xconf file then the page cache of the server needs to be cleared 
3. If an html of xconf file is added or removed the page cache must be cleared or server restarted


### Database

Elasticsearch 2.4.6 is embedded into the server. It is auto configured based on the Website/webapp/WEB-INF/node.xml and plugins/catalog/html/configuration/*.xml files. The elastic index is stored in Website/WEB-INF/elastic and is auto created on bootup

### Environment


- `JAVA_HOME` Used by eme.sh
- `EME_LIB` Optionally used by eme.sh start. If not set will use in the nearest eme-lib sibling folder of the website 
- AI Server Security keys are stored in /usr/share/eme-lib/plugins/catalog/html/data/lists/aiserver/*.xml

### Common commands

| Task | Command |
|------|---------|
|eme|init /usr/share/myserver|
|eme|start /usr/share/myserver|

### Gotchas

- When running within VSS we recommend using the Java debugger to start and stop your WebSite
