public class Dockerbuild implements Serializable {
	    public String podlabel
        public String builderimage
        public String workingdir
        def script
        public Dockerbuild(String label, String builderimage, String workingdir, script) {
            this.podlabel=label
            this.builderimage=builderimage
            this.workingdir=workingdir
            this.script=script
        }
        public void DockerbuildTemplate (body) {    
            script.podTemplate(
                label: podlabel,
                containers: [
                    script.containerTemplate(
                    name: 'occlient', 
                    image: builderimage,
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
                    script.persistentVolumeClaim(claimName: 'build-workspace', mountPath: '/jenkins-workspace')
                ]
            ){
                    body ()
            }
        }
}