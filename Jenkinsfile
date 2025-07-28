pipeline {
    agent any

    environment {
        // Set JAVA_HOME and PATH globally. This will be inherited by ALL stages.
        JAVA_HOME = "/usr/lib/jvm/java-17-openjdk-arm64"
        PATH = "${env.JAVA_HOME}/bin:${env.PATH}"

        DOCKER_IMAGE = 'zebra-prj'       // Название Docker образа
        DOCKER_TAG = "${env.BUILD_ID}"   // Тег образа (по номеру сборки)
        CONTAINER_NAME = 'zebra-prj'     // Имя контейнера
        APP_PORT = '8081'                // Порт приложения
        DOCKER_PATH = '/usr/bin/docker'
        DOCKER_NETWORK = 'jenkins-network' // Имя сети из docker-compose
    }

    stages {
        stage('Setup') {
            steps {
                            sh '''
                                echo "=== Проверка окружения ==="
                                echo "1. Docker Version:"
                                docker --version
                                echo "2. Java Version (from global environment):"
                                java -version
                                echo "3. Maven Version:"
                                mvn -v

                                // --- THIS IS THE CRITICAL DIAGNOSTIC ---
                                echo "=== Listing Available Docker Networks ==="
                                docker network ls
                                echo "========================================"
                            '''
                        }
        }

        // ====================== ПОДГОТОВКА ======================
        stage('Checkout') {
            steps {
                // Очистка рабочей директории перед checkout
                cleanWs()

                // Получаем код из репозитория
                checkout scm

                // Выводим информацию о последнем коммите
                sh '''
                    echo "===== Latest commit message ====="
                    git log -1 --pretty=%B
                    echo "================================"
                '''
            }
        }

        // ====================== NEW DIAGNOSTIC STAGE ======================
        stage('Diagnose Networking') {
            steps {
                sh '''
                    echo "--- STARTING NETWORK DIAGNOSTIC ---"

                    # Step 1: Start a simple server container, publishing a random port
                    echo "--> Starting a web server container named 'network-test-server'..."
                    docker stop network-test-server || true
                    docker rm network-test-server || true
                    # The -P flag tells Docker to map the container's port 80 to a random port on the host
                    docker run -d --rm -P --name network-test-server --network ${DOCKER_NETWORK} nginx:alpine

                    # Give it a moment to start
                    sleep 5

                    # Step 2: Get the HOST port that Docker mapped port 80 to
                    HOST_PORT=$(docker inspect --format='{{(index (index .NetworkSettings.Ports "80/tcp") 0).HostPort}}' network-test-server)
                    if [ -z "$HOST_PORT" ]; then
                        echo "CRITICAL: Could not get the mapped HOST PORT of the test server."
                        docker stop network-test-server
                        exit 1
                    fi
                    echo "Test server container port 80 is mapped to HOST port: ${HOST_PORT}"

                    # Step 3: Test connectivity to the host using host.docker.internal
                    echo "--> Testing connection to 'host.docker.internal' on the mapped port..."

                    echo "--> First, trying to resolve host.docker.internal..."
                    ping -c 4 host.docker.internal

                    echo "--> Now, attempting to curl http://host.docker.internal:${HOST_PORT}"
                    # We expect this curl to succeed by returning the nginx welcome page
                    if curl -s --connect-timeout 10 "http://host.docker.internal:${HOST_PORT}" | grep -q "Thank you for using nginx"; then
                        echo "SUCCESS: Connection to host.docker.internal worked!"
                        echo "This means the problem lies elsewhere. But if it fails, this is the root cause."
                    else
                        echo "FAILURE: Could not connect to host.docker.internal. This is the root cause of the Testcontainers issue."
                        echo "The Jenkins container cannot reach sibling containers via the Docker host."
                    fi

                    # Step 4: Clean up
                    echo "--> Cleaning up the test server container..."
                    docker stop network-test-server

                    echo "--- NETWORK DIAGNOSTIC COMPLETE ---"
                '''
            }
        }

        // ====================== ТЕСТИРОВАНИЕ ======================
        stage('Run Tests') {
            steps {
                // Запускаем только тесты (без сборки) и Tell Testcontainers how to connect to sibling containers
                sh 'mvn test -Dtestcontainers.host.override=host.docker.internal surefire-report:report'

                // Архивируем отчёты тестов
                archiveArtifacts artifacts: 'target/surefire-reports/**/*', fingerprint: true

                // Публикуем результаты в формате JUnit для Jenkins
                junit 'target/surefire-reports/*.xml'
            }

            post {
                always {
                    // Всегда сохраняем отчёты, даже если тесты упали
                    echo "Test results archived"
                }
            }
        }

        // ====================== СБОРКА ======================
        stage('Build') {
            agent {
                docker {
                    image 'maven:3.8.3-openjdk-17'
                    args '-v $HOME/.m2:/root/.m2'
                }
            }
            steps {
                // Собираем проект, пропуская тесты (они уже выполнены)
                sh 'mvn clean package -DskipTests'
                echo "=== Checking JAR file on Build stage (1)==="
                sh 'ls -l target/ZebraPRJ-0.0.1-SNAPSHOT.jar' // Проверка наличия JAR

                // Архивируем собранный JAR-файл
                //archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                stash name: 'jar-artifact', includes: 'target/ZebraPRJ-0.0.1-SNAPSHOT.jar'
            }
        }

        // ====================== СОЗДАНИЕ DOCKER ОБРАЗА ======================
        stage('Build Docker Image') {
            steps {
                    // Извлечь архивированный JAR-файл
                    unstash 'jar-artifact'

                    // Собираем Docker образ с двумя тегами
                    sh """
                        echo "=== Checking JAR file on Build Docker Image stage(2)==="
                        ls -l target/ZebraPRJ-0.0.1-SNAPSHOT.jar
                        echo "=== Building Docker image ==="
                        ${DOCKER_PATH} build --no-cache -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                        ${DOCKER_PATH} tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest
                        echo "Image built: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                    """
            }
        }

        // ====================== ДЕПЛОЙ ======================
        stage('Deploy') {
            steps {
                    // Останавливаем и удаляем старый контейнер (если есть)
                    sh """
                        echo "=== Stopping old container ==="
                        ${DOCKER_PATH} stop ${CONTAINER_NAME} || true
                        ${DOCKER_PATH} rm ${CONTAINER_NAME} || true
                    """

                    // Запускаем новый контейнер
                    sh """
                        echo "=== Starting new container ==="
                        ${DOCKER_PATH}  run -d \\
                            -p ${APP_PORT}:${APP_PORT} \\
                            --name ${CONTAINER_NAME} \\
                            --network ${DOCKER_NETWORK} \\
                            -e SPRING_PROFILES_ACTIVE=docker \\
                            ${DOCKER_IMAGE}:latest
                    """
            }
        }

        // ====================== ПРОВЕРКА ======================
        stage('Verify') {
            steps {
                // Даём приложению 5 секунд на запуск
                sleep(time: 15, unit: 'SECONDS')

                // Проверяем статус контейнера и доступность эндпоинтов
                sh """
                    echo \\"=== Container status ===\\"
                    docker ps --filter name=${CONTAINER_NAME}

                    echo \\"=== Testing /hello endpoint ===\\"
                    curl -f http://localhost:${APP_PORT}/hello || echo \\"Service not responding\\"

                    echo \\"=== Testing Swagger UI ===\\"
                    curl -f http://localhost:${APP_PORT}/swagger-ui.html || echo \\"Swagger UI not accessible\\"
                """
            }
        }
    }

    // ====================== ПОСТ-ОБРАБОТКА ======================
    post {
        always {
            // Всегда выводим результат выполнения пайплайна
            echo "Pipeline ${currentBuild.fullDisplayName} completed with status: ${currentBuild.currentResult}"
        }
        failure {
            echo "Pipeline failed! Please check logs above for details."
        }
        success {
            echo "Pipeline succeeded! Application is running on port ${APP_PORT}"
        }
    }
}
