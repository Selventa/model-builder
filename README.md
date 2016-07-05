SDP Model Builder
=================

Cytoscape 3.0+ plugin to interface with an instance of the Selventa Development Platform (SDP) to explore and analyze biological networks (SDP models).

### Functionality

- View, edit, and save models.
- Import paths found between two sets of nodes in an SDP knowledge network.
- Overlay RCR (Reverse Causal Reasoning) datasets on Cytoscape networks.
- Overlay Comparisons on Cytoscape networks.
- View, edit, and delete Sets.

### Build process

This app is built using Maven and produces a single JAR (OSGi-enabled).

Requirements:

- Java 8
- Maven 3+

Installation steps:

- git clone https://github.com/Selventa/model-builder.git
- cd model-builder
- mvn clean package -DskipTests

### Install process

Install from the AppStore.

- Open Cytoscape then Apps → App Manager and search for BEL Navigator.
- Install BEL Navigator App.

Install from source.

- Follow "Build process" section above.
- Open Cytoscape then Apps → App Manager → Install from File...
- Locate target/modelbuilder-VERSION.jar and click Open.

### Development branches

*experimental* branch

- New/Unstable development

*master* branch

- Stable development for release.
- Releases are tagged here.

### Releases

To create a release of *SDP Model Builder* follow these steps:

1. Build a single OSGi bundle jar.

  `mvn clean package -DskipTests`

2. Got to the [Cytoscape App Store][Cytoscape App Store] and [submit][submit] a 3.0 application.

3. Locate *target/modelbuilder-VERSION.jar* and upload the file.
4. Receive confirmation from Cytoscape.
5. Update details on the App Store's project page.

[Cytoscape App Store]: http://apps.cytoscape.org/
[submit]:              http://apps.cytoscape.org/submit_app/
