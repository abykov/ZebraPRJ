pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'zebra-prj'
        DOCKER_TAG = "${env.BUILD_ID}"
        CONTAINER_NAME = 'zebra-prj'
        APP_PORT = '8081'
        DOCKER_PATH = '/usr/bin/docker'
        DOCKER_NETWORK = 'jenkins_jenkins-network'
    }

    stages {

        stage('Checkout') {
            steps {
                cleanWs()
                checkout scm
            }
        }

        // ====================== TESTING STAGE ======================
        stage('Run Tests') {
            agent {
                docker {
                    image 'maven:3.8.3-openjdk-17'
                    args """
                        -u root \
                        -v /var/run/docker.sock:/var/run/docker.sock \
                        -v $HOME/.m2:/root/.m2 \
                        --network=${DOCKER_NETWORK} \
                        -e TESTCONTAINERS_RYUK_DISABLED=false \
                        -e TESTCONTAINERS_CHECKS_DISABLE=true \
                        -e TESTCONTAINERS_NETWORK=${DOCKER_NETWORK}
                    """
                }
            }
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        // ====================== BUILD STAGE ======================
        stage('Build') {
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

        // ====================== BUILD DOCKER IMAGE STAGE ======================
        stage('Build Docker Image') {
            steps {
                unstash 'jar-artifact'
                sh """
                    ${DOCKER_PATH} build --no-cache -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                    ${DOCKER_PATH} tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest
                """
            }
        }

        // ====================== DEPLOY STAGE ======================
        stage('Deploy') {
            steps {
                sh """
                    ${DOCKER_PATH} stop ${CONTAINER_NAME} || true
                    ${DOCKER_PATH} rm ${CONTAINER_NAME} || true
                """
                sh """
                    ${DOCKER_PATH} run -d \\
                        -p ${APP_PORT}:${APP_PORT} \\
                        --name ${CONTAINER_NAME} \\
                        --network ${DOCKER_NETWORK} \\
                        -e SPRING_PROFILES_ACTIVE=docker \\
                        ${DOCKER_IMAGE}:latest
                """
            }
        }

        // ====================== VERIFY STAGE ======================
        stage('Verify') {
            steps {
                sleep(time: 15, unit: 'SECONDS')
                sh """
                    docker ps --filter name=${CONTAINER_NAME}
                    curl -f http://localhost:${APP_PORT}/hello || echo "Service not responding"
                """
            }
        }
    }

    post {
        always {
            echo "Pipeline ${currentBuild.fullDisplayName} completed with status: ${currentBuild.currentResult}"
        }
        success {
            echo "Pipeline succeeded! Application deployed successfully."
        }
        failure {
            echo "Pipeline failed. Please review the logs."
        }
    }
}
