#!groovy
pipeline {
    agent {
        docker {
            image 'maven:3.8.6-openjdk-17'
            args '-v /var/run/docker.sock:/var/run/docker.sock'  // Даём доступ к Docker из контейнера
        }
    }

    environment {
        DOCKER_IMAGE = 'zebra-prj'
        DOCKER_TAG = "${env.BUILD_ID}"  // Используем ID сборки как тег
        CONTAINER_NAME = 'zebra-prj'
        APP_PORT = '8081'
    }

    stages {
        stage('Checkout') {
            steps {
                // Получаем код из репозитория
                checkout scm

                // Выводим информацию о последнем коммите (как в вашей текущей джобе)
                sh '''
                    echo "===== Latest commit message ====="
                    git log -1 --pretty=%B
                    echo "================================"
                '''
            }
        }

        stage('Run Tests') {
            steps {
                // Запускаем тесты и сохраняем отчеты в формате TestNG
                sh 'mvn test surefire-report:report'

                // Архивируем результаты тестов
                archiveArtifacts artifacts: 'target/surefire-reports/**/*', fingerprint: true
            }

            post {
                always {
                    // Всегда публикуем отчеты, даже если тесты упали
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build') {
            steps {
                // Собираем проект без запуска тестов (они уже выполнены на предыдущем этапе)
                sh 'mvn clean package -DskipTests'

                // Архивируем собранный JAR-файл
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    // Собираем Docker образ с двумя тегами: latest и с номером сборки
                    docker.build("${DOCKER_IMAGE}:${DOCKER_TAG}")
                    docker.image("${DOCKER_IMAGE}:${DOCKER_TAG}").tag('latest')
                }
            }
        }

        stage('Deploy') {
            steps {
                script {
                    // Останавливаем и удаляем старый контейнер (если существует)
                    sh "docker stop ${CONTAINER_NAME} || true"
                    sh "docker rm ${CONTAINER_NAME} || true"

                    // Запускаем новый контейнер с маппингом портов
                    sh """
                        docker run -d \
                          -p ${APP_PORT}:${APP_PORT} \
                          --name ${CONTAINER_NAME} \
                          ${DOCKER_IMAGE}:latest
                    """
                }
            }
        }

        stage('Verify') {
            steps {
                // Даём приложению 5 секунд на запуск
                sleep(time: 5, unit: 'SECONDS')

                script {
                    // Проверяем статус контейнера
                    sh "docker ps -f name=${CONTAINER_NAME}"

                    // Проверяем доступность эндпоинтов (как в вашей текущей джобе)
                    sh """
                        echo "=== Testing /hello endpoint ==="
                        curl -f http://localhost:${APP_PORT}/hello || echo "Service not responding"
                        echo "\n=== Testing Swagger UI ==="
                        curl -f http://localhost:${APP_PORT}/swagger-ui.html || echo "Swagger UI not accessible"
                    """
                }
            }
        }
    }

    post {
        always {
            // Всегда выводим сообщение о завершении пайплайна
            echo "Pipeline ${currentBuild.fullDisplayName} completed"
        }
    }
}