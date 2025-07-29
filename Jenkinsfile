pipeline {
    // This top-level agent is for simple orchestration steps.
    // Tool-specific stages like testing and building will use their own dedicated agents.
    agent any

    environment {
        // Project-specific variables, accessible in all stages.
        DOCKER_IMAGE = 'zebra-prj'
        DOCKER_TAG = "${env.BUILD_ID}"
        CONTAINER_NAME = 'zebra-prj'
        APP_PORT = '8081'
        DOCKER_PATH = '/usr/bin/docker'
        // This is the correct network name for the Deploy stage, as discovered by our diagnostics.
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
                    // Explicitly run as the root user.
                    // This gives the process inside the container permission to use the mounted Docker socket.
                    // The arguments to mount the cache and the Docker socket are still required.
                    args '-u root -v /var/run/docker.sock:/var/run/docker.sock -v $HOME/.m2:/root/.m2'
                }
            }
            steps {
                // This property is still needed to solve the networking between the sibling containers.
                sh 'mvn test -Dtestcontainers.host.override=host.docker.internal'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'target/surefire-reports/**/*', fingerprint: true
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        // ====================== BUILD STAGE ======================
        stage('Build') {
            // This stage also uses a clean container, which is excellent practice.
            agent {
                docker {
                    image 'maven:3.8.3-openjdk-17'
                    args '-v $HOME/.m2:/root/.m2'
                }
            }
            steps {
                // We skip tests here because they were successfully run in the previous stage.
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
                    # The --network flag will now correctly use the 'jenkins_jenkins-network' name
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

    // ====================== POST-BUILD ACTIONS ======================
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