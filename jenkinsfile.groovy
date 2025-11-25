cat > Jenkinsfile.groovy << 'EOF'
pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "skchilam1/hello-Jenkins-CICD"
        APP_SERVER = "13.59.20.120"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Maven Project') {
            steps {
                sh 'mvn clean package'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t ${DOCKER_IMAGE}:latest ."
            }
        }

        stage('Push to Docker Hub') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub-creds',
                    usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                    sh """
                      echo "$PASS" | docker login -u "$USER" --password-stdin
                      docker push ${DOCKER_IMAGE}:latest
                    """
                }
            }
        }

        stage('Deploy on EC2') {
            steps {
                sshagent(['app-ec2-ssh']) {
                    sh """
                       ssh -o StrictHostKeyChecking=no ec2-user@${APP_SERVER} "docker stop app || true"
                       ssh -o StrictHostKeyChecking=no ec2-user@${APP_SERVER} "docker rm app || true"
                       ssh -o StrictHostKeyChecking=no ec2-user@${APP_SERVER} "docker pull ${DOCKER_IMAGE}:latest"
                       ssh -o StrictHostKeyChecking=no ec2-user@${APP_SERVER} "docker run -d --name app -p 8080:8080 ${DOCKER_IMAGE}:latest"
                    """
                }
            }
        }
    }
}
EOF
