## Installing Corrosion

### Try the Eclipse IDE for Rust developers package

Directly get the **Eclipse IDE for Rust Developers** package at https://www.eclipse.org/downloads/eclipse-packages/ which contains a ready-to-use Eclipse IDE installation for Rust development. Java must be installed to run this IDE.

### Using Eclipse Marketplace to install Eclipse Corrosion in your Eclipse IDE
Download [Corrosion from the Eclipse Marketplace](https://marketplace.eclipse.org/content/corrosion-rust-edition-eclipse-ide)
For further instructions on how to install using Eclipse Marketplace, see [their tutorial](https://marketplace.eclipse.org/marketplace-client-intro?mpc_install=3835145)

### Using p2 repository to install Eclipse Corrosion in your Eclipse IDE
The p2 site: http://download.eclipse.org/releases/latest/
(or, if you're an early-adopter http://download.eclipse.org/corrosion/snapshots/ )

 - Open Eclipse IDE
 - Open the Install New Software Wizard (Under the Help menu)
 - Enter http://download.eclipse.org/releases/latest/ in the site field
 - Select Corrosion - Rust in Eclipse IDE and click Next
 - Wait for the dependencies to load
 - Press Next again
 - Accept the license and press Finish
 - Restart Eclipse

### Using Github
**For contributors and testers, This will allow running the plugin within a child Eclipse to test and develop new features**

 - Download [Eclipse for Eclipse Contributors](https://www.eclipse.org/downloads/packages/) or any version of Eclipse with the `Eclipse Plug-in Development Environment` package
 - Clone the repo: https://github.com/eclipse/corrosion
 - In the root of the repository, run `mvn clean verify` (You will need [Maven](http://maven.apache.org/))
   - If this does not execute successfully, try running `mvn clean verify -DskipTests` instead (See [Issue #104](https://github.com/eclipse/corrosion/issues/104))

For running Corrosion in a child Eclipse instance:

 - Open the following projects in Eclipse:
   - org.eclipse.corrosion
   - target-platform
   - org.eclipse.corrosion.tests (If you intend on contributing)
 - Ignore the errors, they are about to go away with the next step
 - Set the Target Platform
   - Open the `target-platform/target-platform.target` file with the Target Editor
   - Press the `Refresh` button
   - Wait for the reload to perform
   - In the top right corner, press the `Set as Active Target Platform` button
 - Run the `org.eclipse.corrosion` project as an `Eclipse Application`

For using a local p2 repository:

 - Follow the "Using p2 repository" above
 - Use `/{path_to_repository}/corrosion/repository/target/repository` as the p2 site


