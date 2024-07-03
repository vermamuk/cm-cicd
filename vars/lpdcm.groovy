#!/usr/bin/groovy

def sonarscan (String sonarscanArgs) {
    println ("Running sonarscan")
    println ("Sonarscan Args: ${sonarscanArgs}")    
    container("maven"){
        dir("${env.WORKSPACE}"){
            withSonarQubeEnv('ooo-sonarqube'){
                sh """
                        mvn -V -f pom.xml ${sonarscanArgs} \
                        -Dsonar.branch.name=${env.BRANCH_NAME} \
                        -Dsonar.host.url=${SONAR_HOST_URL} \
                        -Dsonar.login=${SONAR_AUTH_TOKEN} \
                        -DSONAR_USER_HOME=${env.WORKSPACE}
                """
            }
        }
    }    
}

def sonarrunnerscan (String versionNumber){    
    container('sonar') {
        sh 'sonar-scanner -v'
        dir("${env.WORKSPACE}"){
            withSonarQubeEnv('ooo-sonarqube'){                
                sh """
                    sonar-scanner -Dsonar.projectVersion=${versionNumber} \
                    -Dsonar.branch.name=${env.BRANCH_NAME} \
                    -Dsonar.host.url=${SONAR_HOST_URL} \
                    -Dsonar.login=${SONAR_AUTH_TOKEN} \
                    -DSONAR_USER_HOME=${env.WORKSPACE}
                """
            }
        }
    }    
}

def buildChart(String chartName, String tillerNamespace, String namespace) {
    sh """
    helm repo update
    helm dependency update $chartName
    helm upgrade $chartName $chartName --tiller-namespace $tillerNamespace \
    --namespace $namespace --install --force
    """
}
     
def publishChart(List<String> chartsList, String helmrepo) {
    println ("Publish charts to artifactory")
    chartsList.each { name ->
        sh """
        helm repo update
        helm dependency update $name
        helm_push $name ${helmrepo}
        """
    }  
}

def deploy(String chart, String version, String tillerNamespace, String namespace, String helmvirtualrepo){
   println ("Deploy application to environment")
   sh """
        helm repo update
        helm upgrade $chart '$helmvirtualrepo/$chart' --recreate-pods \
        --tiller-namespace $tillerNamespace --namespace $namespace \
        --version ${version} --install --force --wait
    """    
}
def fortifyTranslate(String fortifyURL,
                     String fortifyProjectName,                     
                     String versionNumber,
                     String fortifyProjectVersion,
                     String exclusionList,
                     String maxHeap) {                       
    def reportingBranch = "${env.BRANCH_NAME}"    
    reportingBranch = sh(script: "echo ${reportingBranch} | sed -e 's#/#-#g'", returnStdout: true)
    reportingBranch = reportingBranch.trim()
    echo "reportingBranch: ${reportingBranch}"
    echo "Fortify Project:********** ${fortifyProjectName} ************"
    echo "FPR FileName: **************${fortifyProjectVersion}-${versionNumber}.fpr **********"

    //update fortify rules
    //sh 'fortifyupdate -acceptKey'
    sh "fortifyupdate -url ${fortifyURL} -acceptKey -acceptSSLCertificate"

    //Cleanup previous workspace before starting scan.
    sh 'sourceanalyzer -version'        
    sh "sourceanalyzer -clean -b ${fortifyProjectVersion} ."

    //Translate phase
    sh """sourceanalyzer  -verbose ${exclusionList} \
    -source '1.8' -cp '/data/.m2/repository/**/*.jar' \
    -64 -Xms300M -Xmx${maxHeap} -b ${fortifyProjectVersion} ."""
}

def fortifyScan(String versionNumber,String fortifyProjectVersion,String threadCount){
    sh """
        sourceanalyzer -b ${fortifyProjectVersion} \
        -scan -j ${threadCount} -XX:+UnlockExperimentalVMOptions \
        -XX:+UseCGroupMemoryLimitForHeap \
        -debug -verbose \
        -f ${fortifyProjectVersion}-${versionNumber}.fpr
    """
}
 
def fortifyParallelScan(String versionNumber,
                        String fortifyProjectVersion,
                        String threadCount,
                        String maxHeap){
    sh """
        sourceanalyzer -b ${fortifyProjectVersion} \
        -scan -64 -j ${threadCount} -Xss256M -Xms300M -Xmx${maxHeap} \
        -debug -verbose -Dcom.fortify.sca.DisableSwapTaintProfiles=True \
        -f ${fortifyProjectVersion}-${versionNumber}.fpr
    """
}
//Generate for security auditor view
def fortifyReportGenerator(String versionNumber,String fortifyProjectVersion){    
    sh """
        ReportGenerator -verbose \
        -format pdf \
        -f ${fortifyProjectVersion}_vulnerabilities.pdf \
        -source ${fortifyProjectVersion}-${versionNumber}.fpr \
        -template /opt/fortify/Core/config/reports/OWASP2017.xml \
        -filterSet 'Security Auditor View'
    """
}
//Upload FPR report to fortify SSC server
def fortifyUpload(String versionNumber,
                  String fortifyProjectName,
                  String fortifyProjectVersion,
                  String fortifyToken,
                  String fortifyURL){
    
    sh """
        fortifyclient uploadFPR \
        -project '${fortifyProjectName}' \
        -version '${fortifyProjectVersion}' \
        -url ${fortifyURL} \
        -f '${fortifyProjectVersion}-${versionNumber}.fpr' \
        -authtoken ${fortifyToken}
    """
}

/*////////////////////////////////
ToDo: Add node unit test stage.
      Add Selenium test.
      Add Ansible samples.
      Add Fortify on Demand stage.
      Add Xrayscanning stage.
      Add on Prem deployment stage.
////////////////////////////////*/