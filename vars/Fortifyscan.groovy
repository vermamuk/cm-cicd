public class Fortifyscan implements Serializable {
	    public String podlabel
        public String builderimage
        public String workingdir
        def script
        public Fortifyscan(String label, String builderimage, String workingdir, script) {
            this.podlabel=label
            this.builderimage=builderimage
            this.workingdir=workingdir
            this.script=script
        }
        public void FortifyscanTemplate (body) {    
            script.podTemplate(
                label: podlabel,
                containers: [
                    script.containerTemplate(
                    name: 'fortify', 
                    image: builderimage,
                    command: 'cat', 
                    ttyEnabled: true,
                    workingDir: workingdir,
                    alwaysPullImage: false,
                    resourceRequestCpu: '500m',
                    resourceLimitCpu: '4',
                    resourceRequestMemory: '8Gi',
                    resourceLimitMemory: '64Gi'
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