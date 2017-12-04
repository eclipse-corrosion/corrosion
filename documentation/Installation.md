## Installing Corrosion

### Using Eclipse Marketplace
Download [Corrosion from the Eclipse Marketplace](https://marketplace.eclipse.org/content/corrosion-rust-edition-eclipse-ide)
For further instructions on how to install using Eclipse Marketplace, see [their tutorial](https://marketplace.eclipse.org/marketplace-client-intro?mpc_install=3835145)

### Using p2 repository
The p2 site: https://lucasbullen.github.io/corrosion/site/
 - Open Eclipse IDE
 - Open the Install New Software Wizard (Under the Help menu)
 - Enter https://lucasbullen.github.io/corrosion/site/ in the site field
 - Select Corrosion - Rust in Eclipse IDE and click Next
 - Wait for the dependencies to load
 - Press Next again
 - Accept the license and press Finish
 - Restart Eclipse

### Using Github
##### For contributors and testers, This will allow running the plugin within a child Eclipse to test and develop new features 
 - Download [Eclipse for Eclipse Contributors](https://www.eclipse.org/downloads/packages/eclipse-ide-eclipse-committers/oxygen2) or any version of Eclipse with the `Eclipse Plug-in Development Environment` package
 - Clone the repo: https://github.com/LucasBullen/corrosion
 - In the root of the repository, run `mvn clean verify` (You will need [Maven](http://maven.apache.org/))
 - Open the following projects in Eclipse:
   - org.eclipse.corrosion
   - target-platform
   - org.eclipse.corrosion.tests (If you intend on contributing)
 - Set the Target Platform
   - Preferences > Plug-in Development > Target Platform
   - Select the `corrosion` target defininition
 - Run the `org.eclipse.corrosion` project as an `Eclipse Application`


