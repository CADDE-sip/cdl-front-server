ARG mavenImg
FROM  ${mavenImg}
ARG mavenImg

ARG  dir_top
ARG  dir_maven_repo_local
ARG  dir_build


RUN  mkdir -p /var/log/cdl ${dir_top}

COPY ${dir_maven_repo_local}  ${dir_top}/${dir_maven_repo_local}
COPY ${dir_build}             ${dir_top}/${dir_build}
RUN  rm -rf                   ${dir_top}/${dir_build}/src/*/java

WORKDIR ${dir_top}/build
ENV     MAVEN_OPTS="-Dmaven.repo.local=${dir_top}/${dir_maven_repo_local}"
ENV     JAVA_OPTS=
CMD     mvn ${MAVEN_OPTS} ${JAVA_OPTS} jetty:run

LABEL   baseImage = "${mavenImg}"
