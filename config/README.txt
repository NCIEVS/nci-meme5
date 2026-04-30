NM-278 CONFIGURATION NOTE

The legacy packaged `config.properties` approach under `config/**` has been
retired for normal local and application startup.

Preferred configuration path:

- shared defaults in `src/main/resources/application.properties`
- environment/profile overrides in `src/main/resources/application-*.properties`
- local developer setup through `config/local/setenv.sh`

The old `config/dev-windows` packaged config files have been removed. Their
remaining useful handler aliases and UI flags were folded into the Spring-style
application properties.

The remaining `config/prod-nci-meta` resources are no longer a runtime
`config.properties` source. They currently serve two narrower purposes:

- deploy-specific web resource overlays, such as images and
  `app/page/general/**`
- operational scripts, metadata support files, and table-generator migration
  SQL that still need separate review before cleanup

New configuration changes should be made in the Spring-style property files
first. New web branding/content overrides should be added through the explicit
web-resource overlay path in `build.gradle`, not by reviving packaged
`config.properties` files.
