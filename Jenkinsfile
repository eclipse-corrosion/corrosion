pipeline {
	agent any
	options {
		buildDiscarder(logRotator(numToKeepStr:'10'))
	}
	environment {
		PATH = "$HOME/.cargo/bin/:$PATH"
	}
	tools {
        maven 'apache-maven-3.0.5'
        jdk 'jdk1.8.0-latest'
    }
	stages {
		stage('Prepare') {
			steps {
				git url: 'https://github.com/eclipse/corrosion.git'
				cleanWs()
				checkout scm
				sh 'echo $PATH'
			}
		}
		stage('Build') {
			steps {
				sh 'cargo --version'
				sh 'rustup show'
				wrap([$class: 'Xvnc', useXauthority: true]) {
					sh 'mvn clean verify -Dmaven.test.error.ignore=true -Dmaven.test.failure.ignore=true'
				}
			}
			post {
				always {
					junit '*/target/surefire-reports/TEST-*.xml'
					archiveArtifacts artifacts: '*/target/work/data/.metadata/.log'
				}
			}
		}
		stage('SonarQube analysis') {
			when {
			 branch 'master'
			}
			steps {
				withSonarQubeEnv('Eclipse Sonar') {
					sh 'mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.3.0.603:sonar -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.jdbc.url=$SONAR_JDBC_URL -Dsonar.jdbc.username=$SONAR_JDBC_USERNAME -Dsonar.jdbc.password=$SONAR_JDBC_PASSWORD'
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