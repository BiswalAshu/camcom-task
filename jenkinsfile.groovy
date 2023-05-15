  pipeline {
    agent any
    environment {
        registry = "https://hub.docker.com"
        registryCredential = 'docker-hub'
        dockerImage = 'deploy-app'
        KUBECONFIG = credentials('kubeconfig-secret')
    }
    stages {
        stage('Cloning Git') {
        steps {
            git branch: 'main',
                url: 'https://github.com/BiswalAshu/deploy-app.git'
            }
        }
        stage('Building image') {
            steps{
                script {
                    dockerImage = docker.build dockerImage + ":latest"
                }
            }
        }
       stage('Push Docker Image') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub', passwordVariable: 'DOCKERHUB_PASSWORD', usernameVariable: 'DOCKERHUB_USERNAME')]) {
                    sh 'echo $DOCKERHUB_PASSWORD | docker login -u $DOCKERHUB_USERNAME --password-stdin'
                    sh 'docker push biswalashu/deploy-app'
                }
            }
        }
 
        stage('Deploy') {
            environment{
                CLUSTER = 'gke_sincere-cortex-366017_asia-south1_camcom-cluster'
                NAMESPACE= 'mopid-backend-stage'
            }
            steps {
                 withKubeConfig([credentialsId: KUBECONFIG]){
                    sh 'kubectl config use-context ${CLUSTER}'
                    // sh 'kubectl get pods -n ${NAMESPACE}'
                    sh 'kubectl set image deployment/deploy-${dockerImage} deploy-app-${dockerImage}-container=biswalashu/${dockerImage} -n ${NAMESPACE}'
                }
            }
        }
    } 
    post {
        always {
            sh "docker logout ${registry}"
        }
    }
}