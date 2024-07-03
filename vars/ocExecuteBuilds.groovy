#!/usr/bin/groovy

def call(buildInfos = [:]) {
  def builds = buildInfos.collectEntries { name, contextDir ->
    [ 
      "${name}": {
        sh """
          oc start-build ${name} --from-dir=${contextDir} --wait --follow
        """
      }
    ]
  }  
  parallel builds 
}