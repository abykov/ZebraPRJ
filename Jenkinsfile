pipeline {
    agent any // Всё выполняется на хостовой машине

    environment {
        DOCKER_IMAGE = 'zebra-prj'
        DOCKER_TAG = "${env.BUILD_ID}"
        CONTAINER_NAME = 'zebra-prj'
        APP_PORT = '8081'
    }

    stages {
        stage('Setup') {
            steps {
                sh '''
                    echo "=== Проверка окружения ==="
                    java -version
                    mvn -v
                    docker --version
                '''
            }
        }

        stage('Build & Test') {
            steps {
                sh 'mvn clean package' // Собирает и тестирует
                archiveArtifacts artifacts: 'target/*.jar'
                junit 'target/surefire-reports/*.xml'
            }
        }

        stage('Deploy') {
            steps {
                sh """
                    docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                    docker stop ${CONTAINER_NAME} || true
                    docker rm ${CONTAINER_NAME} || true
                    docker run -d -p ${APP_PORT}:${APP_PORT} --name ${CONTAINER_NAME} ${DOCKER_IMAGE}:latest
                """
            }
        }
    }
}