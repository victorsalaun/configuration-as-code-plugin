FROM maven:3.5-jdk-8-slim

ENV prj=/usr/src/casc

WORKDIR ${prj}

VOLUME ["/root/.m2"]

ADD . .

# Build hpi file for jenkins
RUN mvn clean package -DskipTests

FROM jenkins:2.60.3

ARG JAVA_OPTS
ENV JAVA_OPTS "-Djenkins.install.runSetupWizard=false ${JAVA_OPTS:-}"

##### JENKINS SETUP
ENV JENKINS_UC https://updates.jenkins-ci.org
#ENV JENKINS_REF /usr/share/jenkins/ref
ENV JENKINS_HOME /var/jenkins_home
#ENV JENKINS_WAR /usr/share/jenkins/jenkins.war

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN xargs /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt

COPY --from=0 /usr/src/casc/target/configuration-as-code.hpi /usr/share/jenkins/ref/plugins