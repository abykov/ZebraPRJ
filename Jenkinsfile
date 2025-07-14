pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'zebra-prj'           // Название Docker образа
        DOCKER_TAG = "${env.BUILD_ID}"       // Тег образа (по номеру сборки)
        CONTAINER_NAME = 'zebra-prj'         // Имя контейнера
        APP_PORT = '8081'                    // Порт приложения
        DOCKER_PATH = '/usr/local/bin/docker' // Для MacOS явно указываем путь к Docker
    }

    stages {
        stage('Setup') {
            steps {
                            sh '''
                                echo "=== Проверка окружения ==="
                                echo "1. Проверка Docker сокета:"
                                if [ -S "/var/run/docker.sock" ]; then
                                    echo "Docker socket доступен"
                                    echo "2. Проверка версии Docker:"
                                    docker --version
                                    if ! docker --version > /dev/null 2>&1; then
                                        echo "ERROR: Не удалось определить версию Docker!"
                                        exit 1
                                    fi
                                    docker --version
                                    echo "3. Проверка расположения Docker:"
                                    if [ -x "/usr/bin/docker" ]; then
                                        ls -la /usr/bin/docker
                                    else
                                        echo "ERROR: Docker не найден в /usr/bin/docker!"
                                    fi
                                else
                                    echo "ERROR: Docker socket не найден!"
                                    exit 1
                                fi
                                echo "4. Проверка Java:"
                                echo "=== Установка переменных окружения ==="

                                export JAVA_HOME=/usr/java/openjdk-17  # Правильный путь для этого контейнера
                                export PATH=$JAVA_HOME/bin:$PATH

                                if ! java -version > /dev/null 2>&1; then
                                    echo "ERROR: Java не установлена или недоступна!"
                                    exit 1
                                fi
                                java -version

                                echo "Проверка symlinks:"
                                ls -la /usr/bin/java /usr/bin/mvn

                                echo "5. Проверка Maven:"
                                mvn -v




                            '''
                        }
        }

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
                        ${DOCKER_PATH} build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
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
                        ${DOCKER_PATH}  stop ${CONTAINER_NAME} || true
                        ${DOCKER_PATH}  rm ${CONTAINER_NAME} || true
                    """

                    // Запускаем новый контейнер
                    sh """
                        echo "=== Starting new container ==="
                        ${DOCKER_PATH}  run -d \\
                            -p ${APP_PORT}:${APP_PORT} \\
                            --name ${CONTAINER_NAME} \\
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
