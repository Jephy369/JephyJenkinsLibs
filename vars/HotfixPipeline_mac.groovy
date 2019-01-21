#!groovy
// params参数解析：
//    area = win上用的盘符，在mac上可以不用。例：'D:'
//    projectPath = 项目路径。例：'D:/Work/M02_JP_patch_2'
//    unityPath = unity安装目录。例：'E:/Program Files/Unity5.6.5p3/Editor/Unity.exe'
//    qaEmail = 执行完通知qa的邮箱。例：'494754224@qq.com'
//    cdnFileName = CDN文件夹。例如日服："AWSS3"
//    server = 服务器。国服1 日服2 韩服3 美服4。例：'2'
//    resProjName = 资源项目文件夹名字。例："AndroidEditor"
//    resServerUrl = 资源服务器地址，用于找不到hash文件的时候下载
//    uploadScriptName = 用于运行将资源放到uploadBySelf的脚本，每个服不大一样。例："upload.js"
def call(Map params)
{
    pipeline {
        agent any 
        parameters {
            string(name: 'versionCode',
            defaultValue: '["$azhash$1$1$1$xxx","$cvhash$1$xxx","","","$l2dhash$1$xxx"]',
            description: '请输入上一次的版本号')

            string(name: 'targetVersion',
            defaultValue: '',
            description: '自定义版本号 只针对资源版本 如：1.5.5')
            
            booleanParam(name: 'updateCv',
            defaultValue: false,
            description: '是否要更新CV')
            
            booleanParam(name: 'updateL2d',
            defaultValue: false,
            description: '是否要更新Live2d')
        }

        environment {
            area = "${params.area}"
            projectPath = "${params.projectPath}"
            unityPath = "${params.unityPath}"
            qaEmail = "${params.qaEmail}"
            cdnFileName = "${params.cdnFileName}"
            server = "${params.server}"
            resProjName = "${params.resProjName}"
            resServerUrl = "${params.resServerUrl}"
            uploadScriptName = "${params.uploadScriptName}"
        }

        stages {
            stage('Step 1 : Update Project') {
                steps {
                    sh '''
                    cd ${projectPath}
                    svn cleanup --username ProjectBuilder --password cbbyn94l --no-auth-cache
                    svn up --accept tc --username ProjectBuilder --password cbbyn94l --no-auth-cache
                    svn revert . -R --username ProjectBuilder --password cbbyn94l --no-auth-cache
                    cd ${projectPath}/client/project/${resProjName}
                    svn cleanup --username ProjectBuilder --password cbbyn94l --no-auth-cache
                    svn up --accept tc --username ProjectBuilder --password cbbyn94l --no-auth-cache
                    svn revert . -R --username ProjectBuilder --password cbbyn94l --no-auth-cache
                    cd ${projectPath}/tool/UnityRealtimeLog
                    svn up --accept tc --username ProjectBuilder --password cbbyn94l --no-auth-cache
                    '''
                }
            }
            stage('Step 2 : Build Assets Bundle') {
                steps {
                    sh '''
                    ${unityPath} -projectPath ${projectPath}/client/project/${resProjName} -quit
                    cd ${projectPath}/tool/UnityRealtimeLog
                    python azurlane_respacker_formac.py -unity ${unityPath} -project ${projectPath}/client/project/${resProjName} -method SpineChar.Live2dPrefabImport
                    python azurlane_respacker_formac.py -unity ${unityPath} -project ${projectPath}/client/project/${resProjName} -method ResPacker.CheckAssetBundles
                    '''
                }
            }
            stage('Step 3 : Build Lua Bundle') {
                steps {
                    sh '''
                    ${unityPath} -projectPath ${projectPath}/client/project/main -quit
                    cd ${projectPath}/tool/UnityRealtimeLog
                    python unity_realtime_log.py -unity ${unityPath} -project ${projectPath}/client/project/main -method BuildProject.CommandLineBuildLua -args 9~false~false
                    '''
                }
            }
            stage('Step 4 : Archive') {
                steps {
                    sh '''
                    cd ${projectPath}/tool/UnityRealtimeLog
                    python azurlane_archive.py -unity ${unityPath} -project ${projectPath}/client/project/main -method BuildProject.CommandLineArchive -targetVersion "${targetVersion}" -args ${server}~9~true~true -oldversion ${versionCode}
                    '''
                }
            }
            stage('Step 5 : Fetch And Compare For Update Files') {
                steps {
                    sh '''
                    cd ${projectPath}/tool/UnityRealtimeLog
                    python CompareHash.py -oldVersion ${versionCode} -cdnFileName ${cdnFileName} -platform ios -resServerUrl "${resServerUrl}"
                    '''
                }
            }
            stage('Step 6 : Upload hot files') {
                steps {
                    sh '''
                    cd ${projectPath}/tool/CDN/${cdnFileName}
                    node ${uploadScriptName}
                    '''
                }
            }
            stage('Step 7 : Final Handler') {
                steps {
                    sh '''
                    cd ${projectPath}/tool/UnityRealtimeLog
                    python final_handler.py -cdnFileName ${cdnFileName} -platform ios -oldversion ${versionCode} -qaEmail ${qaEmail} -updateCv ${updateCv} -updateL2d ${updateL2d}
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
                sh '''
                cd ${projectPath}/tool/UnityRealtimeLog
                python send_email.py -qaEmail ${qaEmail} -content 2
                '''
            }
            failure {
                echo 'I failed :('
                sh '''
                cd ${projectPath}/tool/UnityRealtimeLog
                python send_email.py -qaEmail ${qaEmail} -content 3
                '''
            }
            changed {
                echo 'Things were different before...'
            }
        }
    }
}