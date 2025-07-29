pipeline {
    // This top-level agent is only for simple orchestration steps.
    agent any

    environment {
        DOCKER_IMAGE = 'zebra-prj'
        DOCKER_TAG = "${env.BUILD_ID}"
        CONTAINER_NAME = 'zebra-prj'
        APP_PORT = '8081'
        DOCKER_PATH = '/usr/bin/docker'
        // This is the correct network name for the final Deploy stage.
        DOCKER_NETWORK = 'jenkins_jenkins-network'
    }

    stages {
        stage('Checkout') {
            steps {
                cleanWs()
                checkout scm
            }
        }

        // ====================== ТЕСТИРОВАНИЕ ======================
        stage('Run Tests') {
            // Use a dedicated, clean Maven+Java container for the tests.
            agent {
                docker {
                    image 'maven:3.8.3-openjdk-17'
                    // THIS IS THE FIX:
                    // 1. Mount the Maven cache.
                    // 2. Mount the Docker socket to allow Testcontainers to work.
                    // The JAVA_HOME inside this official image is set correctly by default.
                    args '-v $HOME/.m2:/root/.m2 -v /var/run/docker.sock:/var/run/docker.sock'
                }
            }
            steps {
                // This command tells Testcontainers the correct address for the Docker host,
                // which is required when running inside a sibling container.
                sh 'mvn test -Dtestcontainers.host.override=host.docker.internal'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'target/surefire-reports/**/*', fingerprint: true
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        // ====================== СБОРКА ======================
        stage('Build') {
            // This stage also uses a clean container, which is excellent practice.
            agent {
                docker {
                    image 'maven:3.8.3-openjdk-17'
                    args '-v $HOME/.m2:/root/.m2'
                }
            }
            steps {
                sh 'mvn clean package -DskipTests'
                stash name: 'jar-artifact', includes: 'target/ZebraPRJ-0.0.1-SNAPSHOT.jar'
            }
        }

        // ... All other stages (Build Docker Image, Deploy, Verify) are correct and remain the same ...
        stage('Build Docker Image') { /* ... */ }
        stage('Deploy') { /* ... */ }
        stage('Verify') { /* ... */ }
    }

    post {
        always {
            echo "Pipeline ${currentBuild.fullDisplayName} completed with status: ${currentBuild.currentResult}"
        }
    }
}