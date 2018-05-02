def slackBuildFailure (stage) {
    slackSend channel: "${env.NOTIFY_CHANNEL}",
            color: 'danger',
            message: "<!here> Error in job <${env.BLUE_URL}|${env.JOB_NAME} #${env.BUILD_NUMBER}> stage ${stage} build."
}

pipeline {
    agent {
        node {
            label ''
            customWorkspace "${env.JOB_NAME}/${env.BUILD_NUMBER}"
        }
    }
    options {
        skipDefaultCheckout()
    }
    parameters {
        string(name: 'NOTIFY_CHANNEL', defaultValue: '#ci-notification', description: 'Slack channel to send notifications to.')
        string(name: 'TAG', defaultValue: 'master', description: 'A specific sha or branch to build.')
    }
    environment {
        BLUE_URL = "${env.JENKINS_URL}blue/organizations/jenkins/${env.JOB_NAME}/detail/${env.JOB_NAME}/${env.BUILD_NUMBER}/pipeline"
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh('git checkout $TAG')
            }
        }
        stage('TestUnit') {
            steps {
                sh './bin/test'
            }
            post {
                failure {
                    slackBuildFailure('TestUnit')
                }
            }
        }
    }
    post {
        aborted {
            deleteDir()
        }
        success {
            deleteDir()
        }
        failure {
            deleteDir()
        }
    }
}
