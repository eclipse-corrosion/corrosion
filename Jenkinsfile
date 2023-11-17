pipeline {
	agent {
  		label "centos-8"
	}
	options {
		buildDiscarder(logRotator(numToKeepStr:'10'))
		timeout(time: 30, unit: 'MINUTES')
	}
	environment {
		PATH = "$HOME/.local/bin:$HOME/.cargo/bin/:$PATH"
		USER = "jenkins"
	}
	tools {
        jdk 'openjdk-jdk17-latest'
    }
	stages {
		stage('Prepare') {
			steps {
				sh 'org.eclipse.corrosion/scripts/rustup-init.sh -y'
				sh 'rustup install stable-x86_64-unknown-linux-gnu'
				sh 'rustup default stable-x86_64-unknown-linux-gnu'
				sh 'mkdir -p ~/.local/bin'
				sh 'echo $PATH'
			}
		}
		stage('Build') {
			steps {
				sh 'cargo --version'
				sh 'rustup show'
				sh 'rust-gdb --version'
				wrap([$class: 'Xvnc', useXauthority: true]) {
					sh './mvnw -Dmaven.repo.local=$WORKSPACE/.m2 clean verify -Dmaven.test.error.ignore=true -Dmaven.test.failure.ignore=true -Dtycho.showEclipseLog=true -Psign -Dsurefire.timeout=1800'
				}
				sh 'rust-analyzer --version'
			}
			post {
				always {
					sh 'which rustup'
					sh 'which rust-analyzer'
					sh 'rust-analyzer --version'
					junit '*/target/surefire-reports/TEST-*.xml'
					archiveArtifacts artifacts: '*/target/work/data/.metadata/.log'
				}
			}
		}
		stage('Deploy') {
			when {
				branch 'master'
				// TODO deploy all branch from Eclipse.org Git repo
			}
			steps {
				// TODO compute the target URL (snapshots) according to branch name (0.5-snapshots...)
				sshagent ( ['projects-storage.eclipse.org-bot-ssh']) {
					sh '''
						ssh genie.corrosion@projects-storage.eclipse.org rm -rf /home/data/httpd/download.eclipse.org/corrosion/snapshots
						ssh genie.corrosion@projects-storage.eclipse.org mkdir -p /home/data/httpd/download.eclipse.org/corrosion/snapshots
						scp -r repository/target/repository/* genie.corrosion@projects-storage.eclipse.org:/home/data/httpd/download.eclipse.org/corrosion/snapshots
						ssh genie.corrosion@projects-storage.eclipse.org mkdir -p /home/data/httpd/download.eclipse.org/corrosion/snapshots/products
						scp -r repository/target/products/*.tar.gz genie.corrosion@projects-storage.eclipse.org:/home/data/httpd/download.eclipse.org/corrosion/snapshots/products
						scp -r repository/target/products/*.zip genie.corrosion@projects-storage.eclipse.org:/home/data/httpd/download.eclipse.org/corrosion/snapshots/products
					'''
				}
			}
		}
	}
}
