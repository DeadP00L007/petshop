pipeline {
    agent any

    tools {
        maven 'maven3'
        jdk   'jdk17'
    }

    environment {
        SCANNER_HOME = tool 'sonar-scanner'
        DOCKER_USER  = 'dcx369'
        IMAGE_NAME   = 'petshop'
        IMAGE_TAG    = "build-${BUILD_NUMBER}"
    }

    stages {
        stage('Clean') { steps { cleanWs() } }

        stage('Checkout') {
            steps { git branch: 'main', url: 'https://github.com/DeadP00L007/petshop.git' }
        }

        stage('Build') {
            steps { sh 'mvn clean package -DskipTests' }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh """
                        ${SCANNER_HOME}/bin/sonar-scanner \\
                            -Dsonar.projectKey=petshop \\
                            -Dsonar.projectName="Petshop Demo" \\
                            -Dsonar.sources=src/main \\
                            -Dsonar.java.binaries=target/classes \\
                            -Dsonar.exclusions=**/target/**
                    """
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 60, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true, credentialsId: 'sonar-token'
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                withDockerRegistry(credentialsId: 'docker', url: '') {
                    sh """
                        docker build -t ${DOCKER_USER}/${IMAGE_NAME}:${IMAGE_TAG} .
                        docker push ${DOCKER_USER}/${IMAGE_NAME}:${IMAGE_TAG}
                    """
                }
            }
        }

        stage('Trivy Scan') {
            steps {
                sh "trivy image --severity HIGH,CRITICAL ${DOCKER_USER}/${IMAGE_NAME}:${IMAGE_TAG} > trivy.txt || true"
                archiveArtifacts 'trivy.txt'
            }
        }

        stage('Deploy QA') {
            steps {
                sh 'docker rm -f petshop-qa || true'
                sh "docker run -d --name petshop-qa -p 8090:8080 ${DOCKER_USER}/${IMAGE_NAME}:${IMAGE_TAG}"
                sleep 60
                sh 'curl -f http://localhost:8090/jpetstore/ | grep -q "JPetStore"'
            }
            post { always { sh 'docker rm -f petshop-qa || true' } }
        }

        stage('Trigger CD') {
            steps {
                build job: 'DevSecOps-CD', parameters: [string(name: 'IMAGE_TAG', value: IMAGE_TAG)], wait: true
            }
        }
    }

    post {
        success { echo 'IT IS GREEN! FULL SUCCESS! Application deployed!' }
        always  { cleanWs() }
    }
}
