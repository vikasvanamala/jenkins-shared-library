def call (Map configMap){
    pipeline {
    // These are pre-build sections
        agent {
            node {
                label 'AGENT-1'
            }
        }
        environment {
            COURSE = "Jenkins"
            appVersion = configMap.get("appVersion")
            ACC_ID = "160885265516"
            PROJECT = configMap.get("project")
            COMPONENT = configMap.get("component")
            deploy_to = configMap.get("deploy_to")
            REGION = "us-east-1"
        }
        options {
            timeout(time: 30, unit: 'MINUTES') 
            disableConcurrentBuilds()
        }
        /* parameters {
            string(name: 'appVersion', description: 'Which app version you want to deploy')
            choice(name: 'deploy_to', choices: ['dev', 'qa', 'prod'], description: 'Pick something')
        } */
        // This is build section
        stages {
            
            stage('Deploy') {
                when{
                    expression { deploy_to == "dev" || deploy_to = "qa" || deploy_to = "qa" }
                }
                steps {
                    script{
                        withAWS(region:'us-east-1',credentials:'aws-creds') {
                            sh """
                                set -e
                                aws eks update-kubeconfig --region ${REGION} --name ${PROJECT}-${deploy_to}
                                kubectl get nodes
                                sed -i "s/IMAGE_VERSION/${appVersion}/g" values.yaml
                                helm upgrade --install ${COMPONENT} -f values-${deploy_to}.yaml -n ${PROJECT} --atomic --wait --timeout=5m .
                                #kubectl apply -f ${COMPONENT}-${deploy_to}.yaml
                            """
                        }
                    }
                }
            }
            stage('Functional Testing'){
                when{
                    expression { deploy_to == "dev" }
                }
                steps{
                    script{
                        sh """
                            echo "functional tests in DEV environment"
                        """
                    }
                }
            }
            stage('Integration Testing'){
                when{
                    expression { deploy_to == "qa" }
                }
                steps{
                    script{
                        sh """
                            echo "integration tests QA DEV environment"
                        """
                    }
                }
            }
            stage('E2E Testing'){
                when{
                    expression { deploy_to == "uat" }
                }
                steps{
                    script{
                        sh """
                            echo "e2e tests UAT environment"
                        """
                    }
                }
            }
            stage('PROD Process'){
                when{
                    expression { deploy_to == "prod" }
                }
                steps{
                    script{
                        sh """ 
                            echo "received CR ticket id"
                            echo "e2e tests UAT environment"
                        """
                    }
                }
            }
            
        }
        post {
            always {
                echo 'i will always say hello/....'
                cleanWs()
            }
            success {
                echo 'I will run if success'
            }
            failure {
                echo 'I will run if failure'
            }
            aborted {
                echo 'pipeline is aborted'
            }
        }
    }
}