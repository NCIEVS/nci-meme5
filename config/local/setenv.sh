#!/usr/bin/env bash

# Local environment bootstrap for NM-278 configuration migration.
# Source this file before running Gradle tasks or local app startup:
#   source config/local/setenv.sh
#
# Required for most local flows:
#   APP_DIR, DATA_DIR, DB_HOST, DB_PORT, DB_NAME, DB_USER
#
# Optional overrides:
#   DB_PASSWORD, INDEX_DIR, LVG_DIR, BASE_URL, MAIL_*, DEPLOY_*, SECURITY_*
#
# These variables map directly into src/main/resources/application.properties.

set -a

# Resolve the sourced script location in both bash and zsh.
if [ -n "${BASH_SOURCE[0]:-}" ]; then
  _SCRIPT_SOURCE="${BASH_SOURCE[0]}"
elif [ -n "${(%):-%N:-}" ]; then
  _SCRIPT_SOURCE="${(%):-%N}"
else
  _SCRIPT_SOURCE="$0"
fi

SCRIPT_DIR="$(cd "$(dirname "${_SCRIPT_SOURCE}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
WORKSPACE_ROOT="$(cd "${REPO_ROOT}/.." && pwd)"

# Core app/data layout
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-local}"
export CONFIG_LEGACY_RUN_CONFIG_ENABLED="${CONFIG_LEGACY_RUN_CONFIG_ENABLED:-false}"
export APP_DIR="${APP_DIR:-$WORKSPACE_ROOT/meme-jdk17}"
export DATA_DIR="${DATA_DIR:-$APP_DIR/data}"
export INDEX_DIR="${INDEX_DIR:-$DATA_DIR/indexes-jdk17}"
export LVG_DIR="${LVG_DIR:-$DATA_DIR/lvg2020}"
export SOURCE_DATA_DIR="${SOURCE_DATA_DIR:-$DATA_DIR}"

# Database
export DB_HOST="${DB_HOST:-127.0.0.1}"
export DB_PORT="${DB_PORT:-3306}"
export DB_NAME="${DB_NAME:-ncimdb}"
export DB_USER="${DB_USER:-root}"
export DB_PASSWORD="${DB_PASSWORD:-}"
export DB_DRIVER="${DB_DRIVER:-com.mysql.jdbc.Driver}"
export DB_SERVER_TIMEZONE="${DB_SERVER_TIMEZONE:-UTC}"
export DB_MAX_POOL_SIZE="${DB_MAX_POOL_SIZE:-64}"
export DB_MIN_POOL_SIZE="${DB_MIN_POOL_SIZE:-5}"
export DB_IDLE_TIMEOUT_MS="${DB_IDLE_TIMEOUT_MS:-3600000}"
export DB_CONNECTION_TIMEOUT_MS="${DB_CONNECTION_TIMEOUT_MS:-30000}"
export DB_MAX_LIFETIME_MS="${DB_MAX_LIFETIME_MS:-3600000}"
export DB_VALIDATION_TIMEOUT_MS="${DB_VALIDATION_TIMEOUT_MS:-5000}"
export DB_LEAK_DETECTION_THRESHOLD_MS="${DB_LEAK_DETECTION_THRESHOLD_MS:-0}"
export DB_POOL_NAME="${DB_POOL_NAME:-NciMemeHikariCPPool}"

