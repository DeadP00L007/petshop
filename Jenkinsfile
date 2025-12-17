pipeline {
    agent any

    tools {
        maven 'maven3'
        jdk   'jdk17'
    }

    environment {
        DOCKER_USER = 'dcx369'
        IMAGE_NAME  = 'petshop'
        IMAGE_TAG   = "build-${BUILD_NUMBER}"
    }

    stages {

        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/DeadP00L007/petshop.git'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh '''
                      mvn sonar:sonar \
                      -Dsonar.projectKey=petshop \
                      -Dsonar.projectName="Petshop Demo" \
                      -Dsonar.host.url=http://sonarqube:9000
                    '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                withDockerRegistry(
                  credentialsId: 'dockerhub',
                  url: 'https://index.docker.io/v1/'
                ) {
                    sh '''
                      docker build -t dcx369/petshop:${IMAGE_TAG} .
                      docker push dcx369/petshop:${IMAGE_TAG}
                    '''
                }
            }
        }

        stage('Trivy Scan') {
            steps {
                sh '''
                  docker run --rm \
                    -v /var/run/docker.sock:/var/run/docker.sock \
                    aquasec/trivy image \
                    --severity HIGH,CRITICAL \
                    dcx369/petshop:${IMAGE_TAG} > trivy.txt || true
                '''
                archiveArtifacts 'trivy.txt'
            }
        }
    }

    post {
        success {
            echo '✅ CI pipeline completed successfully'
        }
        always {
            deleteDir()
        }
    }
}
