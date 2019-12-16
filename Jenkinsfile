pipeline {
	agent any
	options {
		buildDiscarder(logRotator(numToKeepStr:'10'))
	}
	environment {
		PATH = "$HOME/.cargo/bin/:$PATH"
	}
	tools {
        jdk 'jdk1.8.0-latest'
    }
	stages {
		stage('Prepare') {
			steps {
				git url: 'https://github.com/eclipse/corrosion.git'
				cleanWs()
				checkout scm
				sh 'rustup install stable-x86_64-unknown-linux-gnu'
				sh 'rustup default stable-x86_64-unknown-linux-gnu'
				sh 'rustup component add rls'
				sh 'echo $PATH'
			}
		}
		stage('Build') {
			steps {
				sh 'cargo --version'
				sh 'rustup show'
				wrap([$class: 'Xvnc', useXauthority: true]) {
					sh './mvnw -Dmaven.repo.local=$WORKSPACE/.m2 clean verify -Dmaven.test.error.ignore=true -Dmaven.test.failure.ignore=true -PpackAndSign'
				}
			}
			post {
				always {
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
				sh 'rm -rf /home/data/httpd/download.eclipse.org/corrosion/snapshots'
				sh 'mkdir -p /home/data/httpd/download.eclipse.org/corrosion/snapshots'
				sh 'cp -r repository/target/repository/* /home/data/httpd/download.eclipse.org/corrosion/snapshots'
				sh 'cp repository/target/repository-*-SNAPSHOT.zip /home/data/httpd/download.eclipse.org/corrosion/snapshots/repository.zip'
			}
		}
	}
}
