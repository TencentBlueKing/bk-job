#!/usr/bin/bash
# api origin
origin="http://"
# bk_ticket
ticket=""
curl "${origin}"'/job-execute/web/execution/app/'"${1}"'/taskInstance/'"${2}"'/terminate' \
  -X 'POST' \
  -H 'Cookie: bk_ticket='"${ticket}" \
  --compressed \
  --insecure