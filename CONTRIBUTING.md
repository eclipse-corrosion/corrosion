# Contributing to Corrosion

## Reporting Issues
Issue reports and feature requests are always appreciated and are made in the [Issues tab](https://github.com/eclipse/corrosion/issues)

## Code for Corrosion

### Building locally

Local build happens with a simple `mvn clean verify`. Main build output is a p2 repository that you can find in `repository/target/repository`. You can use it to install and test the built artifacts in a working Eclipse IDE.

### Modify Corrosion in the Eclipse IDE

For running Corrosion in a child Eclipse instance:

 - Run a first preliminary local build (see above)
 - Use [Eclipse for Eclipse Contributors](https://www.eclipse.org/downloads/packages/) or any version of the Eclipse IDE with the `Eclipse Plug-in Development Environment` plugins installed..
 - Open the following projects in the Eclipse IDE:
   - `org.eclipse.corrosion`
   - `target-platform`
   - `org.eclipse.corrosion.tests`
 - (Ignore the errors, they are about to go away with the next step)
 - Set the Target Platform
   - Open the `target-platform/target-platform.target` file with the Target Editor
   - Press the `Refresh` button and wait for completion
   - In the top right corner, press the `Set as Active Target Platform` button
 - Run the `org.eclipse.corrosion` project as an `Eclipse Application` (Right-click on project > `Run As` > `Eclipse Application`; or using the Launch Configuration dialog)

## Making Pull Requests

To keep the commit history clean and navigable, PR are limited to a single commit. If your PR has multiple commits that all work together, they should be squashed into a single commit. If you believe that the different commits are too unrelated to be squashed together, then they should be put into multiple PRs to allow single feature PRs.

Corrosion is an Eclipse project and all contributors must do the following before having a pull request merged:
 - Sign the Eclipse Contributor Agreement (ECA)
	- Create an Eclipse Account: https://accounts.eclipse.org/user/register
	- Sign the ECA: https://www.eclipse.org/contribute/cla
 - Add the Signed-off-by footer to your commits
	- Footer of all commits should contain: `Signed-off-by: @{name} <@{email}>`, see [here for an example commit](https://github.com/eclipse/corrosion/commit/09f4fa5d771bca3de6f4e5454ad324a517fc42bf)
	- Either add manually or use `git commit -s`

