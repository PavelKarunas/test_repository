node ('master') {
    
    stage('Preparation (Checking out)'){
        checkout scm
    }

     stage('Building') {
        git branch: 'master', url: 'https://github.com/PavelKarunas/test_repository.git'
        withMaven(maven: 'Maven 3.6.3') {
            sh "mvn clean package -U -f helloworld-project/helloworld-ws/pom.xml"
            sh "cp helloworld-project/helloworld-ws/target/helloworld-ws.war helloworld-ws.war"
            stash includes: 'helloworld-ws.war', name: 'our_stash'
        }
    }

     stage ('Sonar scan') {
        def scannerHome = tool 'Sonar 4.3.0'
        withSonarQubeEnv('Sonar 7.1.0') {
            sh "${scannerHome}/bin/sonar-scanner -Dsonar.projectKey=pkarunas_helloworld -Dsonar.sources=helloworld-project/helloworld-ws/src -Dsonar.java.binaries=helloworld-project/helloworld-ws/target"
        }
    }

    stage('Testing') {
        parallel(
                'pre-integration': {
                    sh 'echo "mvn pre-integration-test"'
                },
                'integration-test': {
                    withMaven(maven: 'Maven 3.6.3') {
                        sh "mvn -f helloworld-project/helloworld-ws/pom.xml integration-test"
                    }
                },
                'post-integration-test': {
                    sh 'echo "mvn post-integration-test"'
                }
        )
    }

    stage('Triggering job and fetching artefact after finishing'){
        build job: "MNTLAB-pkarunas-child1-build-job", parameters: [[$class: 'StringParameterValue', name: 'BRANCH_NAME', value: "pkarunas"]], wait: true
        copyArtifacts projectName: "MNTLAB-pkarunas-child1-build-job", selector: lastCompleted()
    }

    stage ('Packaging and Publishing results'){
            sh "tar -zxvf pkarunas_dsl_script.tar.gz"
            sh "tar -czf pipeline-pkarunas-${BUILD_NUMBER}.tar.gz output.txt Jenkinsfile helloworld-ws.war"
            nexusArtifactUploader artifacts: [[artifactId: 'pipeline-pkarunas', classifier: '', file: 'pipeline-pkarunas-${BUILD_NUMBER}.tar.gz', type: 'tar.gz']], credentialsId: 'Nexus_jenkins', groupId: 'Pipeline_Helloworld', nexusUrl: 'nexus.k8s.pkarunas.playpit.by:80', nexusVersion: 'nexus3', protocol: 'http', repository: 'MNT-pipeline', version: '${BUILD_NUMBER}'
            }
}

node ('Docker') {

    stage ('Creating Docker Image'){
                sh """
                    cat << "EOF" > Dockerfile
                    FROM alpine
                    RUN apk update && apk add wget tar openjdk8 && \
 	                wget https://archive.apache.org/dist/tomcat/tomcat-8/v8.5.20/bin/apache-tomcat-8.5.20.tar.gz && \
 	                tar -xvf apache-tomcat-8.5.20.tar.gz && \
 	                mkdir /opt/tomcat && \
 	                mv apache-tomcat*/* /opt/tomcat/
                    COPY helloworld-ws.war /opt/tomcat/webapps/
                    EXPOSE 8080
                    CMD ["/opt/tomcat/bin/catalina.sh", "run"]
                    """
                unstash 'our_stash'
                sh '''
                    docker build -t helloworld-pkarunas:${BUILD_NUMBER} .
                    IP=$(kubectl get po -n nexus -o wide | grep nexus-deploy | awk '{ print $6}'):8124;
                    docker login -u Jenkins -p 824atm21 $IP
                    docker tag helloworld-pkarunas:${BUILD_NUMBER} $IP/helloworld-pkarunas:${BUILD_NUMBER}
                    docker push $IP/helloworld-pkarunas:${BUILD_NUMBER}
                    '''
                }
                       
    stage('Asking for manual approval'){
        timeout(time: 5, unit: "MINUTES") {
            input message: 'Deploy this app?', ok: 'Yes'
        }
    }

    stage ('Deployment (rolling update, zero downtime)') {
        sh '''
        curl https://raw.githubusercontent.com/PavelKarunas/test_repository/master/deploy.yaml --output deploy.yaml
        IP=$(kubectl get po -n nexus -o wide | grep nexus-deploy | awk '{ print $6}'):8124;
        sed -i "s|replace_string|$IP/helloworld-pkarunas:${BUILD_NUMBER}|" deploy.yaml
        kubectl apply -f deploy.yaml
        '''
    }
}
