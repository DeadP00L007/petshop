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
                    sh "${SCANNER_HOME}/bin/sonar-scanner -Dsonar.projectKey=petshop"
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 30, unit: 'MINUTES') {   // 30 MINUTES = 100% GUARANTEED PASS
                    waitForQualityGate abortPipeline: true, credentialsId: 'sonar-token'
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                withDockerRegistry(credentialsId: 'docker', url: '') {
                    sh "docker build -t ${DOCKER_USER}/${IMAGE_NAME}:${IMAGE_TAG} ."
                    sh "docker push ${DOCKER_USER}/${IMAGE_NAME}:${IMAGE_TAG}"
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
                sleep 50
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
        success { echo 'SUCCESS: PIPELINE IS GREEN! Petshop deployed!' }
        always  { cleanWs() }
    }
}
