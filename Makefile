#####################################################################################
#   Makefile                                                                        #
#                                                                                   #
#   COPYRIGHT FUJITSU LIMITED 2021                                                  #
#####################################################################################

SHELL=bash

#####################################################################################
#                                                                                   #
#  to configure proxy,                                                              #
#  set http_proxy, https_proxy and JAVA_OPTS environment var as usual               #
#                                                                                   #
#   export http_proxy='..'                                                          #
#   export JAVA_OPTS='...'                                                          #
#                                                                                   #
#  DON'T WRITE THEM DOWN ANYWHERE, EVEN IN THIS FILE, docker/* docker-compose.yml   #
#                                                                                   #
#                                                                                   #
#####################################################################################

# define params >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

# define names for pom.xml and java packages
pom_gid = com.fujitsu
pom_artifactId = cdlfrontserver
package_base = ${pom_gid}.${pom_artifactId}
package_api = ${package_base}.api
package_model = ${package_base}.model

# REST API port on host
# HTTP PORT
RestPortHTTP=3000
# HTTPS PORT
RestPortHTTPS=3443

# files and directries in host machine >>>>>>>>>>>>>

# api_yaml: API definition
api_yaml=${PWD}/api/cdl2-api-sip2020.yaml

# dirs in host. don't change this.
dir_maven_repo_local=${PWD}/maven-repo-local
dir_codegen=${PWD}/codegen
dir_build=${PWD}/build
dir_impl=${PWD}/override.d
dir_cert=${PWD}/cert

# certification  parameters
cert_pass=changeit
cert_days=3650
cert_opt=-subj "/O=CDL Test/CN=cdlfrontserver.org01.cdl.com" -addext "subjectAltName=DNS:localhost,DNS:cdlfrontserver,IP:10.130.100.58"
cert_alias=jetty

# docker images to use
codegenBaseImg=openapitools/openapi-generator-cli:v5.2.0
mavenImg=maven:3-openjdk-8

# option to codegen
codegenOpt= --group-id      ${pom_gid} --artifact-id   ${pom_artifactId} \
	    --api-package   ${package_api} \
	    --model-package ${package_model} \
	    --import-mappings=DateTime=java.time.LocalDateTime --type-mappings=DateTime=java.time.LocalDateTime

imgTag=v2-kunipro

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<  define params


# targets

all:: compile genCert buildImg start

compile:
	# pull docker image
	docker pull ${codegenBaseImg}
	docker pull ${mavenImg}

	# genrate codes from open api yaml file
	docker run -it --rm -e TZ="Asia/Tokyo" \
	   -v ${api_yaml}:/api.yaml:ro \
	   -v ${dir_codegen}:/codegen \
	   ${codegenBaseImg} generate -g jaxrs-jersey -i /api.yaml -o /codegen ${codegenOpt}

	# setup files into ${dir_build} for compiling by maven
	cp -p -r ${dir_codegen}/pom.xml ${dir_codegen}/src                ${dir_build}/  # copy auto generated files
	cp -p -r ${dir_impl}/*  ${PWD}/pom.xml  ${PWD}/connection.yaml    ${dir_build}/  # override  files

	# compile by maven
	docker run -it --rm  \
		-v ${dir_build}:${dir_build} -w ${dir_build} \
		-v ${dir_maven_repo_local}:${dir_maven_repo_local} -e MAVEN_OPTS="-Dmaven.repo.local=${dir_maven_repo_local} ${JAVA_OPTS}" \
		${mavenImg} mvn package

	# download all dependencies before starting container
	docker run -it --rm  \
		-v ${dir_build}:${dir_build} -w ${dir_build} \
		-v ${dir_maven_repo_local}:${dir_maven_repo_local} -e MAVEN_OPTS="-Dmaven.repo.local=${dir_maven_repo_local} ${JAVA_OPTS}" \
		${mavenImg} mvn de.qaware.maven:go-offline-maven-plugin:resolve-dependencies

genCert:
	# generate self-signed certification in p12 format ${dir_cert}/jetty-ssl.p12
	openssl req -x509 -newkey rsa:4096 -batch -nodes -keyout ${dir_cert}/jetty-ssl-key.pem -out ${dir_cert}/jetty-ssl-cert.pem -days ${cert_days} ${cert_opt}
	openssl pkcs12 -export -passout pass:${cert_pass} -in ${dir_cert}/jetty-ssl-cert.pem -inkey ${dir_cert}/jetty-ssl-key.pem -out ${dir_cert}/jetty-ssl.p12 -name ${cert_alias}
	# generate java keystore  from certification      ${dir_cert}/jetty-ssl.p12 => ${dir_build}/target/jetty-ssl.keystore
        # password of deststorepass 'changeit' is defined in override.d/src/main/resources/jetty-ssl.xml
	docker run -it --rm -v ${dir_cert}/jetty-ssl.p12:/jetty-ssl.p12:ro -v ${dir_build}/target:/certout \
		${mavenImg} keytool -importkeystore -srckeystore /jetty-ssl.p12 -srcstoretype pkcs12 \
		-alias ${cert_alias} -srcstorepass ${cert_pass} -deststorepass changeit -destalias ${cert_alias} -destkeystore /certout/jetty-ssl.keystore \
		-deststoretype pkcs12 -noprompt


# start reqires compile and genCert
start: ${dir_build}/pom.xml ${dir_build}/target/jetty-ssl.keystore
	mavenImg=${mavenImg} \
	dir_maven_repo_local=${dir_maven_repo_local} \
	containerName=${pom_artifactId} hostName=${pom_artifactId} \
	RestPortHTTP=${RestPortHTTP} \
	RestPortHTTPS=${RestPortHTTPS} \
	dir_build=${dir_build}  \
	docker-compose up -d

stop:
	mavenImg=${mavenImg}  \
	dir_maven_repo_local=${dir_maven_repo_local} \
	dir_build=${dir_build}  \
	RestPortHTTP=${RestPortHTTP} \
	RestPortHTTPS=${RestPortHTTPS} \
	docker-compose down -v

clean: stop
	sudo rm -rf ${dir_codegen}/* ${dir_codegen}/.openapi-generator*
	sudo rm -rf ${dir_cert}/*
	sudo rm -rf ${dir_maven_repo_local}/*
	sudo rm -rf ${dir_build}/*
	sudo rm -rf ${PWD}/logs/*
	docker rmi -f ${mavenImg}
	docker rmi -f ${codegenBaseImg}


${dir_build}/connection.yaml: connection.yaml
	cp -p connection.yaml ${dir_build}/connection.yaml
buildImg: ${dir_build}/pom.xml ${dir_build}/connection.yaml
	docker build -f docker/Dockerfile \
	  --build-arg mavenImg=${mavenImg} \
	  --build-arg dir_top=${PWD} --build-arg dir_build=`basename ${dir_build}` --build-arg dir_maven_repo_local=`basename ${dir_maven_repo_local}` \
	  -t ${pom_artifactId}:${imgTag} ./
