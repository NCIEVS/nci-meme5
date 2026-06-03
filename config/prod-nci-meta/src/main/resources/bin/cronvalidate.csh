#!/bin/tcsh -f
#
# Run the MID Validation Report process from cron.

# Configuration
set APP_DIR = "/meme_work/ncim"
set DB_HOST = "127.0.0.1"
set DB_PORT = "3306"
set DB_NAME = "ncimdb"
set DB_USER = "root"
set MYSQL_BIN = "mysql"
set BASE_URL = "https://meme-edit.semantics.cancer.gov/ncim-server-rest"
set ADMIN_USER = "admin"
set ADMIN_PASSWORD = "admin"
set CRONVALIDATE_PROCESS_NAME = "MID Validation Report"

if (! $?DB_PASSWORD) setenv DB_PASSWORD ""
if ("$DB_PASSWORD" == "") then
  set mysql = ( "$MYSQL_BIN" -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$DB_NAME" )
else
  set mysql = ( "$MYSQL_BIN" -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "-p$DB_PASSWORD" "$DB_NAME" )
endif

echo "--------------------------------------------------------"
echo "Starting `/bin/date`"
echo "--------------------------------------------------------"
echo "APP_DIR = $APP_DIR"
echo "DB_NAME = $DB_NAME"
echo "BASE_URL = $BASE_URL"

set enabled = `echo "select if(automationsEnabled,'true','false') from projects;" | $mysql | tail -1`
set projectId = `echo "select id from projects;" | $mysql | tail -1`

echo "project: $projectId"
echo "enabled: $enabled"
echo ""

if ("$enabled" == "true") then
  echo "  Login ... `/bin/date`"
  set authToken = `curl -H "Content-type: text/plain" -X POST -d "$ADMIN_PASSWORD" "$BASE_URL/security/authenticate/$ADMIN_USER" | perl -pe 's/.*"authToken":"([^"]*).*/$1/;'`

  echo "  Run $CRONVALIDATE_PROCESS_NAME... `/bin/date`"
  set processId = `echo "select id from process_configs where name='$CRONVALIDATE_PROCESS_NAME';" | $mysql | tail -1`
  set executionId = `curl -H "Content-type: application/json" -H "Authorization: $authToken" -X GET "$BASE_URL/process/config/$processId/prepare?projectId=$projectId"`
  sleep 2
  curl -H "Content-type: application/json" -H "Authorization: $authToken" -X GET "$BASE_URL/process/execution/$executionId/execute?projectId=$projectId&background=true"
else
  echo "  DISABLED"
endif

echo "--------------------------------------------------------"
echo "Finished ... `/bin/date`"
echo "--------------------------------------------------------"