# Web/deploy defaults
export SERVER_PORT="${SERVER_PORT:-8080}"
export SERVER_CONTEXT_PATH="${SERVER_CONTEXT_PATH:-/umls-server-rest}"
export SERVER_SERVLET_CONTEXT_PATH="${SERVER_SERVLET_CONTEXT_PATH:-$SERVER_CONTEXT_PATH}"
export BASE_URL="${BASE_URL:-http://localhost:${SERVER_PORT}${SERVER_CONTEXT_PATH}}"
export RUN_CONFIG_LABEL="${RUN_CONFIG_LABEL:-umls}"
export DEPLOY_LINK="${DEPLOY_LINK:-http://www.westcoastinformatics.com}"
export DEPLOY_COPYRIGHT="${DEPLOY_COPYRIGHT:-Copyright @2026}"
export DEPLOY_FEEDBACK_EMAIL="${DEPLOY_FEEDBACK_EMAIL:-info@westcoastinformatics.com}"
export DEPLOY_PASSWORD_RESET="${DEPLOY_PASSWORD_RESET:-http://passwordreset.example.com}"
export DEPLOY_PRESENTED_BY="${DEPLOY_PRESENTED_BY:-Presented by <a href=\"http://www.westcoastinformatics.com\">West Coast Informatics, LLC</a>}"
export DEPLOY_ENABLED_TABS="${DEPLOY_ENABLED_TABS:-workflow,edit,admin,process,inversion}"
export DEPLOY_LANDING_ENABLED="${DEPLOY_LANDING_ENABLED:-true}"
export DEPLOY_LICENSE_ENABLED="${DEPLOY_LICENSE_ENABLED:-true}"
export DEPLOY_LOGIN_ENABLED="${DEPLOY_LOGIN_ENABLED:-true}"
export DEPLOY_SIMPLEEDIT_ENABLED="${DEPLOY_SIMPLEEDIT_ENABLED:-false}"
export DEPLOY_TRACKING_CODE="${DEPLOY_TRACKING_CODE:-<!-- sample tracking code -->}"
export DEPLOY_COOKIE_CODE="${DEPLOY_COOKIE_CODE:-}"
export SITE_VERIFICATION_FILE="${SITE_VERIFICATION_FILE:-google3aef83c7ba606df3.html}"
export SITE_TRACKING_CODE="${SITE_TRACKING_CODE:-<!-- sample tracking code -->}"
export CYGWIN_BIN="${CYGWIN_BIN:-c:/cygwin64/bin}"

# Search/security/local user defaults
export ATOMCLASS_ACRONYMS_FILE="${ATOMCLASS_ACRONYMS_FILE:-$DATA_DIR/acronyms.txt}"
export ATOMCLASS_SPELLING_FILE="${ATOMCLASS_SPELLING_FILE:-$DATA_DIR/spelling.txt}"
export ATOMCLASS_SPELLING_INDEX="${ATOMCLASS_SPELLING_INDEX:-$INDEX_DIR/spelling}"
export SECURITY_USERS_ADMIN="${SECURITY_USERS_ADMIN:-author,admin,admin1,admin2,admin3,BAC,RAW}"
export SECURITY_USERS_USER="${SECURITY_USERS_USER:-author1,author2,author3,reviewer1,reviewer2,reviewer3,LAR,CFC,DSS}"
export SECURITY_USERS_VIEWER="${SECURITY_USERS_VIEWER:-guest}"
export UTS_LICENSE_CODE="${UTS_LICENSE_CODE:-NLM-XXX}"
export UTS_URL="${UTS_URL:-https://uts-ws.nlm.nih.gov/restful/isValidUMLSUser}"
export ADMIN_USER="${ADMIN_USER:-admin}"
export ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin}"
export VIEWER_USER="${VIEWER_USER:-guest}"
export VIEWER_PASSWORD="${VIEWER_PASSWORD:-guest}"
export BAD_USER="${BAD_USER:-i_am_a_bad_user}"
export BAD_PASSWORD="${BAD_PASSWORD:-i_am_a_bad_password}"

# Mail connection settings. Profile files control whether mail is enabled.
export MAIL_USER="${MAIL_USER:-}"
export MAIL_FROM="${MAIL_FROM:-mail@westcoastinformatics.com}"
export MAIL_PASSWORD="${MAIL_PASSWORD:-}"
export MAIL_HOST="${MAIL_HOST:-mail.westcoastinformatics.com}"
export MAIL_PORT="${MAIL_PORT:-587}"
export MAIL_STARTTLS_ENABLE="${MAIL_STARTTLS_ENABLE:-false}"
export MAIL_SMTP_AUTH="${MAIL_SMTP_AUTH:-true}"
export MAIL_TO="${MAIL_TO:-}"

set +a

echo "Loaded local NM-278 environment from ${SCRIPT_DIR}/setenv.sh"
echo "  SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}"
echo "  CONFIG_LEGACY_RUN_CONFIG_ENABLED=${CONFIG_LEGACY_RUN_CONFIG_ENABLED}"
echo "  APP_DIR=${APP_DIR}"
echo "  DATA_DIR=${DATA_DIR}"
echo "  INDEX_DIR=${INDEX_DIR}"
echo "  SOURCE_DATA_DIR=${SOURCE_DATA_DIR}"
echo "  DB_NAME=${DB_NAME}"
echo "  BASE_URL=${BASE_URL}"
