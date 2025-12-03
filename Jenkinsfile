pipeline {
    agent any

    tools {
        maven 'maven3'
        jdk    'jdk17'
    }

    environment {
        // SonarQube Scanner (installed via Jenkins Global Tool Configuration)
        SCANNER_HOME = tool 'sonar-scanner'

        // Docker Hub
        DOCKER_USER  = 'dcx369'
        IMAGE_NAME   = 'petshop'
        IMAGE_TAG    = "${BUILD_TAG}".toLowerCase()  // jenkins-build-123

        // SonarQube server name as configured in Jenkins → Manage Jenkins → Configure System
        SONAR_SERVER = 'sonar-server'
    }

    stages {
        stage('Clean Workspace') {
            steps { cleanWs() }
        }

        stage('Checkout SCM') {
            steps {
                git branch: 'main', url: 'https://github.com/DeadP00L007/petshop.git'
            }
        }

        stage('Compile') {
            steps { sh 'mvn clean compile' }
        }

        stage('Unit Tests') {
            steps { sh 'mvn test' }
            post { always { junit 'target/surefire-reports/*.xml' } }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv(SONAR_SERVER) {
                    sh """
                        ${SCANNER_HOME}/bin/sonar-scanner \
                            -Dsonar.projectKey=Petshop \
                            -Dsonar.projectName=Petshop \
                            -Dsonar.sources=src/main \
                            -Dsonar.java.binaries=target/classes \
                            -Dsonar.junit.reportsPath=target/surefire-reports \
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                    """
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true, credentialsId: 'sonar-token'
                }
            }
        }

        stage('Build WAR') {
            steps { sh 'mvn clean install -DskipTests' }
        }

        stage('OWASP Dependency-Check') {
            steps {
                dependencyCheck additionalArguments: '--format XML --enableExperimental', 
                                odcInstallation: 'dependency-check'
                dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
            }
        }

        stage('Build & Push Docker Image') {
            steps {
                script {
                    withDockerRegistry(credentialsId: 'docker', url: '') {
                        sh """
                            docker build -t ${DOCKER_USER}/${IMAGE_NAME}:${IMAGE_TAG} .
                            docker push ${DOCKER_USER}/${IMAGE_NAME}:${IMAGE_TAG}
                        """
                    }
                }
            }
        }

        stage('Trivy Image Scan') {
            steps {
                sh """
                    trivy image --format table --severity HIGH,CRITICAL \
                        ${DOCKER_USER}/${IMAGE_NAME}:${IMAGE_TAG} > trivy.txt
                """
                archiveArtifacts artifacts: 'trivy.txt', allowEmptyArchive: true
            }
        }

        stage('QA Testing (Smoke Test)') {
            steps {
                script {
                    // Remove old container if exists
                    sh 'docker rm -f qacontainer || true'

                    // Run latest image for QA
                    sh """
                        docker run -d --name qacontainer -p 8090:8080 \
                            ${DOCKER_USER}/${IMAGE_NAME}:${IMAGE_TAG}
                    """

                    sleep(time: 30, unit: 'SECONDS')

                    retry(10) {
                        sh '''
                            curl --fail --silent http://localhost:8090/jpetstore/ | \
                                grep -q "JPetStore" || \
                                (echo "JPetStore title not found!" && exit 1)
                        '''
                    }
                    echo "QA Smoke Test Passed – Application is up and running!"
                }
            }
            post {
                failure {
                    sh 'docker logs qacontainer || true'
                }
                always {
                    sh 'docker rm -f qacontainer || true'
                }
            }
        }

        stage('Trigger CD Pipeline') {
            steps {
                build job: 'DevSecOps-CD', 
                      parameters: [string(name: 'IMAGE_TAG', value: "${IMAGE_TAG}")],
                      wait: true
            }
        }
    }

    post {
        always {
            // Attach Trivy report and Jenkins log to every email
            emailext (
                subject: "CI Build ${currentBuild.result ?: 'SUCCESS'} - ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """
                    <h2>CI/CD Pipeline Result: ${currentBuild.result ?: 'SUCCESS'}</h2>
                    <p><b>Project:</b> ${env.JOB_NAME}</p>
                    <p><b>Build #${env.BUILD_NUMBER}</b></p>
                    <p><b>Duration:</b> ${currentBuild.durationString}</p>
                    <p><a href="${env.BUILD_URL}">View Full Console Output</a></p>
                    <p>Application is deployed and accessible via CD pipeline.</p>
                """,
                to: 'dhamichiran@gmail.com',
                attachmentsPattern: 'trivy.txt',
                attachLog: true
            )

            cleanWs()
        }

        success {
            echo 'CI Pipeline completed successfully! Image pushed and QA passed!'
        }

        failure {
            echo 'CI Pipeline failed. Check logs and Trivy report attached in email.'
        }
    }
}
