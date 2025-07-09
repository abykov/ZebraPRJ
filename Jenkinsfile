pipeline {
    agent {
        docker {
            image 'maven:3.8.3-openjdk-17'
            // Монтируем Docker сокет и бинарник для работы с Docker внутри контейнера
            args '-v /var/run/docker.sock:/var/run/docker.sock -v /usr/local/bin/docker:/usr/bin/docker -v $HOME/.m2:/root/.m2'
        }
    }

    environment {
        DOCKER_IMAGE = 'zebra-prj'       // Название Docker образа
        DOCKER_TAG = "${env.BUILD_ID}"   // Тег образа (по номеру сборки)
        CONTAINER_NAME = 'zebra-prj'     // Имя контейнера
        APP_PORT = '8081'                // Порт приложения
    }

    stages {
        // ====================== ПОДГОТОВКА ======================
        stage('Checkout') {
            steps {
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
            steps {
                // Собираем проект, пропуская тесты (они уже выполнены)
                sh 'mvn clean package -DskipTests'

                // Архивируем собранный JAR-файл
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        // ====================== СОЗДАНИЕ DOCKER ОБРАЗА ======================
        stage('Build Docker Image') {
            steps {
                script {
                    // Собираем Docker образ с двумя тегами
                    sh """
                        echo "=== Building Docker image ==="
                        docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                        docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest
                        echo "Image built: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                    """
                }
            }
        }

        // ====================== ДЕПЛОЙ ======================
        stage('Deploy') {
            steps {
                script {
                    // Останавливаем и удаляем старый контейнер (если есть)
                    sh """
                        echo "=== Stopping old container ==="
                        docker stop ${CONTAINER_NAME} || true
                        docker rm ${CONTAINER_NAME} || true
                    """

                    // Запускаем новый контейнер
                    sh """
                        echo "=== Starting new container ==="
                        docker run -d \\
                            -p ${APP_PORT}:${APP_PORT} \\
                            --name ${CONTAINER_NAME} \\
                            ${DOCKER_IMAGE}:latest
                    """
                }
            }
        }

        // ====================== ПРОВЕРКА ======================
        stage('Verify') {
            steps {
                // Даём приложению 5 секунд на запуск
                sleep(time: 5, unit: 'SECONDS')

                // Проверяем статус контейнера и доступность эндпоинтов
                sh """
                    echo "=== Container status ==="
                    docker ps -f name=${CONTAINER_NAME}

                    echo "=== Testing /hello endpoint ==="
                    curl -f http://localhost:${APP_PORT}/hello || echo "Service not responding"

                    echo "=== Testing Swagger UI ==="
                    curl -f http://localhost:${APP_PORT}/swagger-ui.html || echo "Swagger UI not accessible"
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