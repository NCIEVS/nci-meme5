NCI-META RESOURCE NOTES

This directory is no longer a packaged `config.properties` deployment source.
Runtime settings now come from:

- `src/main/resources/application.properties`
- environment variables supplied by systemd, Gradle, or a shell setup script

The remaining files in this directory are retained for narrower operational
purposes:

- `images/**` and `app/page/general/**` are used as deploy-specific web
  resource overlays by the Gradle `prepareWebapp` task.
- `migrate-table-generators.sql` and `validate-table-generators.sql` are
  operational database migration/validation helpers.
- `META/MRCOLS.RRF` and `META/MRFILES.RRF` support NCI-META metadata workflows.
- `bin/**` and `crontab.txt` are operational scripts. NM-310 modernizes the
  retained production scripts in place with visible per-script configuration
  blocks. `load.csh` was retired because the old Maven load flow is now covered
  by Gradle admin tasks.

The old Maven assembly descriptor for creating a `term-server-config-prod-nci-meta`
zip has been removed. Do not add a new `config.properties` here; add new runtime
configuration defaults to `src/main/resources/application.properties` instead.

SPRING BOOT DEPLOYMENT NOTES

NCI-META production now runs as a Spring Boot executable WAR with embedded
Tomcat. Do not deploy the WAR under the old external Tomcat `webapps` directory.
The external `tomcat-evs-meme` service should be stopped/disabled so the Boot
service can own port 8080.

Build the executable WAR from the checked-out branch:

```
./gradlew clean bootWar
```

Install the built artifact outside the old Tomcat tree:

```
mkdir -p /local/content/MEME/MEME5/ncim/deploy
cp build/libs/ROOT-2.0.0-SNAPSHOT-webapp.war \
  /local/content/MEME/MEME5/ncim/deploy/nci-meme5-webapp.war
```

The `nci-meme5` systemd service should source the server-owned production
environment file before starting Java:

```
/local/content/MEME/MEME5/ncim/setenv.sh
```

That file should contain only production overrides, such as:

- `APP_DIR`
- `DATA_DIR`
- `INDEX_DIR`
- `SOURCE_DATA_DIR`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `SERVER_PORT=8080`
- `SERVER_SERVLET_CONTEXT_PATH=/ncim-server-rest`
- `BASE_URL`
- `DEPLOY_*`
- `MAIL_*`
- `SECURITY_*`

Expected service command:

```
/bin/bash -lc 'source /local/content/MEME/MEME5/ncim/setenv.sh && exec java -Dcatalina.base=/local/content/MEME/MEME5/ncim -jar /local/content/MEME/MEME5/ncim/deploy/nci-meme5-webapp.war'
```

The `catalina.base` value is retained for legacy Log4j paths, especially
`logs/user_activity.log`. Ensure this directory exists and is writable by the
service user:

```
/local/content/MEME/MEME5/ncim/logs
```

Main console output should be captured by systemd or redirected to an app log,
for example:

```
/local/content/MEME/MEME5/ncim/logs/nci-meme5.out
```

If a file is used for stdout/stderr, configure log rotation for it. The public
URL should continue routing through the existing proxy:

```
https://meme-edit.semantics.cancer.gov/ncim-server-rest
  -> http://ncias-q3793-c.nci.nih.gov:8080/ncim-server-rest
```

The web overlay from this directory is applied during `./gradlew war`,
`./gradlew bootWar`, and `./gradlew explodeWar`.
