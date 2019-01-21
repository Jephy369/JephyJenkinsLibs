#!groovy
// params����������
//    area = win���õ��̷�����mac�Ͽ��Բ��á�����'D:'
//    projectPath = ��Ŀ·��������'D:/Work/M02_JP_patch_2'
//    unityPath = unity��װĿ¼������'E:/Program Files/Unity5.6.5p3/Editor/Unity.exe'
//    qaEmail = ִ����֪ͨqa�����䡣����'494754224@qq.com'
//    cdnFileName = CDN�ļ��С������շ���"AWSS3"
//    server = ������������1 �շ�2 ����3 ����4������'2'
//    resProjName = ��Դ��Ŀ�ļ������֡�����"AndroidEditor"
//    resServerUrl = ��Դ��������ַ�������Ҳ���hash�ļ���ʱ������
//    uploadScriptName = �������н���Դ�ŵ�uploadBySelf�Ľű���ÿ��������һ��������"upload.js"
def call(Map params)
{
    pipeline {
        agent any 
        parameters {
            string(name: 'versionCode',
            defaultValue: '["$azhash$1$1$1$xxx","$cvhash$1$xxx","","","$l2dhash$1$xxx"]',
            description: '��������һ�εİ汾��')

            string(name: 'targetVersion',
            defaultValue: '',
            description: '�Զ���汾�� ֻ�����Դ�汾 �磺1.5.5')
            
            booleanParam(name: 'updateCv',
            defaultValue: false,
            description: '�Ƿ�Ҫ����CV')
            
            booleanParam(name: 'updateL2d',
            defaultValue: false,
            description: '�Ƿ�Ҫ����Live2d')
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
                    bat '''
                    %area%
                    cd %projectPath%
                    svn cleanup --username ProjectBuilder --password cbbyn94l --no-auth-cache
                    svn up --accept tc --username ProjectBuilder --password cbbyn94l --no-auth-cache
                    svn revert . -R --username ProjectBuilder --password cbbyn94l --no-auth-cache
                    cd %projectPath%/client/project/%resProjName%
                    svn cleanup --username ProjectBuilder --password cbbyn94l --no-auth-cache
                    svn up --accept tc --username ProjectBuilder --password cbbyn94l --no-auth-cache
                    svn revert . -R --username ProjectBuilder --password cbbyn94l --no-auth-cache
                    cd %projectPath%/tool/UnityRealtimeLog
                    svn up --accept tc --username ProjectBuilder --password cbbyn94l --no-auth-cache
                    '''
                }
            }
            stage('Step 2 : Build Assets Bundle') {
                steps {
                    bat '''
                    %area%
                    cd %projectPath%/tool/UnityRealtimeLog
                    python unity_realtime_log.py -unity "%unityPath%" -project %projectPath%/client/project/%resProjName% -method SpineChar.Live2dPrefabImport
                    python unity_realtime_log.py -unity "%unityPath%" -project %projectPath%/client/project/%resProjName% -method ResPacker.CheckAssetBundles
                    '''
                }
            }
            stage('Step 3 : Build Lua Bundle') {
                steps {
                    bat '''
                    %area%
                    cd %projectPath%/tool/UnityRealtimeLog
                    python unity_realtime_log.py -unity "%unityPath%" -project %projectPath%/client/project/main -method BuildProject.CommandLineBuildLua -args 13~false~false
                    '''
                }
            }
            stage('Step 4 : Archive') {
                steps {
                    bat '''
                    %area%
                    cd %projectPath%/tool/UnityRealtimeLog
                    python azurlane_archive.py -unity "%unityPath%" -project %projectPath%/client/project/main -method BuildProject.CommandLineArchive -targetVersion "%targetVersion%" -args %server%~13~true~true -oldversion %versionCode%
                    '''
                }
            }
            stage('Step 5 : Fetch And Compare For Update Files') {
                steps {
                    bat '''
                    %area%
                    cd %projectPath%/tool/UnityRealtimeLog
                    python CompareHash.py -oldVersion %versionCode% -cdnFileName %cdnFileName% -platform android -resServerUrl "%resServerUrl%"
                    '''
                }
            }
            stage('Step 6 : Upload hot files') {
                steps {
                    bat '''
                    %area%
                    cd %projectPath%/tool/CDN/%cdnFileName%
                    node %uploadScriptName%
                    '''
                }
            }
            stage('Step 7 : Final Handler') {
                steps {
                    bat '''
                    %area%
                    cd %projectPath%/tool/UnityRealtimeLog
                    python final_handler.py -cdnFileName %cdnFileName% -platform android -oldversion %versionCode% -qaEmail %qaEmail% -updateCv %updateCv% -updateL2d %updateL2d%
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
                %area%
                cd %projectPath%/tool/UnityRealtimeLog
                python send_email.py -qaEmail %qaEmail% -content 2
                '''
            }
            failure {
                echo 'I failed :('
                bat '''
                %area%
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