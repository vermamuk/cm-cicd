#!/usr/bin/groovy

public class Mavenbuilder implements Serializable {
	 public String podlabel
   public String builderimage
   public String workingdir
   def script

  public Mavenbuilder(String label, String builderimage, String workingdir, script) {
	  this.podlabel=label
    this.builderimage=builderimage
    this.workingdir=workingdir
    this.script=script
  }
  public void MavenbuilderTemplate (body) {    
    script.podTemplate(
          label: podlabel,
          containers: [
            script.containerTemplate(
              name: 'maven', 
              image: builderimage,
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