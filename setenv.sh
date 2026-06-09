#!/usr/bin/env bash

export APP_DIR=/Users/deborahshapiro/Code/workspace-meme/meme-jdk17
export DATA_DIR=$APP_DIR/data
export INDEX_DIR=$APP_DIR/data/indexes-jdk17
export SOURCE_DATA_DIR=$APP_DIR/data

export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_NAME=ncimdb
export DB_USER=root
export DB_PASSWORD=

export SERVER_PORT=8080
export SERVER_CONTEXT_PATH=/umls-server-rest
export SERVER_SERVLET_CONTEXT_PATH=$SERVER_CONTEXT_PATH
export BASE_URL=http://localhost:8080/umls-server-rest

export DEPLOY_ENABLED_TABS=terminology,metadata,workflow,edit,admin,process,inversion
