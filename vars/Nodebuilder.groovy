#!/usr/bin/groovy

public class Nodebuilder implements Serializable {
	    public String podlabel
        public String builderimage
        public String workingdir
        def script
        public Nodebuilder(String label, String builderimage, String workingdir, script) {
            this.podlabel=label
            this.builderimage=builderimage
            this.workingdir=workingdir
            this.script=script
        }
        public void NodebuilderTemplate (body) {    
            script.podTemplate(
                label: podlabel,
                containers: [
                    script.containerTemplate(
                    name: 'node', 
                    image: builderimage,
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