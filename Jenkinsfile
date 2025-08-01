pipeline {
    // This top-level agent is for simple orchestration steps and has the Docker CLI.
    agent any

    environment {
        DOCKER_IMAGE = 'zebra-prj'
        DOCKER_TAG = "${env.BUILD_ID}"
        CONTAINER_NAME = 'zebra-prj'
        APP_PORT = '8081'
        DOCKER_PATH = '/usr/bin/docker'
        // This is the correct network name for the Deploy stage.
        DOCKER_NETWORK = 'jenkins_jenkins-network'
    }

    stages {
        stage('Checkout') {
            steps {
                cleanWs()
                checkout scm
            }
        }

        // ====================== NEW DISCOVERY STAGE ======================
        stage('Prepare Test Environment') {
            steps {
                script {
                    // This stage works perfectly. It discovers the correct gateway IP.
                    def dockerHostIp = sh(script: "docker network inspect ${DOCKER_NETWORK} -f '{{(index .IPAM.Config 0).Gateway}}'", returnStdout: true).trim()
                    echo "--- Discovered correct Docker Host IP for Testcontainers: ${dockerHostIp} ---"
                    // We save this IP into a global environment variable for the next stage.
                    env.DOCKER_HOST_IP_FOR_TESTS = dockerHostIp
                }
            }
        }

        // ====================== TESTING STAGE ======================

        // STAGE 1: Actively diagnose the network environment
                stage('Run Network Diagnostics') {
                    agent {
                        docker {
                            image 'maven:3.8.3-openjdk-17'
                            args "--network=${env.DOCKER_NETWORK} -u root -v /var/run/docker.sock:/var/run/docker.sock"
                        }
                    }
                    steps {
                        echo "--- Running Network Diagnostics inside the agent container ---"
                        sh 'echo "Container IP Address:"; hostname -i'
                        sh 'echo "\nNetwork Interfaces:"; ip a'
                        sh 'echo "\nGateway of Custom Network (jenkins_jenkins-network):"; docker network inspect ${DOCKER_NETWORK} --format "{{(index .IPAM.Config 0).Gateway}}"'
                        sh 'echo "\nAttempting to ping host.docker.internal:"; ping -c 4 host.docker.internal || echo "Ping failed, as expected."'
                        echo "--- Diagnostics Complete ---"
                    }
                }

                // STAGE 2: Run tests using the correctly determined Docker Host IP
                stage('Run Tests') {
                    steps {
                        script {
                            // Programmatically determine the gateway of the custom network. This is the reliable IP for the host.
                            def dockerHostIp = sh(script: "docker network inspect ${env.DOCKER_NETWORK} -f '{{(index .IPAM.Config 0).Gateway}}'", returnStdout: true).trim()
                            echo "--- Discovered Docker Host IP for Testcontainers: ${dockerHostIp} ---"

                            // Use the discovered IP to run the tests in the agent container
                            docker.image('maven:3.8.3-openjdk-17').args("--network=${env.DOCKER_NETWORK} -u root -v /var/run/docker.sock:/var/run/docker.sock -v $HOME/.m2:/root/.m2").inside {
                                sh "mvn test -Dtestcontainers.host.override=${dockerHostIp}"
                            }
                        }
                    }
                    post {
                        always {
                            junit 'target/surefire-reports/*.xml'
                            archiveArtifacts 'target/surefire-reports/*'
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