#!/bin/tcsh -f
#
# Archive Lucene indexes locally and upload the archive to S3.

# Configuration
set APP_DIR = "/meme_work/ncim"
set INDEX_DIR = "/local/content/MEME/MEME5/ncim/data/indexes"
set INDEX_ARCHIVE_DIR = "$APP_DIR/archive/indexes"
set DB_HOST = "127.0.0.1"
set DB_PORT = "3306"
set DB_NAME = "ncimdb"
set DB_USER = "root"
set MYSQL_BIN = "mysql"
set AWS_BIN = "aws"
set AWS_PROFILE = "meme"
set S3_BUCKET_NAME = "nci-evs-meme"
set S3_BUCKET = "s3://$S3_BUCKET_NAME"

if (! $?DB_PASSWORD) setenv DB_PASSWORD ""
if ("$DB_PASSWORD" == "") then
  set mysql = ( "$MYSQL_BIN" -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$DB_NAME" )
else
  set mysql = ( "$MYSQL_BIN" -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "-p$DB_PASSWORD" "$DB_NAME" )
endif
if ("$AWS_PROFILE" == "") then
  set aws = ( "$AWS_BIN" )
else
  set aws = ( "$AWS_BIN" --profile "$AWS_PROFILE" )
endif

echo "--------------------------------------------------------"
echo "Starting `/bin/date`"
echo "--------------------------------------------------------"
echo "APP_DIR = $APP_DIR"
echo "INDEX_DIR = $INDEX_DIR"
echo "INDEX_ARCHIVE_DIR = $INDEX_ARCHIVE_DIR"
echo "S3_BUCKET = $S3_BUCKET"

set enabled = `echo "select if(automationsEnabled,'true','false') from projects;" | $mysql | tail -1`
set projectId = `echo "select id from projects;" | $mysql | tail -1`

echo "project: $projectId"
echo "enabled: $enabled"
echo ""

if ("$enabled" == "true") then
  if (! -d "$INDEX_DIR") then
    echo "ERROR: INDEX_DIR must exist: $INDEX_DIR"
    exit 1
  endif

  mkdir -p "$INDEX_ARCHIVE_DIR"
  cd "$INDEX_ARCHIVE_DIR"

  if ($#argv == 1) then
    set dayofweek = 'manual'
    set todaysdate = `date +"manual_%Y%m%d"`
  else
    set dayofweek = `date +"%a"`
    set todaysdate = `date +"%a_%Y%m%d"`
  endif

  echo "todaysdate: $todaysdate"

  mkdir "$INDEX_ARCHIVE_DIR/$todaysdate"
  cp -R "$INDEX_DIR"/* "$INDEX_ARCHIVE_DIR/$todaysdate"

  cd "$INDEX_ARCHIVE_DIR/$todaysdate"
  tar -cvf "$todaysdate.tar" *

  set fileExists = `$aws s3api list-objects-v2 --bucket "$S3_BUCKET_NAME" --max-items 10 --prefix "indexes/$dayofweek" --output json | jq -r '.Contents | .[] |[.Key]' | grep "$dayofweek" | wc -l`
  if ($fileExists == 1) then
    echo "INFO: replacing $dayofweek indexes file"
    set lastWeekFile = `$aws s3api list-objects-v2 --bucket "$S3_BUCKET_NAME" --max-items 10 --prefix "indexes/$dayofweek" --output json | jq -r '.Contents | .[0] |[.Key][0]' | grep "$dayofweek" | perl -pe 's/"//g;'`
    echo "INFO: lastWeekFile to remove: $lastWeekFile"
    $aws s3 rm "$S3_BUCKET/$lastWeekFile"
  else
    echo "INFO: no previous $dayofweek indexes file"
  endif

  $aws s3 cp "$todaysdate.tar" "$S3_BUCKET/indexes/$todaysdate.tar"

  rm "$todaysdate.tar"
  rm -rf "$INDEX_ARCHIVE_DIR/$todaysdate"
else
  echo "  DISABLED"
endif

echo "--------------------------------------------------------"
echo "Finished ... `/bin/date`"
echo "--------------------------------------------------------"
