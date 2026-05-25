NCI-META RESOURCE NOTES

This directory is no longer a packaged `config.properties` deployment source.
Runtime settings now come from:

- `src/main/resources/application.properties`
- environment variables supplied by Tomcat, Gradle, or a shell setup script

The remaining files in this directory are retained for narrower operational
purposes:

- `images/**` and `app/page/general/**` are used as deploy-specific web
  resource overlays by the Gradle `prepareWebapp` task.
- `migrate-table-generators.sql` and `validate-table-generators.sql` are
  operational database migration/validation helpers.
- `META/MRCOLS.RRF` and `META/MRFILES.RRF` support NCI-META metadata workflows.
- `bin/**` and `crontab.txt` are historical/operational scripts that still need
  separate owner review before removal or modernization.

The old Maven assembly descriptor for creating a `term-server-config-prod-nci-meta`
zip has been removed. Do not add a new `config.properties` here; add new runtime
configuration defaults to `src/main/resources/application.properties` instead.

TOMCAT DEPLOYMENT NOTES

Set production environment variables in Tomcat's `setenv.sh`, including the DB,
path, mail, and deploy values consumed by `application.properties`.

Typical examples:

- `APP_DIR`
- `DATA_DIR`
- `INDEX_DIR`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `BASE_URL`
- `DEPLOY_*`
- `MAIL_*`
- `SECURITY_*`

Build and deploy the WAR from the Gradle build output. The web overlay from this
directory is applied during `./gradlew war` and `./gradlew explodeWar`.
