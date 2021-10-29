#!/usr/bin/bash
# api origin
origin="http://"
# bk_ticket
ticket=""
# enable? true or false
enable="false"
curl "${origin}"'/job-crontab/web/app/'"${1}"'/cron/job/'"${2}"'/status/?enable='"${enable}" \
  -X 'PUT' \
  -H 'Cookie: bk_ticket='"${ticket}" \
  --compressed \
  --insecure