pipeline {
    agent {
        docker {
            image 'maven:3.9-eclipse-temurin-17'
            args '-v /var/run/docker.sock:/var/run/docker.sock' // give Maven container access to host Docker
        }
    }

    environment {
        MAVEN_OPTS = "-Dmaven.repo.local=.m2/repository"
    }

    stages {
        stage('Print Docker Info') {
            steps {
                sh 'docker info'
                sh 'docker ps -a'
            }
        }

        stage('Build and Test') {
            steps {
                sh 'mvn clean verify -Dspring.profiles.active=test'
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            junit 'target/surefire-reports/*.xml'
        }
    }
}
