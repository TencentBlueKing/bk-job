#!/bin/bash
set -e
echo 'Begin to package job'
BACKEND_MODULES=(job-config job-crontab job-execute job-gateway job-logsvr job-manage job-backup job-file-gateway job-ticket job-file-worker job-analysis)
FRONTEND_MODULES=(job-frontend)
ALL_MODULES=(job-config job-crontab job-execute job-gateway job-logsvr job-manage job-backup job-file-gateway job-ticket job-file-worker job-analysis job-frontend)

if [[ ! -d "release" ]]; then
	mkdir release
else
	rm -rf release/*
fi

usage () {
    cat <<EOF
用法: 
    $PROGRAM [ -h --help -?  查看帮助 ]
            [ -m, --module      [必选] "子模块(${PROJECTS[*]}), 逗号分隔。ALL表示全部都更新" ]
            [ -v, --version    [必选] "Job版本" ]
EOF
}

usage_and_exit () {
    usage
    exit "$1"
}

log () {
    echo "$@"
}

error () {
    echo "$@" 1>&2
    usage_and_exit 1
}

warning () {
    echo "$@" 1>&2
    EXITCODE=$((EXITCODE + 1))
}

# 解析命令行参数，长短混合模式
(( $# == 0 )) && usage_and_exit 1
while (( $# > 0 )); do 
    case "$1" in
        -m | --module )
            shift
            JOB_MODULES="$1"
            ;;
        -v | --version )
            shift
            JOB_VERSION="$1"
            ;;
        --help | -h | '-?' )
            usage_and_exit 0
            ;;
        -*)
            error "不可识别的参数: $1"
            ;;
        *) 
            break
            ;;
    esac
    shift 
done 

JOB_MODULES=${JOB_MODULES,,}          # to lower case
# 判断参数
if [[ -z $JOB_MODULES ]] || ! [[ $JOB_MODULES =~ ^[A-Za-z,-]+$ ]]; then
    warning "-m, --module必须指定要更新的模块名, 逗号分隔: 如job-config,job-logsvr"
fi

# 解析需要更新的模块，如果是all/ALL，则包含所有模块
if [[ $JOB_MODULES = all ]]; then
    PACKAGE_MODULES=("${ALL_MODULES[@]}")
else
    IFS=, read -ra PACKAGE_MODULES <<<"$JOB_MODULES"
fi

function packageJarAndScript()
{
	backend_module="$1"
	log "Packaging $backend_module ..."
	if [[ ! -d "release" ]]; then
		mkdir release
	fi
	
	mkdir -p release/job/backend/${backend_module}/bin

	cp src/backend/release/${backend_module}-${JOB_VERSION}.jar release/job/backend/${backend_module}/${backend_module}.jar
	cp scripts/${backend_module}/${backend_module}.sh release/job/backend/${backend_module}/bin/
	chmod 755 release/job/backend/${backend_module}/bin/${backend_module}.sh
	echo "Package ${backend_module} successfully"
}

# Package the back-end jar file and the corresponding execution script
for m in "${PACKAGE_MODULES[@]}"; do
	for BACKEND_MODULE in ${BACKEND_MODULES[@]}
	do
		[ "$BACKEND_MODULE" == "$m" ] && packageJarAndScript $m
	done
	# Package Upgrader
	cp src/backend/release/upgrader-${JOB_VERSION}.jar release/job/backend/upgrader-${JOB_VERSION}.jar
done

# Package versionLogs
if [ ! -d "src/frontend/release/job/frontend/static" ] 
then
	mkdir -p src/frontend/release/job/frontend/static
fi
if [ ! -d "release/job/frontend/static" ] 
then
	mkdir -p release/job/frontend/static
fi
cp versionLogs/bundledVersionLog*.json release/job/frontend/static
if [ ! -d "src/frontend/dist/*" ]
then
	mkdir -p src/frontend/dist/_init_
fi

# Package front-end static files
for m in "${PACKAGE_MODULES[@]}"; do
	for FRONTEND_MODULE in ${FRONTEND_MODULES[@]}
	do
		if [[ "$FRONTEND_MODULE" == "$m" ]]; then
			log "Packaging $m ..."
			mkdir -p release/job/frontend
			cp -r src/frontend/dist/* release/job/frontend/
			echo "Package frontend successfully"
		fi
	done
done


# Package support-files
log "Packaging support-files ..."
cp -r support-files/ release/job/
sed -i 's/job.edition=ee/job.edition=ce/g' "release/job/support-files/templates/#etc#job#job-gateway#job-gateway.properties"
sed -i 's/job.edition=ee/job.edition=ce/g' "release/job/support-files/templates/#etc#job#job-manage#job-manage.properties"
echo "Package support-files successfully"

# Package project documents
log "Packaging project doc ..."
cp README.md release/job/
cp projects.yaml release/job/
cp release.md release/job/
cp UPGRADE.md release/job/
cp VERSION release/job/
cp -r docs/ release/job/
echo "Package project doc successfully"

cd release
tar -czf "job_ce-${JOB_VERSION}.tgz" job

log "Package job successfully! File: job_ce-${JOB_VERSION}.tgz"
set +e
