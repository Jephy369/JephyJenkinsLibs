def call(Map params)
{
    pipeline {
        agent any 

        environment {
            area = "${params.area}"
            qaEmail = "${params.qaEmail}"
            projectPath = "${params.projectPath}"
            cdnFileName = "${params.cdnFileName}"
        }

        stages {
            stage('build : Check Res') {
                steps {
                    bat '''
                    %area%
                    cd %projectPath%/tool/CDN/%cdnFileName%/
                    node upload.js
                    '''
                }
            }
        }
        post {
            always {
                echo 'One way or another, I have finished'
            }
            success {
                echo 'I succeeeded!'
            }
            unstable {
                echo 'I am unstable :/'
                bat '''
                cd %projectPath%/tool/UnityRealtimeLog
                python send_email.py -qaEmail %qaEmail% -content 2
                '''
            }
            failure {
                echo 'I failed :('
                bat '''
                cd %projectPath%/tool/UnityRealtimeLog
                python send_email.py -qaEmail %qaEmail% -content 3
                '''
            }
            changed {
                echo 'Things were different before...'
            }
        }
    }
}