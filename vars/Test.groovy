// 使用方法
// library 'Jephy-libs'
// Test(content: "This is test content!")

def call(Map params)
{
    pipeline
    {
        agent any
        parameters
        {
            string(name:'content',defaultValue:"这是content第一",description:"测试")
            string(name:'content2',defaultValue:"这是content第二",description:"测试")
        }
        environment
        {
            CONTENT_LOG = "${params.content}"
            CONTENT_LOG2 = "${params.content2}"
        }

        stages
        {
            stage("test")
            {
                steps
                {
                    echo "${CONTENT_LOG}"
                    echo "${CONTENT_LOG2}"
                    echo "${content}"
                    echo "${content2}"
                }
            }
        }
    }
}