#!/bin/tcsh -f
#
# Weekly MEME automation:
# 1. Rebuild deep_relationships tables.
# 2. Optionally restart the Tomcat service.

# Configuration
set APP_DIR = "/meme_work/ncim"
set MEME_BIN = "$APP_DIR/bin"
set DB_HOST = "127.0.0.1"
set DB_PORT = "3306"
set DB_NAME = "ncimdb"
set DB_USER = "root"
set MYSQL_BIN = "mysql"
set BASE_URL = "https://meme-edit.semantics.cancer.gov/ncim-server-rest"
set TOMCAT_SERVICE = "tomcat-evs-meme"
set RESTART_SERVER_AFTER_WEEKLY = "true"
set REBUILD_DEEP_RELS_SQL = "$MEME_BIN/rebuildDeepRels.sql"

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
  if (! -e "$REBUILD_DEEP_RELS_SQL") then
    echo "ERROR: REBUILD_DEEP_RELS_SQL not found: $REBUILD_DEEP_RELS_SQL"
    exit 1
  endif

  echo "  Rebuild the deep_relationships tables ...  `/bin/date`"
  $mysql < "$REBUILD_DEEP_RELS_SQL"
  if ($status != 0) then
    echo "ERROR: deep_relationships rebuild failed"
    exit 1
  endif

  if ("$RESTART_SERVER_AFTER_WEEKLY" == "true") then
    sudo service "$TOMCAT_SERVICE" stop
    if ($status != 0) then
      echo "ERROR: could not stop $TOMCAT_SERVICE ...  `/bin/date`"
      exit 1
    endif

    sudo service "$TOMCAT_SERVICE" start
    if ($status != 0) then
      echo "ERROR: could not start $TOMCAT_SERVICE ...  `/bin/date`"
      exit 1
    endif
  else
    echo "  Skipping restart because RESTART_SERVER_AFTER_WEEKLY=$RESTART_SERVER_AFTER_WEEKLY"
  endif
else
  echo "  DISABLED"
endif

echo "--------------------------------------------------------"
echo "Finished ... `/bin/date`"
echo "--------------------------------------------------------"
