#!/usr/bin/groovy
def call(body) {
   def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    try {
         body()
        // pod label
        def label = "pod-${UUID.randomUUID().toString()}"

        //build variables
        def mavenimage=config.mavenImage ?: 'maven:3.5-jdk-8'
        def nodeimage=config.nodeImage ?: 'node:8'
        def antimage=config.antImage ?: 'webratio/ant:1.9.6'
        def mavenbuildArgs=config.mavenbuildArgs ?: ''
        def npmbuildArgs=config.npmbuildArgs ?: ''
        def buildhome=config.buildhome ?: '.'

        //Sonarscan variables
        def sonarscan=config.sonarscan ?: ''
        def sonarimage=config.sonarImage ?: 'docker.fnis.com/base-sonar28:latest'

        //fortify scan variables
        def fortifyscan=config.fortifyscan ?: ''
        def fortifyimage=config.fortifyImage ?: 'docker.fnis.com/epo/base-fortify:latest'

        //docker build variables     
        def ocpproject=config.ocpproject ?: ''
        def dockerimagebuild=config.dockerimagebuild ?: ''
        def dockerbc=config.dockerbc ?: ''
        def dockerTag=config.dockerTag ?: ''
        def dockerpublish=config.dockerpublish ?: ''
        def occlientimage=config.occlientImage ?: 'docker.fnis.com/base-occlient360:latest'

        //ansible variables
        def ansibledeploy=config.ansibledeploy ?: ''
        def ansibleimage=config.ansibleImage ?: 'docker.fnis.com/base-ansible:latest'

        // print the usefull details to console
        echo "Pod Label: ${label}"
        echo "Run sonarscan:${sonarscan}"
        echo "Run fortifyscan:${fortifyscan}"
        echo "Run dockerimagebuild:${dockerimagebuild}"
        echo "Maven Image: ${mavenimage}"
        echo "Node Image: ${nodeimage}"
        echo "Ant Image: ${antimage}"
        
        timestamps {
          stage ("checkout-code") {              
             Mavenbuilder coslaveTemplate = new Mavenbuilder(label, mavenimage, this)    
             coslaveTemplate.MavenbuilderTemplate {
               println "I am in MavenbuilderTemplate"
               node(coslaveTemplate.podlabel) {
                 container('maven') {
                   sh 'echo checkout code'
                 }                  
               }
             }                        
          }
          if (mavenbuildArgs) {
            stage ("maven-build"){
              Mavenbuilder mavenslaveTemplate = new Mavenbuilder(label, mavenimage, this)
              mavenslaveTemplate.MavenbuilderTemplate {
                node(mavenslaveTemplate.podlabel) {
                  println "I am in MavenbuilderTemplate"
                  container('maven') {
                      sh 'mvn -version'
                  }
                }
              }
            }  // end maven build stage
          } //end of maven build if loop
          if (antbuildArgs) {
            stage ("ant-build"){
              Mavenbuilder mavenslaveTemplate = new Mavenbuilder(label, antimage, this)
              mavenslaveTemplate.MavenbuilderTemplate {
                node(mavenslaveTemplate.podlabel) {
                  println "I am in AntbuilderTemplate"
                  container('ant') {
                      sh 'ant -version'
                  }
                }
              }
            }  // end ant build stage
          } //end of ant build if loop
          if (npmbuildArgs) {
            stage ("node-build"){
              Nodebuilder nodeslaveTemplate = new Nodebuilder(label, nodeimage, this)
              nodeslaveTemplate.NodebuilderTemplate {
                node(nodeslaveTemplate.podlabel) {
                  println "I am in NodebuilderTemplate"
                  container('node') {
                      sh 'node -v && npm -v'
                  }
                }
              }
            }  // end node-build stage
          } //end of maven build if loop
          if (sonarscan){
            echo "Skipping Sonar Scan"
          }
          else{
            stage ("sonar-scan"){
              Sonarscan sonarslaveTemplate = new Sonarscan(label, sonarimage, this)
              sonarslaveTemplate.sonarscanTemplate {
                node(sonarslaveTemplate.podlabel) {
                  println "I am in sonarbuilderTemplate"
                  container('sonar') {
                      sh 'sonar-scanner -v'
                  }
                }
              }
            } // end sonar-scan stage
          } // end else loop
          if (fortifyscan){
            echo "Skipping Fortify Scan"
          }
          else{
            stage('fortify-scan'){
              Fortifyscan fortifyslaveTemplate = new Fortifyscan(label, fortifyimage, this)
              fortifyslaveTemplate.fortifyscanTemplate {
                node(fortifyslaveTemplate.podlabel) {
                  println "I am in fortifyscanTemplate"
                  container('fortify') {
                      sh 'sourceanalyzer -h'
                  }
                }
              }
            } // end fortify-scan stage
          } // end of else loop
          if (dockerimagebuild){
            echo "Skipping Docker image build"
          }
          else {
            stage('Docker-image-build'){
              Dockerbuild occlientslaveTemplate = new Dockerbuild(label, occlientimage, this)
              occlientslaveTemplate.dockerbuildTemplate {
                node(occlientslaveTemplate.podlabel) {
                  println "I am in occlientTemplate"
                  container('occlient') {
                      sh 'oc version'
                  }
                }
              }
            } // end docker image build stage
          } // end of else loop
          if (ansibledeploy){
            echo "Skipping Ansible deploy"
          }
          else {
            stage('Ansible-deploy'){
              Ansiblebuilder ansibleslaveTemplate = new Ansiblebuilder(label, ansibleimage, this)
              ansibleslaveTemplate.ansibleTemplate {
                node(ansibleslaveTemplate.podlabel) {
                  println "I am in ansibleclientTemplate"
                  container('ansible') {
                      sh 'echo hello from ansible'
                      sh 'ansible --version && ansible-galaxy --version'
                  }
                }
              }
            } // end ansible deploy stage
          } // end of else loop
        } // End of timestamp
    } //end of try loop
    catch(e) {
        currentBuild.result = "FAILED"
        echo 'BUILD FAILED'
        throw e
    } finally {
        buildNotification()
        //echo "Build Status: ${currentBuild.result}"
    }    
}