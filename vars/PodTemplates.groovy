#!/usr/bin/groovy
public class PodTemplates implements Serializable {
    public String clustername
    public String namespace
    public String podlabel
    public String workingdir
    public String memLmt
    public String cpuLmt
    public String m2FileSystem
    public String buildWrkspace
    def script
    public Map images
    def inputcontainers = []

  public PodTemplates(String clustername,
                      String namespace,
                      String label, 
                      Map images,
                      String workingdir, 
                      script) {
        this.clustername=clustername
        this.namespace=namespace               
		    this.podlabel=label
        this.workingdir=workingdir
        this.script=script
        this.images = images
        if (images.containsKey('mavenStorage')) {
          m2FileSystem = images."mavenStorage"
        }
        else {
          m2FileSystem = 'maven-storage'
        }
        if (images.containsKey('buildworkspace')){
          buildWrkspace = images."buildworkspace"
        }
        else {
          buildWrkspace = "build-workspace"
        }
        //Build what containerTemplate will be contrusted in podTemplates
        if (images.containsKey('custom')) {
          cpuLmt = '500m'
          memLmt = '500Mi'
          if (images.containsKey('podCpuLmt')) {
            cpuLmt = images."podCpuLmt"
          }
          if (images.containsKey('podMemLmt')) {
            memLmt = images."podMemLmt"
          }
          this.inputcontainers  << 
            script.containerTemplate(
              name: 'custom', 
              image: images."custom",
              command: 'cat',                      
              ttyEnabled: true,
              workingDir: workingdir,
              alwaysPullImage: false,
              resourceRequestCpu: '100m',
              resourceLimitCpu: cpuLmt,
              resourceRequestMemory: '500Mi',
              resourceLimitMemory: memLmt
            )
        }
        if (images.containsKey('fortify')) {
          cpuLmt = '500m'
          memLmt = '500Mi'
          if (images.containsKey('fortifyCpuLmt')) {
            cpuLmt = images."fortifyCpuLmt"
          }
          if (images.containsKey('fortifyMemLmt')) {
            memLmt = images."fortifyMemLmt"
          }
          this.inputcontainers << 
            script.containerTemplate(
              name: 'fortify', 
              image: images."fortify",
              command: 'cat', 
              ttyEnabled: true,
              workingDir: workingdir,
              alwaysPullImage: false,
              resourceRequestCpu: '100m',
              resourceLimitCpu: cpuLmt,
              resourceRequestMemory: '500Mi',
              resourceLimitMemory: memLmt
            )
        }
        if (images.containsKey('helm')) {
          cpuLmt = '500m'
          memLmt = '500Mi'
          if (images.containsKey('helmCpuLmt')) {
            cpuLmt = images."helmCpuLmt"
          }
          if (images.containsKey('helmMemLmt')) {
            memLmt = images."helmMemLmt"
          }
          this.inputcontainers << 
            script.containerTemplate(
              name: 'helm', 
              image: images."helm",
              command: 'cat', 
              ttyEnabled: true,
              workingDir: workingdir,
              alwaysPullImage: false,
              resourceRequestCpu: '100m',
              resourceLimitCpu: cpuLmt,
              resourceRequestMemory: '500Mi',
              resourceLimitMemory: memLmt
            )
        }        
        if (images.containsKey('jnlp')) {
	        cpuLmt = '800m'
	        memLmt = '1500Mi'
          if (images.containsKey('jnlpCpuLmt')) {
            cpuLmt = images."jnlpCpuLmt"
          }
          if (images.containsKey('jnlpMemLmt')) {
              memLmt = images."jnlpMemLmt"
          }
          this.inputcontainers  << 
              script.containerTemplate(
                name: 'jnlp', 
                image: images."jnlp",                        
                args: '${computer.jnlpmac} ${computer.name}',
                workingDir: workingdir,
                resourceRequestCpu: '50m',
                resourceLimitCpu: cpuLmt,
                resourceRequestMemory: '512Mi',
                resourceLimitMemory: memLmt
              )
        }
        if (images.containsKey('maven')) {
          cpuLmt = '500m'
          memLmt = '500Mi'
          if (images.containsKey('mavenCpuLmt')) {
            cpuLmt = images."mavenCpuLmt"
          }
          if (images.containsKey('mavenMemLmt')) {
            memLmt = images."mavenMemLmt"
          }
          this.inputcontainers  << 
            script.containerTemplate(
              name: 'maven', 
              image: images."maven",
              command: 'cat',
              envVars: [
                script.envVar(key: 'JAVA_TOOL_OPTIONS', value: "-Duser.home=${workingdir}"),
                script.envVar(key: 'MAVEN_CONFIG', value: "${workingdir}/.m2")
              ],
              ttyEnabled: true,
              workingDir: workingdir,
              alwaysPullImage: false,
              resourceRequestCpu: '100m',
              resourceLimitCpu: cpuLmt,
              resourceRequestMemory: '500Mi',
              resourceLimitMemory: memLmt
            )
        }
        if (images.containsKey('node')) {
          cpuLmt = '500m'
          memLmt = '500Mi'
          if (images.containsKey('nodeCpuLmt')) {
            cpuLmt = images."nodeCpuLmt"
          }
          if (images.containsKey('nodeMemLmt')) {
            memLmt = images."nodeMemLmt"
          }
          this.inputcontainers <<
            script.containerTemplate(
              name: 'node', 
              image: images."node",
              command: 'cat',
              envVars: [
                script.envVar(key: 'NPM_CONFIG_USERCONFIG', value: '/data/.npm/config/.npmrc')
              ],
              ttyEnabled: true,
              workingDir: workingdir,
              alwaysPullImage: false,
              resourceRequestCpu: '100m',
              resourceLimitCpu: cpuLmt,
              resourceRequestMemory: '500Mi',
              resourceLimitMemory: memLmt
            )
        }
        if (images.containsKey('nodetest')) {
          cpuLmt = '500m'
          memLmt = '500Mi'
          if (images.containsKey('nodetstCpuLmt')) {
            cpuLmt = images."nodetstCpuLmt"
          }
          if (images.containsKey('nodetstMemLmt')) {
            memLmt = images."nodetstMemLmt"
          }
          this.inputcontainers <<
            script.containerTemplate(
              name: 'nodetest', 
              image: images."nodetest",
              command: 'cat',
              envVars: [
                script.envVar(key: 'NPM_CONFIG_USERCONFIG', value: '/data/.npm/config/.npmrc')
              ],
              ttyEnabled: true,
              workingDir: workingdir,
              alwaysPullImage: false,
              resourceRequestCpu: '100m',
              resourceLimitCpu: cpuLmt,
              resourceRequestMemory: '500Mi',
              resourceLimitMemory: memLmt
            )
        }
        if (images.containsKey('occlient')) { 
          this.inputcontainers << 
            script.containerTemplate(
              name: 'occlient', 
              image: images."occlient",
              command: 'cat',
              ttyEnabled: true,
              workingDir: workingdir,
              alwaysPullImage: false,
              resourceRequestCpu: '100m',
              resourceLimitCpu: '500m',
              resourceRequestMemory: '200Mi',
              resourceLimitMemory: '800Mi'
            )
        }        
        if (images.containsKey('sonar')) {
          cpuLmt = '500m'
          memLmt = '500Mi'
          if (images.containsKey('sonarCpuLmt')) {
            cpuLmt = images."sonarCpuLmt"
          }
          if (images.containsKey('sonarMemLmt')) {
            memLmt = images."sonarMemLmt"
          }
          this.inputcontainers << 
            script.containerTemplate(
              name: 'sonar', 
              image: images."sonar",
              command: 'cat',
              envVars: [
                script.envVar(key: 'MAVEN_CONFIG', value: "${workingdir}/.m2")
              ],
              ttyEnabled: true,
              workingDir: workingdir,
              alwaysPullImage: false,
              resourceRequestCpu: '100m',
              resourceLimitCpu: cpuLmt,
              resourceRequestMemory: '500Mi',
              resourceLimitMemory: memLmt
            )
        }
  }
  public void BuilderTemplate (body) {
    script.podTemplate(
          label: podlabel,
          cloud: clustername,
          namespace: namespace,
          containers: this.inputcontainers,
          volumes: [
            script.secretVolume(secretName: 'maven-settings', mountPath: "${workingdir}/.m2"),
            script.secretVolume(secretName: 'npm-settings', mountPath: '/data/.npm/config'),
            script.secretVolume(secretName: 'helm-repository', mountPath: '/home/groot/helm/repository'),
            script.persistentVolumeClaim(claimName: "${m2FileSystem}", mountPath: "/data/.m2/repository"),
            script.persistentVolumeClaim(claimName: 'npm-storage', mountPath: '/data/.npm'),
            script.persistentVolumeClaim(claimName: "${buildWrkspace}", mountPath: '/jenkins-workspace'),
            script.emptyDirVolume(mountPath: '/home/groot/helm/repository/cache', memory: false)
          ]
       ){
            body ()
       }
  }
}