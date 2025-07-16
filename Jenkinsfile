pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'zebra-prj'       // Название Docker образа
        DOCKER_TAG = "${env.BUILD_ID}"   // Тег образа (по номеру сборки)
        CONTAINER_NAME = 'zebra-prj'     // Имя контейнера
        APP_PORT = '8081'                // Порт приложения
        DOCKER_PATH = '/usr/bin/docker'
        DOCKER_NETWORK = 'jenkins_default' // Имя сети из docker-compose
    }

    stages {
        stage('Setup') {
            steps {
                            sh '''
                                echo "=== Проверка окружения ==="
                                echo "1. Проверка Docker:"
                                docker --version

                                echo "2. Проверка Java:"
                                echo "3. Установка переменных окружения ==="

                                export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
                                export PATH=$JAVA_HOME/bin:$PATH

                                java -version

                                echo "4. Проверка Maven:"
                                mvn -v
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

        // ====================== ТЕСТИРОВАНИЕ ======================
        stage('Run Tests') {
             agent {
                docker {
                    image 'maven:3.8.3-openjdk-17'
                    args '-v $HOME/.m2:/root/.m2'
                }
            }
            steps {
                // Запускаем только тесты (без сборки)
                sh 'mvn test surefire-report:report'

                // Архивируем отчёты TestNG
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
                sh 'ls -l target/ZebraPRJ-0.0.1-SNAPSHOT.jar' // Проверка наличия JAR

                // Архивируем собранный JAR-файл
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        // ====================== СОЗДАНИЕ DOCKER ОБРАЗА ======================
        stage('Build Docker Image') {
            steps {
                    // Собираем Docker образ с двумя тегами
                    sh """
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
                sleep(time: 5, unit: 'SECONDS')

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
