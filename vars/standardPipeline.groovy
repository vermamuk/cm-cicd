#!/usr/bin/groovy
def call(body) {
   def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    
    //ocp variables
    def label = "pod-${UUID.randomUUID().toString()}"
    Map images = config.images ?: '[ jnlp:"jenkins/jnlp-slave:3.19-1-alpine", maven:"maven:3.5-jdk-8" ]'
    def clustername = config.clustername ?: 'kubernetes'
    def namespace = config.namespace ?: 'cm-cicd-dev'
    def tillerNamespace = config.tillerNamespace ?: 'cm-cicd-dev'
    def ocpdockerimagebuild = config.ocpdockerimagebuild ?: false
    def ocpregistry = config.ocpregistry ?: 'registry.ocpapp.ooodev.local:80'       
    def chartName = config.chartName ?: ''
    def buildImage = config.buildImage ?: ''
    ocpdockerimagebuild = String.valueOf(ocpdockerimagebuild)

    //build variables
    def mavenimage = config.mavenImage ?: 'maven:3.5-jdk-8'
    def nodeimage = config.nodeImage ?: 'node:8'
    def mavenbuildArgs = config.mavenbuildArgs ?: ''
    def enablestash = config.enablestash ?: false    
    def npmbuildArgs = config.npmbuildArgs ?: ''
    def buildhome = config.buildhome ?: '.'
    def workingdir = config.workingdir ?: '/home/jenkins'
    enablestash = String.valueOf(enablestash)

    //Sonarscan variables
    def sonarscanArgs = config.sonarscanArgs ?: ''
    def sonarscan = config.sonarscan ?: false    
    def sonarrunnerscan = config.sonarrunnerscan ?: false    
    def sonarimage = config.sonarImage ?: 'docker.fnis.com/base-sonar28:latest'
    sonarscan = String.valueOf(sonarscan)
    sonarrunnerscan = String.valueOf(sonarrunnerscan)

    //fortify scan variables
    def fortifyscan = config.fortifyscan ?: false
    fortifyscan = String.valueOf(fortifyscan)
    def fortifyimage = config.fortifyImage ?: 'docker.fnis.com/epo/base-fortify:latest'
    def fortifyProjectName = config.fortifyProjectName ?: ''
    def fortifyProjectVersion = config.fortifyProjectVersion ?: ''
    def fortifyupload = config.fortifyupload ?: false
    fortifyupload = String.valueOf(fortifyupload)

    //docker build variables
    def dockerimagebuild = config.dockerimagebuild ?: false
    def dockerpublish = config.dockerpublish ?: false
    def dockertag = config.dockertag ?: ''    
    def dockerregistry = config.dockerregistry ?: 'http://docker.ooodev.local'
    dockerimagebuild = String.valueOf(dockerimagebuild)    
    dockerpublish = String.valueOf(dockerpublish)
    
    // print usefull details to console
    echo "Pod Label: ${label}"
    echo "Run sonarscan:${sonarscan}"
    echo "Run fortifyscan:${fortifyscan}"
    echo "Run dockerimagebuild:${dockerimagebuild}"
    echo "Run ocpdockerimagebuild:${ocpdockerimagebuild}"
    echo "Container images: ${images}"
    echo "ImageTag: ${dockertag}"
    echo "BuilderImage: ${buildImage}"

   /////////////////////////////////////////
    def buildChart = { chart ->
          sh """
            helm repo update
            helm dependency update $chart
            helm upgrade $chart $chart --tiller-namespace $tillerNamespace --namespace $namespace --install --force
          """
    }
    def pushChart = { chartNames -> 
      chartNames.each { name ->
        sh """
          helm repo update
          helm dependency update $name
          helm_push $name helm-demo-local
        """  
      }
    }
  /////////////////////////////////////////////
    timestamps {
          slaveTemplate = new PodTemplates(clustername, namespace, label, images, workingdir, this)
          slaveTemplate.BuilderTemplate {
            node(slaveTemplate.podlabel) {
                 stage("checkout"){
                   checkout scm
                 }
                 if (npmbuildArgs){
                  stage ("node-build"){
                    container('node') {
                        sh "node -v && npm -v && ${npmbuildArgs}"
                    }// end of nodejs build
                  }
                 }
                 else {
                   println "Skipping node build"
                 }
                 if (mavenbuildArgs){
                  stage ("maven-build"){
                    container('maven') {
                      sh "mvn -V -U -B -f ${buildhome}/pom.xml ${mavenbuildArgs}"
                      if (enablestash == "true"){
                        stash name: "${ocpproject}", includes: "Dockerfile,**/target/**"
                      }                      
                      //sh 'mvn -version'
                    }//end of maven build.
                  }
                 }
                 else{
                   println "Skipping maven build"
                 }
                 if (sonarscan == "true"){
                   stage ("sonar-scan"){
                     container('maven'){
                       dir("${env.WORKSPACE}"){
                         withSonarQubeEnv('ooo-sonarqube'){
                           sh """
                                  mvn -V -f ${buildhome}/pom.xml ${sonarscanArgs} \
                                  -Dsonar.branch=${env.BRANCH_NAME} \
                                  -Dsonar.host.url=${SONAR_HOST_URL} \
                                  -Dsonar.login=${SONAR_AUTH_TOKEN} \
                                  -DSONAR_USER_HOME=${env.WORKSPACE}
                           """
                         }
                       }
                     }
                   }
                 }
                 else{
                    println "Skipping sonarscan"
                 }
                 if (sonarrunnerscan == "true") {
                   stage ("sonarrunner-scan"){
                     container('sonar') {
                       //sh 'sonar-scanner -v'
                       dir("${env.WORKSPACE}"){
                          withSonarQubeEnv('ooo-sonarqube'){
                            def pom = readMavenPom file: "${buildhome}/pom.xml"
                            versionNumber = pom.version
                            sh """
                              sonar-scanner -Dsonar.projectVersion=${versionNumber} \
                              -Dsonar.branch=${env.BRANCH_NAME} \
                              -Dsonar.host.url=${SONAR_HOST_URL} \
                              -Dsonar.login=${SONAR_AUTH_TOKEN} \
                              -DSONAR_USER_HOME=${env.WORKSPACE}
                            """
                          }
                       }
                     }  
                   }//end of sonarscan stage
                 }
                 else{
                    println "Skipping sonarrunnerscan"
                 } //end of else
                 if (fortifyscan == "true"){
                   stage ("fortify-scan"){
                     container('fortify'){
                       sh 'sourceanalyzer -version'
                       def pom = readMavenPom file: "pom.xml"
                       def reportingBranch = "${env.BRANCH_NAME}"
                       versionNumber = pom.version
                       reportingBranch = sh(script: "echo ${reportingBranch} | sed -e 's#/#-#g'", returnStdout: true)
                       reportingBranch = reportingBranch.trim();
                       echo "reportingBranch: ${reportingBranch}"

                      //Cleanup previous workspace before starting scan.
                      sh "sourceanalyzer -clean -b ${fortifyProjectVersion} ."

                      //Generate fpr report
                      sh """
                        sourceanalyzer -verbose \
                        -exclude '${env.WORKSPACE}/**/node_modules/**/*' \
                        -exclude '${env.WORKSPACE}/**/dist/**/*' \
                        -exclude '${env.WORKSPACE}/**/target/**/*' \
                        -source '1.8' -cp '/data/.m2/repository/**/*.jar' \
                        -64 -b ${fortifyProjectVersion} .
                      """
                      sh """
                          sourceanalyzer -b ${fortifyProjectVersion} \
                          -scan -rules /opt/fortify/rules-reports/fortify-additional-rules.xml \
                          -f ${fortifyProjectVersion}-${versionNumber}.fpr
                      """
                      //Generate for security auditor view
                      sh """
                          ReportGenerator -verbose \
                          -format pdf \
                          -f ${fortifyProjectVersion}_vulnerabilities.pdf \
                          -source ${fortifyProjectVersion}-${versionNumber}.fpr \
                          -template /opt/fortify/rules-reports/v421_Thread_Safety_Report.xml \
                          -filterSet 'Security Auditor View'
                      """
                      if (fortifyupload == "true"){
                        //Upload FPR report to fortify SSC server
                        sh """
                          fortifyclient uploadFPR \
                          -project '${fortifyProjectName}' \
                          -version '${fortifyProjectVersion}' \
                          -url ${FORTIFY_URL} \
                          -f '${fortifyProjectVersion}-${versionNumber}.fpr' \
                          -authtoken ${FORTIFY_TOKEN}
                        """
                      }//end of upload to fortifycentral
                     }
                   }// end of fortifyscan
                 }
                 else{
                   println "Skipping Fortifyscan"
                 }
                 // Build docker Image from traditional docker host machine.
                 if (dockerimagebuild == "true"){
                   stage ("docker-build"){
                      node ("docker"){
                        step([$class: 'WsCleanup'])
                        unstash "${ocpproject}"
                        sh "ls -l"
                        sh """docker build -t ${dockertag} \
                              --build-arg no_proxy=${no_proxy} --build-arg http_proxy=${http_proxy} --build-arg \
                              https_proxy=${https_proxy} .
                        """
                        sh "docker tag ${dockertag} ${ocpregistry}/${ocpproject}/${ocpbuildconfig}:${ocpimagetag}"
                        //publish to openshift registry this may go away in future and everything come down from artifactory.
                        retry(3)  {
                              withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: ocpprojecttoken,
                                usernameVariable: 'OC_USERNAME',
                                passwordVariable: 'OC_PASSWORD']]) {
                                sh "docker login --username=${OC_USERNAME} --password=${OC_PASSWORD} ${ocpregistry}"
                                sh "docker push ${ocpregistry}/${ocpproject}/${ocpbuildconfig}:${ocpimagetag}"
                              }
                        }
                        //if this is release build publish to artifactory docker registry
                        if (dockerpublish == "true") {
                          stage ("publish-to-artifactory"){
                            withDockerRegistry([credentialsId: '50b3e901-2579-437c-b5e8-3dfba692af4b', url: "${dockerregistry}"]) {                              
                              sh "docker push ${dockertag}"
                            }
                          }
                        }
                      }//end of docker node
                   }//end of dockerbuild stage from traditional docker host
                 }
                 else {
                     println "Skipping static Dockerhost image build"
                 }
                 if (ocpdockerimagebuild == "true"){
                   stage ("docker-build"){
                     container('helm') {
                      dir('charts') {
                        buildChart("${chartName}")
                      }
                     }
                     container('occlient') {
                      ocExecuteBuilds(["${buildImage}"])
                     }
                   }                    
                 }
                 else {
                   println "Skipping Dynamic ocpdocker host image build"
                 }
            } //end of node routine
          } // end of slaveTemplate
    } // End of timestamp
}

