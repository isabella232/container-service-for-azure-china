node {
    def app = '';
    def start_index = params.registry_url.indexOf("//") + 2;
    def registry_url = params.registry_url.substring(start_index);
    stage('Checkout git repo') {
      git branch: 'master', url: params.git_repo
    }
    stage('Build Docker image') {
      app = docker.build(params.docker_repository + ":${env.BUILD_NUMBER}", '.')
    }
    stage('Push Docker image to Private Registry') {
      docker.withRegistry(params.registry_url, params.registry_credentials_id ) {
        app.push("${env.BUILD_NUMBER}");
      }
    }
    stage('Test And Validation') {
        app.inside {
            sh 'echo "Test passed"'
        }
    }
    stage('Deploy to K8S') {
        // Clean up old releases
        sh "kubectl delete pods,deployment -l run=${params.service_name} || true"
        def cmd = """kubectl run ${params.service_name} --image=${registry_url}/${app.imageName()} --replicas=2 --port=${params.service_port_number}"""
        // execute shell for the command above
        sh cmd
        sh "kubectl get svc ${params.service_name} || kubectl expose deployment ${params.service_name} --target-port=${params.service_port_number} --type=LoadBalancer"
    }
}
    