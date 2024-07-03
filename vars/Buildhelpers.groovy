#!/usr/bin/groovy

public class Buildhelpers implements Serializable {
	public String podlabel
  public String mavenimage
  public String nodeimage
  public String fortifyimage
  public String sonarimage
  public String occlientimage
  public String workingdir
  def script

  public Buildhelpers(String label, 
                      String mavenimage,
                      String nodeimage, 
                      String sonarimage, 
                      String fortifyimage, 
                      String occlientimage, 
                      String workingdir, 
                      script) {
		this.podlabel=label
    this.mavenimage=mavenimage
    this.nodeimage=nodeimage
    this.sonarimage=sonarimage
    this.fortifyimage=fortifyimage
    this.occlientimage=occlientimage
    this.workingdir=workingdir
    this.script=script
  }
  public void BuilderTemplate (body) {    
    script.podTemplate(
          label: podlabel,
          containers: [
            script.containerTemplate(
              name: 'maven', 
              image: mavenimage,
              command: 'cat',
              envVars: [
                script.envVar(key: 'JAVA_TOOL_OPTIONS', value: "-Duser.home=/home/jenkins"),
                script.envVar(key: 'MAVEN_CONFIG', value: '/home/jenkins/.m2')
              ],
              ttyEnabled: true,
              workingDir: workingdir,
              alwaysPullImage: false,
              resourceRequestCpu: '500m',
              resourceLimitCpu: '4',
              resourceRequestMemory: '1Gi',
              resourceLimitMemory: '8Gi'
            ),
            script.containerTemplate(
                    name: 'node', 
                    image: nodeimage,
                    command: 'cat',
                    envVars: [
                        script.envVar(key: 'NPM_CONFIG_USERCONFIG', value: '/data/.npm/config/.npmrc')
                    ],
                    ttyEnabled: true,
                    workingDir: workingdir,
                    alwaysPullImage: false,
                    resourceRequestCpu: '500m',
                    resourceLimitCpu: '2',
                    resourceRequestMemory: '500Mi',
                    resourceLimitMemory: '2Gi'
            ),            
            script.containerTemplate(
                name: 'sonar', 
                image: sonarimage,
                command: 'cat',
                envVars: [
                    script.envVar(key: 'MAVEN_CONFIG', value: '/home/jenkins/.m2')
                ],
                ttyEnabled: true,
                workingDir: workingdir,
                alwaysPullImage: false,
                resourceRequestCpu: '500m',
                resourceLimitCpu: '2',
                resourceRequestMemory: '500Mi',
                resourceLimitMemory: '2Gi'
            ),
            script.containerTemplate(
                name: 'fortify', 
                image: fortifyimage,
                command: 'cat', 
                ttyEnabled: true,
                workingDir: workingdir,
                alwaysPullImage: false,
                resourceRequestCpu: '500m',
                resourceLimitCpu: '4',
                resourceRequestMemory: '8Gi',
                resourceLimitMemory: '64Gi'
            ),
            script.containerTemplate(
                    name: 'occlient', 
                    image: occlientimage,
                    command: 'cat',
                    ttyEnabled: true,
                    workingDir: workingdir,              
                    alwaysPullImage: false,
                    resourceRequestCpu: '500m',
                    resourceLimitCpu: '3',
                    resourceRequestMemory: '500Mi',
                    resourceLimitMemory: '4Gi'
            )
          ],
          volumes: [
            script.secretVolume(secretName: 'maven-settings', mountPath: '/home/jenkins/.m2'),
            script.secretVolume(secretName: 'npm-settings', mountPath: '/data/.npm/config'),
            script.persistentVolumeClaim(claimName: 'maven-storage', mountPath: '/data/.m2/repository'),            
            script.persistentVolumeClaim(claimName: 'npm-storage', mountPath: '/data/.npm'),
            script.persistentVolumeClaim(claimName: 'build-workspace', mountPath: '/jenkins-workspace')
          ]
       ){
            body ()
       }
  }
 
}