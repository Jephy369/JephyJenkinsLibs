// ʹ�÷���
// library 'Jephy-libs'
// Test(content: "This is test content!")

def call(Map params)
{
    pipeline
    {
        agent any
        parameters
        {
            string(name:'content',defaultValue:"����content��һ",description:"����")
            string(name:'content2',defaultValue:"����content�ڶ�",description:"����")
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