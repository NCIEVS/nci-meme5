/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.helpers;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.Authenticator;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status.Family;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.log4j.Logger;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.hibernate.Hibernate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;
import com.google.common.primitives.UnsignedBytes;
import com.wci.umls.server.model.content.Component;

/**
 * Utility class for interacting with the configuration, serializing to JSON/XML
 * and other purposes.
 */
public class ConfigUtility {

  /** The Constant DEFAULT. */
  public static final String DEFAULT = "DEFAULT";

  /** The Constant ATOMCLASS (search handler for atoms). */
  public static final String ATOMCLASS = "ATOMCLASS";

  /** The date format. - legacy */
  public static final FastDateFormat DATE_FORMAT =
      FastDateFormat.getInstance("yyyyMMdd");

  /** The Constant DATE_YYYYMMDD. */
  public static final FastDateFormat DATE_YYYYMMDD =
      FastDateFormat.getInstance("yyyyMMdd");

  /** The Constant DATE_FORMAT2. */
  public static final FastDateFormat DATE_YYYY_MM_DD =
      FastDateFormat.getInstance("yyyy_MM_dd");

  /** The Constant DATE_FORMAT3. */
  public static final FastDateFormat DATE_YYYY =
      FastDateFormat.getInstance("yyyy");

  /** The Constant DATE_FORMAT4. */
  public static final FastDateFormat DATE_FORMAT4 =
      FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

  /** The Constant DATE_FORMAT5. */
  public static final FastDateFormat DATE_FORMAT5 =
      FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSS");

  /** The Constant DATE_FORMAT4. */
  public static final FastDateFormat DATE_YYYYMMDDHHMMSS =
      FastDateFormat.getInstance("yyyyMMddHHmmss");

  /** The display timezone property. */
  public static final String DISPLAY_TIME_ZONE_PROPERTY =
      "app.display.timezone";

  /** The default display timezone. */
  public static final String DEFAULT_DISPLAY_TIME_ZONE = "America/New_York";

  /** The display timestamp pattern. */
  public static final String DISPLAY_TIMESTAMP_PATTERN =
      "yyyy-MM-dd HH:mm:ss.SSS z";

  /** REST client allowed-hosts property. */
  public static final String REST_CLIENT_ALLOWED_HOSTS_PROPERTY =
      "rest.client.allowed.hosts";

  /** Database client allowed-hosts property. */
  public static final String DATABASE_ALLOWED_HOSTS_PROPERTY =
      "database.allowed.hosts";

  /** Source data directory property. */
  public static final String SOURCE_DATA_DIR_PROPERTY = "source.data.dir";

  /** Default loopback host allowlist. */
  private static final String DEFAULT_LOOPBACK_HOSTS =
      "localhost,127.0.0.1,::1";

  /** REST base URL shape accepted before host allowlist validation. */
  private static final String REST_BASE_URL_PATTERN =
      "(?i)https?://[^\\s/?#@]+(?::[0-9]{1,5})?(?:/[^?#\\s]*)?";

  /** MySQL JDBC URL shape accepted before host allowlist validation. */
  private static final String MYSQL_JDBC_URL_PATTERN =
      "(?i)jdbc:mysql://[^\\s/?#@]+(?::[0-9]{1,5})?(?:/[^?\\s]*)?"
          + "(?:\\?[^\\s#]*)?";

  /** Release QA script filename. */
  private static final String QA_CHECKS_FILE_NAME = "qa_checks.csh";

  /** Fixed release QA command relative to the validated bin directory. */
  private static final String QA_CHECKS_COMMAND = "./qa_checks.csh";

  /** Windows absolute path shape. */
  private static final String WINDOWS_ABSOLUTE_PATH_PATTERN =
      "(?i)^[a-z]:[\\\\/].*";

  /** Allowed release QA targets. */
  private static final List<String> QA_CHECK_TARGETS =
      Collections.unmodifiableList(List.of("MRAUI", "AMBIG", "MRHIST",
          "MRMAP", "MRCONSO", "MRCUI", "MRHIER", "MRDEF", "MRFILESCOLS",
          "MRRANK", "MRREL", "MRSAB", "MRSAT", "MRSTY", "MRDOC", "MRX"));

  /** The Constant PUNCTUATION. */
  public static final String PUNCTUATION =
      " \t-({[)}]_!@#%&*\\:;\"',.?/~+=|<>$`^";

  /** The Constant PUNCTUATION_REGEX. */
  public static final String PUNCTUATION_REGEX =
      "[ \\t\\-\\(\\{\\[\\)\\}\\]_!@#%&\\*\\\\:;\\\"',\\.\\?\\/~\\+=\\|<>$`^]";

  /** The transformer for DOM -> XML. */
  private static Transformer transformer;

  /** The date format. */
  public static final FastDateFormat format =
      FastDateFormat.getInstance("yyyyMMdd");

  /**
   * Returns the application display timezone id.
   *
   * @return the display timezone id
   */
  public static String getDisplayTimeZoneId() {
    String timeZoneId = System.getProperty(DISPLAY_TIME_ZONE_PROPERTY);
    if (isEmpty(timeZoneId)) {
      timeZoneId = System.getenv("APP_DISPLAY_TIMEZONE");
    }
    if (isEmpty(timeZoneId)) {
      timeZoneId = PropertyUtility.getProperty(DISPLAY_TIME_ZONE_PROPERTY);
    }
    return isEmpty(timeZoneId) ? DEFAULT_DISPLAY_TIME_ZONE : timeZoneId;
  }

  /**
   * Returns the application display timezone.
   *
   * @return the display timezone
   */
  public static TimeZone getDisplayTimeZone() {
    final String timeZoneId = getDisplayTimeZoneId();
    try {
      return TimeZone.getTimeZone(ZoneId.of(timeZoneId));
    } catch (DateTimeException e) {
      Logger.getLogger(ConfigUtility.class).warn(
          "Invalid app display timezone " + timeZoneId + ", using "
              + DEFAULT_DISPLAY_TIME_ZONE,
          e);
      return TimeZone.getTimeZone(DEFAULT_DISPLAY_TIME_ZONE);
    }
  }

  /**
   * Formats a timestamp for user-facing application display.
   *
   * @param date the date
   * @return the formatted timestamp
   */
  public static String formatDisplayTimestamp(final Date date) {
    if (date == null) {
      return "";
    }
    return FastDateFormat
        .getInstance(DISPLAY_TIMESTAMP_PATTERN, getDisplayTimeZone(), Locale.US)
        .format(date);
  }

  static {
    try {
      TransformerFactory factory = TransformerFactory.newInstance();
      transformer = factory.newTransformer();
      // Indent output.
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
          "4");
      // Skip XML declaration header.
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    } catch (TransformerConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns a validated REST client base URL without a trailing slash.
   *
   * @param properties the properties
   * @return the REST client base URL
   */
  public static String getRestBaseUrl(final Properties properties) {
    final String baseUrl = requiredNetworkProperty(properties, "base.url");
    if (!baseUrl.matches(REST_BASE_URL_PATTERN)) {
      throw new IllegalArgumentException(
          "Property base.url must be an HTTP(S) URL without user info, query, "
              + "fragment, or whitespace.");
    }

    final URI uri = URI.create(baseUrl);
    validateAllowedHost(uri, properties, REST_CLIENT_ALLOWED_HOSTS_PROPERTY,
        DEFAULT_LOOPBACK_HOSTS);
    return trimTrailingSlash(baseUrl);
  }

  /**
   * Returns a validated REST URL by appending a relative path to base.url.
   *
   * @param properties the properties
   * @param path the relative REST path
   * @return the REST URL
   */
  public static String getRestUrl(final Properties properties,
    final String path) {
    return getRestBaseUrl(properties) + relativeRestPath(path);
  }

  /**
   * Opens a connection to a validated REST URL.
   *
   * @param properties the properties
   * @param path the relative REST path
   * @return the HTTP connection
   * @throws IOException if the connection cannot be opened
   */
  public static HttpURLConnection openRestConnection(
    final Properties properties, final String path) throws IOException {
    final URL url = URI.create(getRestUrl(properties, path)).toURL();
    return (HttpURLConnection) url.openConnection();
  }

  /**
   * Returns a validated JDBC URL from a named property.
   *
   * @param properties the properties
   * @param propertyName the JDBC URL property name
   * @return the JDBC URL
   */
  public static String getJdbcUrl(final Properties properties,
    final String propertyName) {
    return validateJdbcUrl(requiredNetworkProperty(properties, propertyName),
        propertyName, properties, true);
  }

  /**
   * Validates a JDBC URL that must include a schema path.
   *
   * @param jdbcUrl the JDBC URL
   * @param propertyName the JDBC URL property name
   * @param properties the properties
   * @return the validated JDBC URL
   */
  public static String validateJdbcUrl(final String jdbcUrl,
    final String propertyName, final Properties properties) {
    return validateJdbcUrl(jdbcUrl, propertyName, properties, true);
  }

  /**
   * Validates a JDBC URL that may omit the schema path.
   *
   * @param jdbcUrl the JDBC URL
   * @param propertyName the JDBC URL property name
   * @param properties the properties
   * @return the validated JDBC URL
   */
  public static String validateJdbcServerUrl(final String jdbcUrl,
    final String propertyName, final Properties properties) {
    return validateJdbcUrl(jdbcUrl, propertyName, properties, false);
  }

  /**
   * Runs the checked-in release QA script with validated paths and target.
   *
   * @param sourceDataDir the configured source data directory
   * @param binDir the configured bin directory
   * @param metaDir the release META directory
   * @param target the fixed QA target
   * @param previousMetaDir the previous release META directory
   * @param s the optional output writer
   * @return the process output
   * @throws Exception the exception
   */
  public static String runQaChecks(final File sourceDataDir, final File binDir,
    final File metaDir, final String target, final File previousMetaDir,
    final PrintWriter s) throws Exception {

    final String safeTarget = validateQaCheckTarget(target);
    final File safeSourceDataDir =
        validateExistingDirectory(sourceDataDir, "source.data.dir");
    final File safeBinDir = validateExistingDirectory(binDir, "bin directory");
    final File safeMetaDir = validateChildDirectory(safeSourceDataDir, metaDir,
        "release META directory");
    final File safePreviousMetaDir = validateChildDirectory(safeSourceDataDir,
        previousMetaDir, "previous release META directory");
    validateQaChecksScript(safeBinDir);

    if (System.getProperty("os.name").toLowerCase(Locale.ROOT)
        .contains("win")) {
      throw new UnsupportedOperationException(
          QA_CHECKS_FILE_NAME + " execution is supported on Unix-like hosts.");
    }

    final ProcessBuilder processBuilder = new ProcessBuilder(QA_CHECKS_COMMAND,
        safeMetaDir.getPath(), safeTarget, safePreviousMetaDir.getPath());
    processBuilder.directory(safeBinDir);
    processBuilder.environment().clear();
    processBuilder.redirectErrorStream(true);
    return runProcess(processBuilder, s);
  }

  /**
   * Returns the canonical configured source data directory.
   *
   * @return the source data directory
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static File getSourceDataDirectory() throws IOException {

    final String sourceDataDir =
        PropertyUtility.getProperties().getProperty(SOURCE_DATA_DIR_PROPERTY);
    return validateExistingDirectoryPath(sourceDataDir, SOURCE_DATA_DIR_PROPERTY);
  }

  /**
   * Resolves relative path parts below the configured source data directory.
   *
   * @param label the label for error messages
   * @param pathParts the relative path parts
   * @return the canonical resolved path
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static File resolveSourceDataPath(final String label,
    final String... pathParts) throws IOException {

    return resolvePathUnderDirectory(getSourceDataDirectory(), label,
        pathParts);
  }

  /**
   * Resolves an existing directory below the configured source data directory.
   *
   * @param label the label for error messages
   * @param pathParts the relative path parts
   * @return the canonical resolved directory
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static File resolveExistingSourceDataDirectory(final String label,
    final String... pathParts) throws IOException {

    return validateExistingDirectory(resolveSourceDataPath(label, pathParts),
        label);
  }

  /**
   * Resolves the upload directory for a source data record.
   *
   * @param sourceDataId the source data id
   * @return the canonical source data record directory
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static File resolveSourceDataIdDirectory(final Long sourceDataId)
    throws IOException {

    if (sourceDataId == null) {
      throw new IllegalArgumentException("Source data id must not be null.");
    }
    return resolveSourceDataPath("source data upload directory",
        sourceDataId.toString());
  }

  /**
   * Resolves the generated report directory for a project.
   *
   * @param projectId the project id
   * @return the canonical generated report directory
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static File resolveProjectReportsDirectory(final Long projectId)
    throws IOException {

    if (projectId == null) {
      throw new IllegalArgumentException("Project id must not be null.");
    }
    return resolveSourceDataPath("project reports directory",
        projectId.toString(), "reports");
  }

  /**
   * Resolves the META directory for a process release.
   *
   * @param inputPath the process input path below source.data.dir
   * @param version the release version
   * @return the canonical release META directory
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static File resolveProcessReleaseMetaDirectory(
    final String inputPath, final String version) throws IOException {

    return resolveSourceDataPath("process release META directory", inputPath,
        version, "META");
  }

  /**
   * Returns a canonical existing file from a configured path property.
   *
   * @param properties the properties
   * @param propertyName the property name
   * @return the canonical configured file
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static File validateConfiguredExistingFile(final Properties properties,
    final String propertyName) throws IOException {

    return validateExistingFilePath(
        requiredConfiguredPath(properties, propertyName),
        propertyName);
  }

  /**
   * Returns a canonical directory from a configured path property, creating it
   * when requested.
   *
   * @param properties the properties
   * @param propertyName the property name
   * @param create true if the directory should be created when missing
   * @return the canonical configured directory
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static File validateConfiguredDirectory(final Properties properties,
    final String propertyName, final boolean create) throws IOException {

    final String path = requiredConfiguredPath(properties, propertyName);
    return create ? validateOrCreateDirectoryPath(path, propertyName)
        : validateExistingDirectoryPath(path, propertyName);
  }

  /**
   * Returns a canonical existing file from an operator/configured path.
   *
   * @param path the path
   * @param label the label for error messages
   * @return the canonical existing file
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static File validateExistingFilePath(final String path,
    final String label) throws IOException {

    // Absolute configured paths are accepted only at this validation boundary.
    // codeql[java/path-injection]
    return validateExistingFile(new File(requiredPathValue(path, label)), label);
  }

  /**
   * Returns a canonical existing directory from an operator/configured path.
   *
   * @param path the path
   * @param label the label for error messages
   * @return the canonical existing directory
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static File validateExistingDirectoryPath(final String path,
    final String label) throws IOException {

    // Absolute configured paths are accepted only at this validation boundary.
    // codeql[java/path-injection]
    return validateExistingDirectory(new File(requiredPathValue(path, label)),
        label);
  }

  /**
   * Creates a directory if needed and returns its canonical path.
   *
   * @param path the path
   * @param label the label for error messages
   * @return the canonical directory
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static File validateOrCreateDirectoryPath(final String path,
    final String label) throws IOException {

    // Absolute configured paths are accepted only at this validation boundary.
    // codeql[java/path-injection]
    return validateOrCreateDirectory(new File(requiredPathValue(path, label)),
        label);
  }

  /**
   * Resolves relative path parts below an existing base directory.
   *
   * @param baseDirectory the base directory
   * @param label the label for error messages
   * @param pathParts the relative path parts
   * @return the canonical resolved path
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static File resolvePathUnderDirectory(final File baseDirectory,
    final String label, final String... pathParts) throws IOException {

    final File base = validateExistingDirectory(baseDirectory,
        safeLabel(label) + " base directory");
    Path resolvedPath = base.toPath();
    if (pathParts != null) {
      for (final String pathPart : pathParts) {
        resolvedPath = resolvedPath.resolve(
            validateRelativePath(pathPart, label)).normalize();
      }
    }

    final File resolved = resolvedPath.toFile().getCanonicalFile();
    validateChildPath(base, resolved, label);
    return resolved;
  }

  /**
   * Validates that a candidate path is below an existing base directory.
   *
   * @param baseDirectory the base directory
   * @param path the candidate path
   * @param label the label for error messages
   * @return the canonical candidate path
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static File validatePathUnderDirectory(final File baseDirectory,
    final String path, final String label) throws IOException {

    // Candidate paths are canonicalized and checked against the base below.
    // codeql[java/path-injection]
    return validatePathUnderDirectory(baseDirectory,
        new File(requiredPathValue(path, label)), label);
  }

  /**
   * Validates that a candidate path is below an existing base directory.
   *
   * @param baseDirectory the base directory
   * @param path the candidate path
   * @param label the label for error messages
   * @return the canonical candidate path
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static File validatePathUnderDirectory(final File baseDirectory,
    final File path, final String label) throws IOException {

    final File base = validateExistingDirectory(baseDirectory,
        safeLabel(label) + " base directory");
    if (path == null) {
      throw new IllegalArgumentException(safeLabel(label) + " must not be null.");
    }
    final File candidate = path.getCanonicalFile();
    validateChildPath(base, candidate, label);
    return candidate;
  }

  /**
   * Resolves a safe single-component file name below an existing directory.
   *
   * @param directory the directory
   * @param fileName the file name
   * @param label the label for error messages
   * @return the canonical resolved file
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static File resolveFileUnderDirectory(final File directory,
    final String fileName, final String label) throws IOException {

    return resolvePathUnderDirectory(directory, label,
        validateSafeFileName(fileName, label));
  }

  /**
   * Creates a directory if needed and returns its canonical path.
   *
   * @param directory the directory
   * @param label the label for error messages
   * @return the canonical directory
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static File validateOrCreateDirectory(final File directory,
    final String label) throws IOException {

    if (directory == null) {
      throw new IllegalArgumentException(safeLabel(label) + " must not be null.");
    }
    final File canonicalDirectory = directory.getCanonicalFile();
    // Directory creation is constrained by callers through canonical path helpers.
    // codeql[java/path-injection]
    ensureDirectoryExists(canonicalDirectory);
    return validateExistingDirectory(canonicalDirectory, label);
  }

  /**
   * Opens a validated UTF-8 file reader.
   *
   * @param file the file
   * @param label the label for error messages
   * @return the buffered reader
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static BufferedReader newBufferedReader(final File file,
    final String label) throws IOException {

    final File safeFile = validateExistingFile(file, label);
    // Files are opened only after canonical file validation.
    // codeql[java/path-injection]
    return Files.newBufferedReader(safeFile.toPath(), StandardCharsets.UTF_8);
  }

  /**
   * Reads all lines from a validated UTF-8 file.
   *
   * @param file the file
   * @param label the label for error messages
   * @return the file lines
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static List<String> readLines(final File file, final String label)
    throws IOException {

    final File safeFile = validateExistingFile(file, label);
    // Files are read only after canonical file validation.
    // codeql[java/path-injection]
    return Files.readAllLines(safeFile.toPath(), StandardCharsets.UTF_8);
  }

  /**
   * Opens a validated file input stream.
   *
   * @param file the file
   * @param label the label for error messages
   * @return the input stream
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static InputStream newInputStream(final File file, final String label)
    throws IOException {

    final File safeFile = validateExistingFile(file, label);
    // Files are opened only after canonical file validation.
    // codeql[java/path-injection]
    return Files.newInputStream(safeFile.toPath());
  }

  /**
   * Opens a validated UTF-8 file writer.
   *
   * @param file the file
   * @param label the label for error messages
   * @return the buffered writer
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static BufferedWriter newBufferedWriter(final File file,
    final String label) throws IOException {

    if (file == null) {
      throw new IllegalArgumentException(safeLabel(label) + " must not be null.");
    }
    final File safeFile = file.getCanonicalFile();
    final File parent = safeFile.getParentFile();
    if (parent != null) {
      validateOrCreateDirectory(parent, safeLabel(label) + " parent directory");
    }
    // Files are opened only after canonical parent directory validation.
    // codeql[java/path-injection]
    return Files.newBufferedWriter(safeFile.toPath(), StandardCharsets.UTF_8);
  }

  /**
   * Opens a validated UTF-8 print writer.
   *
   * @param file the file
   * @param label the label for error messages
   * @return the print writer
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static PrintWriter newPrintWriter(final File file, final String label)
    throws IOException {

    return new PrintWriter(newBufferedWriter(file, label));
  }

  /**
   * Lists filenames in a validated directory.
   *
   * @param directory the directory
   * @param label the label for error messages
   * @return the filenames
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static String[] list(final File directory, final String label)
    throws IOException {

    final File safeDirectory = validateExistingDirectory(directory, label);
    // Directory listing is allowed only after canonical directory validation.
    // codeql[java/path-injection]
    return safeDirectory.list();
  }

  /**
   * Lists files in a validated directory.
   *
   * @param directory the directory
   * @param label the label for error messages
   * @return the files
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static File[] listFiles(final File directory, final String label)
    throws IOException {

    final File safeDirectory = validateExistingDirectory(directory, label);
    // Directory listing is allowed only after canonical directory validation.
    // codeql[java/path-injection]
    return safeDirectory.listFiles();
  }

  /**
   * Validates a safe single-component filename.
   *
   * @param fileName the file name
   * @param label the label for error messages
   * @return the validated filename
   */
  public static String validateSafeFileName(final String fileName,
    final String label) {

    final Path path = validateRelativePath(fileName, label);
    if (path.getNameCount() != 1) {
      throw new IllegalArgumentException(
          safeLabel(label) + " must be a filename, not a path: " + fileName);
    }
    return path.toString();
  }

  /**
   * Validates a zip entry name and optionally removes the archive root folder.
   *
   * @param entryName the zip entry name
   * @param label the label for error messages
   * @param stripFirstPathSegment true if the first archive path segment is a
   *          wrapper folder to remove
   * @return the validated relative entry path, or blank for a stripped root
   *         directory entry
   */
  public static String validateZipEntryPath(final String entryName,
    final String label, final boolean stripFirstPathSegment) {

    String normalizedEntryName = requiredPathValue(entryName, label)
        .replace('\\', '/');
    if (normalizedEntryName.startsWith("/")
        || normalizedEntryName.matches(WINDOWS_ABSOLUTE_PATH_PATTERN)) {
      throw new IllegalArgumentException(
          safeLabel(label) + " must be relative: " + entryName);
    }
    if (stripFirstPathSegment) {
      final int index = normalizedEntryName.indexOf('/');
      if (index >= 0) {
        normalizedEntryName = normalizedEntryName.substring(index + 1);
      }
    }
    normalizedEntryName = trimTrailingSlashes(normalizedEntryName);
    if (normalizedEntryName.isEmpty()) {
      return "";
    }
    return validateRelativePath(normalizedEntryName, label).toString();
  }

  /**
   * Validates a MySQL JDBC URL against protocol and host allowlist rules.
   *
   * @param jdbcUrl the JDBC URL
   * @param propertyName the JDBC URL property name
   * @param properties the properties
   * @param requireSchema true if a schema path is required
   * @return the validated JDBC URL
   */
  private static String validateJdbcUrl(final String jdbcUrl,
    final String propertyName, final Properties properties,
    final boolean requireSchema) {

    final String value = requiredNetworkValue(jdbcUrl, propertyName);
    if (!value.matches(MYSQL_JDBC_URL_PATTERN)) {
      throw new IllegalArgumentException(
          "Property " + propertyName + " must be a MySQL JDBC URL without "
              + "user info, fragment, or whitespace.");
    }

    final URI uri = URI.create(value.substring("jdbc:".length()));
    if (!"mysql".equalsIgnoreCase(uri.getScheme())) {
      throw new IllegalArgumentException(
          "Property " + propertyName + " must use the mysql JDBC scheme.");
    }
    if (!isEmpty(uri.getUserInfo())) {
      throw new IllegalArgumentException(
          "Property " + propertyName + " must not include user info.");
    }
    if (requireSchema && (uri.getPath() == null || uri.getPath().length() <= 1)) {
      throw new IllegalArgumentException(
          "Property " + propertyName + " must include a schema path.");
    }

    validateAllowedHost(uri, properties, DATABASE_ALLOWED_HOSTS_PROPERTY,
        DEFAULT_LOOPBACK_HOSTS);
    return value;
  }

  /**
   * Validates that the URI host is explicitly allowed.
   *
   * @param uri the URI
   * @param properties the properties
   * @param propertyName the allowlist property name
   * @param defaultHosts the default allowlist
   */
  private static void validateAllowedHost(final URI uri,
    final Properties properties, final String propertyName,
    final String defaultHosts) {

    final String host = uri.getHost();
    if (isEmpty(host)) {
      throw new IllegalArgumentException(
          "Network URL must include a host.");
    }

    final String normalizedHost = normalizeHost(host);
    final String allowedHosts =
        configuredNetworkProperty(properties, propertyName, defaultHosts);
    for (final String allowedHost : allowedHosts.split(",")) {
      if (normalizeHost(allowedHost).equals(normalizedHost)) {
        return;
      }
    }
    throw new IllegalArgumentException(
        "Network host " + host + " is not listed in " + propertyName + ".");
  }

  /**
   * Returns a configured network property, trimming whitespace.
   *
   * @param properties the properties
   * @param propertyName the property name
   * @return the configured value
   */
  private static String requiredNetworkProperty(final Properties properties,
    final String propertyName) {
    return requiredNetworkValue(
        properties == null ? null : properties.getProperty(propertyName),
        propertyName);
  }

  /**
   * Returns a configured network property or default, trimming whitespace.
   *
   * @param properties the properties
   * @param propertyName the property name
   * @param defaultValue the default value
   * @return the configured value
   */
  private static String configuredNetworkProperty(final Properties properties,
    final String propertyName, final String defaultValue) {
    final String value =
        properties == null ? null : properties.getProperty(propertyName);
    return isEmpty(value) ? defaultValue : value.trim();
  }

  /**
   * Returns a required network value, trimming whitespace.
   *
   * @param value the value
   * @param propertyName the property name
   * @return the value
   */
  private static String requiredNetworkValue(final String value,
    final String propertyName) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(
          "Property " + propertyName + " must not be blank.");
    }
    return value.trim();
  }

  /**
   * Validates a release QA target against the fixed qa_checks.csh target list.
   *
   * @param target the target
   * @return the validated target
   */
  private static String validateQaCheckTarget(final String target) {
    final String value = target == null ? "" : target.trim();
    if (!QA_CHECK_TARGETS.contains(value)) {
      throw new IllegalArgumentException(
          "Invalid " + QA_CHECKS_FILE_NAME + " target: " + target);
    }
    return value;
  }

  /**
   * Validates the release QA script below the trusted bin directory.
   *
   * @param binDir the canonical bin directory
   * @return the validated script file
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private static File validateQaChecksScript(final File binDir)
    throws IOException {
    // The script filename is fixed and the bin directory was validated upstream.
    // codeql[java/path-injection]
    final File script = new File(binDir, QA_CHECKS_FILE_NAME).getCanonicalFile();
    if (!QA_CHECKS_FILE_NAME.equals(script.getName())
        || !binDir.equals(script.getParentFile())) {
      throw new IllegalArgumentException(
          "Unexpected " + QA_CHECKS_FILE_NAME + " location: " + script);
    }
    validateExistingFile(script, QA_CHECKS_FILE_NAME);
    // The script name and parent directory were fixed and canonicalized above.
    // codeql[java/path-injection]
    if (!script.canExecute()) {
      throw new IllegalArgumentException(
          QA_CHECKS_FILE_NAME + " must be executable: " + script);
    }
    return script;
  }

  /**
   * Validates an existing directory and returns its canonical path.
   *
   * @param directory the directory
   * @param label the label for error messages
   * @return the canonical directory
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static File validateExistingDirectory(final File directory,
    final String label) throws IOException {
    if (directory == null) {
      throw new IllegalArgumentException(label + " must not be null.");
    }
    final File canonicalDirectory = directory.getCanonicalFile();
    // Directory use is guarded by canonical validation at this helper boundary.
    // codeql[java/path-injection]
    if (!canonicalDirectory.isDirectory()) {
      throw new IllegalArgumentException(
          label + " must be an existing directory: " + directory);
    }
    return canonicalDirectory;
  }

  /**
   * Indicates whether the directory exists after canonicalization.
   *
   * @param directory the directory
   * @param label the label for error messages
   * @return true if the directory exists
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static boolean isExistingDirectory(final File directory,
    final String label) throws IOException {

    if (directory == null) {
      return false;
    }
    final File canonicalDirectory = directory.getCanonicalFile();
    // Directory use is guarded by canonical validation at this helper boundary.
    // codeql[java/path-injection]
    return canonicalDirectory.isDirectory();
  }

  /**
   * Validates an existing file and returns its canonical path.
   *
   * @param file the file
   * @param label the label for error messages
   * @return the canonical file
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static File validateExistingFile(final File file, final String label)
    throws IOException {

    if (file == null) {
      throw new IllegalArgumentException(safeLabel(label) + " must not be null.");
    }
    final File canonicalFile = file.getCanonicalFile();
    // File use is guarded by canonical validation at this helper boundary.
    // codeql[java/path-injection]
    if (!canonicalFile.isFile()) {
      throw new IllegalArgumentException(
          safeLabel(label) + " must be an existing file: " + file);
    }
    return canonicalFile;
  }

  /**
   * Indicates whether the file exists after canonicalization.
   *
   * @param file the file
   * @param label the label for error messages
   * @return true if the file exists
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static boolean isExistingFile(final File file, final String label)
    throws IOException {

    if (file == null) {
      return false;
    }
    final File canonicalFile = file.getCanonicalFile();
    // File use is guarded by canonical validation at this helper boundary.
    // codeql[java/path-injection]
    return canonicalFile.isFile();
  }

  /**
   * Indicates whether an operator/configured path exists.
   *
   * @param path the path
   * @param label the label for error messages
   * @return true if the path exists
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static boolean pathExists(final String path, final String label)
    throws IOException {

    // Absolute configured paths are accepted only at this validation boundary.
    // codeql[java/path-injection]
    final File file = new File(requiredPathValue(path, label)).getCanonicalFile();
    // Path use is guarded by canonical validation at this helper boundary.
    // codeql[java/path-injection]
    return file.exists();
  }

  /**
   * Validates that a directory exists below an allowed parent directory.
   *
   * @param parent the canonical parent directory
   * @param directory the candidate directory
   * @param label the label for error messages
   * @return the canonical child directory
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private static File validateChildDirectory(final File parent,
    final File directory, final String label) throws IOException {
    final File child = validateExistingDirectory(directory, label);
    validateChildPath(parent, child, label);
    return child;
  }

  /**
   * Validates that a path is below an allowed parent directory.
   *
   * @param parent the canonical parent directory
   * @param child the canonical child path
   * @param label the label for error messages
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private static void validateChildPath(final File parent, final File child,
    final String label) throws IOException {

    final Path parentPath = parent.getCanonicalFile().toPath();
    final Path childPath = child.getCanonicalFile().toPath();
    if (!childPath.startsWith(parentPath)) {
      throw new IllegalArgumentException(
          safeLabel(label) + " must be under " + parentPath + ": " + child);
    }
  }

  /**
   * Validates a relative path part.
   *
   * @param path the path
   * @param label the label for error messages
   * @return the validated normalized path
   */
  private static Path validateRelativePath(final String path,
    final String label) {

    final String value = requiredPathValue(path, label);
    if (value.contains("\\") || value.startsWith("/")
        || value.matches(WINDOWS_ABSOLUTE_PATH_PATTERN)) {
      throw new IllegalArgumentException(
          safeLabel(label) + " must be relative: " + path);
    }

    final Path relativePath;
    try {
      // Relative inputs reject absolute/traversal syntax before Path creation.
      // codeql[java/path-injection]
      relativePath = Paths.get(value);
    } catch (InvalidPathException e) {
      throw new IllegalArgumentException(
          safeLabel(label) + " is not a valid path: " + path, e);
    }

    if (relativePath.isAbsolute()) {
      throw new IllegalArgumentException(
          safeLabel(label) + " must be relative: " + path);
    }
    for (final Path segment : relativePath) {
      final String segmentName = segment.toString();
      if (".".equals(segmentName) || "..".equals(segmentName)) {
        throw new IllegalArgumentException(
            safeLabel(label) + " must not contain traversal segments: " + path);
      }
    }

    final Path normalizedPath = relativePath.normalize();
    if (normalizedPath.toString().isEmpty()) {
      throw new IllegalArgumentException(
          safeLabel(label) + " must not be blank.");
    }
    return normalizedPath;
  }

  /**
   * Returns a required path value after trimming whitespace.
   *
   * @param path the path
   * @param label the label for error messages
   * @return the path value
   */
  private static String requiredPathValue(final String path,
    final String label) {

    if (path == null || path.trim().isEmpty()) {
      throw new IllegalArgumentException(
          safeLabel(label) + " must not be blank.");
    }
    final String value = path.trim();
    if (value.indexOf('\0') != -1) {
      throw new IllegalArgumentException(
          safeLabel(label) + " must not contain NUL characters.");
    }
    return value;
  }

  /**
   * Returns a required configured path property.
   *
   * @param properties the properties
   * @param propertyName the property name
   * @return the configured path
   */
  private static String requiredConfiguredPath(final Properties properties,
    final String propertyName) {

    return requiredPathValue(
        properties == null ? null : properties.getProperty(propertyName),
        "Property " + propertyName);
  }

  /**
   * Removes trailing slash separators from a zip entry path.
   *
   * @param path the path
   * @return the path without trailing slashes
   */
  private static String trimTrailingSlashes(final String path) {

    String value = path;
    while (value.endsWith("/")) {
      value = value.substring(0, value.length() - 1);
    }
    return value;
  }

  /**
   * Returns a label for validation errors.
   *
   * @param label the label
   * @return the safe label
   */
  private static String safeLabel(final String label) {

    return isEmpty(label) ? "Path" : label;
  }

  /**
   * Runs a validated process and captures its output.
   *
   * @param processBuilder the process builder
   * @param s the optional output writer
   * @return the process output
   * @throws Exception the exception
   */
  private static String runProcess(final ProcessBuilder processBuilder,
    final PrintWriter s) throws Exception {

    Logger.getLogger(ConfigUtility.class)
        .info("execute = " + String.join(" ", processBuilder.command()));
    Logger.getLogger(ConfigUtility.class)
        .info("  working dir = " + processBuilder.directory());

    final Process proc = processBuilder.start();
    final StringBuilder output = new StringBuilder(1000);
    String line;
    try (BufferedReader in = new BufferedReader(
        new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
      while ((line = in.readLine()) != null) {
        if (s != null) {
          s.println(line);
          s.flush();
        }
        output.append(line).append("\n");
      }
    }

    proc.waitFor();
    if (proc.exitValue() != 0) {
      throw new Exception("Command failed = " + proc.exitValue() + ", "
          + String.join(" ", processBuilder.command()));
    }
    return output.toString();
  }

  /**
   * Returns a normalized host name.
   *
   * @param host the host
   * @return the normalized host
   */
  private static String normalizeHost(final String host) {
    String value = host == null ? "" : host.trim().toLowerCase(Locale.ROOT);
    if (value.startsWith("[") && value.endsWith("]")) {
      value = value.substring(1, value.length() - 1);
    }
    return value;
  }

  /**
   * Returns a relative REST path suitable for appending to base.url.
   *
   * @param path the path
   * @return the normalized relative path
   */
  private static String relativeRestPath(final String path) {
    if (isEmpty(path)) {
      return "";
    }
    final String value = path.trim();
    if (value.startsWith("//")
        || value.matches("(?i)[a-z][a-z0-9+.-]*:.*")) {
      throw new IllegalArgumentException(
          "REST path must be relative: " + path);
    }
    return value.startsWith("/") ? value : "/" + value;
  }

  /**
   * Trims trailing slashes from a URL.
   *
   * @param value the URL
   * @return the URL without trailing slashes
   */
  private static String trimTrailingSlash(final String value) {
    String trimmed = value;
    while (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed;
  }

  /**
   * Indicates whether or not the server is active.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public static boolean isServerActive() throws Exception {
    final Properties properties = PropertyUtility.getProperties();

    try {
      // Attempt to logout to verify service is up (this works like a "ping").
      Client client = ClientBuilder.newClient();
      WebTarget target =
          client.target(getRestUrl(properties, "/security/logout/dummy"));

      Response response = target.request(MediaType.APPLICATION_JSON).get();
      if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Indicates whether or not analysis mode is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public static boolean isAnalysisMode() throws Exception {

    try {
      final Properties properties = PropertyUtility.getProperties();
      return "true".equals(properties.getProperty("analysis.mode"));
    } catch (Throwable e) {
      return false;
    }
  }

  /**
   * New handler instance.
   *
   * @param <T> the
   * @param handler the handler
   * @param handlerClass the handler class
   * @param type the type
   * @return the object
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  public static <T> T newHandlerInstance(String handler, String handlerClass,
    Class<T> type) throws Exception {
    if (handlerClass == null) {
      throw new Exception("Handler class " + handlerClass + " is not defined");
    }
    Class<?> toInstantiate = Class.forName(handlerClass);
    if (toInstantiate == null) {
      throw new Exception("Unable to find class " + handlerClass);
    }
    Object o = null;
    try {
      o = toInstantiate.newInstance();
    } catch (Exception e) {
      e.printStackTrace();
      // do nothing
    }
    if (o == null) {
      throw new Exception("Unable to instantiate class " + handlerClass
          + ", check for default constructor.");
    }
    if (type.isAssignableFrom(o.getClass())) {
      return (T) o;
    }
    throw new Exception("Handler is not assignable from " + type.getName());
  }

  /**
   * Instantiates a handler using standard setup and configures it with
   * properties.
   *
   * @param <T> the
   * @param property the property
   * @param handlerName the handler name
   * @param type the type
   * @return the t
   * @throws Exception the exception
   */
  public static <T extends Configurable> T newStandardHandlerInstanceWithConfiguration(
    String property, String handlerName, Class<T> type) throws Exception {

    // Instantiate the handler
    // property = "metadata.service.handler" (e.g)
    // handlerName = "SNOMED" (e.g.)
    final Properties config = PropertyUtility.getProperties();
    String classKey = property + "." + handlerName + ".class";
    if (config.getProperty(classKey) == null) {
      throw new Exception("Unexpected null classkey " + classKey);
    }
    String handlerClass = config.getProperty(classKey);
    Logger.getLogger(ConfigUtility.class).debug("Instantiate " + handlerClass);
    T handler =
        ConfigUtility.newHandlerInstance(handlerName, handlerClass, type);

    // Look up and build properties
    final Properties handlerProperties = new Properties();
    handlerProperties.setProperty("security.handler", handlerName);

    for (final Object key : config.keySet()) {
      // Find properties like "metadata.service.handler.SNOMED.class"
      if (key.toString().startsWith(property + "." + handlerName + ".")) {
        String shortKey = key.toString()
            .substring((property + "." + handlerName + ".").length());
        if (!property.contains("password")) {
          Logger.getLogger(ConfigUtility.class).debug(" property " + shortKey
              + " = " + config.getProperty(key.toString()));
        }
        handlerProperties.put(shortKey, config.getProperty(key.toString()));
      }
    }
    handler.setProperties(handlerProperties);
    return handler;
  }

  /**
   * Returns the graph for string.
   *
   * @param <T> the generic type
   * @param xml the xml
   * @param graphClass the graph class
   * @return the graph for string
   * @throws JAXBException the JAXB exception
   */
  @SuppressWarnings("unchecked")
  public static <T> T getGraphForString(String xml, Class<T> graphClass)
    throws JAXBException {
    if (ConfigUtility.isEmpty(xml)) {
      return null;
    }
    JAXBContext context = JAXBContext.newInstance(graphClass);
    Unmarshaller unmarshaller = context.createUnmarshaller();
    return (T) unmarshaller.unmarshal(new StreamSource(new StringReader(xml)));
  }

  /**
   * Returns the graph for json.
   *
   * @param <T> the generic type
   * @param json the json
   * @param graphClass the graph class
   * @return the graph for json
   * @throws Exception the exception
   */
  public static <T> T getGraphForJson(String json, Class<T> graphClass)
    throws Exception {
    if (ConfigUtility.isEmpty(json)) {
      return null;
    }
    InputStream in =
        new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    ObjectMapper mapper = new ObjectMapper();
    AnnotationIntrospector introspector =
        new JakartaXmlBindAnnotationIntrospector(mapper.getTypeFactory());
    mapper.setAnnotationIntrospector(introspector);
    return mapper.readValue(in, graphClass);

  }

  /**
   * Returns the graph for json. sample usage:
   * 
   * <pre>
   *   List&lt;ConceptJpa&gt; list = ConfigUtility.getGraphForJson(str, new TypeReference&lt;List&lt;ConceptJpa&gt;&gt;{});
   * </pre>
   * 
   * @param <T> the
   * @param json the json
   * @param typeRef the type ref
   * @return the graph for json
   * @throws Exception the exception
   */
  public static <T> T getGraphForJson(String json, TypeReference<T> typeRef)
    throws Exception {
    if (ConfigUtility.isEmpty(json)) {
      return null;
    }
    InputStream in =
        new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    ObjectMapper mapper = new ObjectMapper();
    AnnotationIntrospector introspector =
        new JakartaXmlBindAnnotationIntrospector(mapper.getTypeFactory());
    mapper.setAnnotationIntrospector(introspector);
    return mapper.readValue(in, typeRef);

  }

  /**
   * Returns the graph for file.
   *
   * @param <T> the generic type
   * @param file the file
   * @param graphClass the graph class
   * @return the graph for file
   * @throws FileNotFoundException the file not found exception
   * @throws JAXBException the JAXB exception
   */
  @SuppressWarnings("resource")
  public static <T> T getGraphForFile(File file, Class<T> graphClass)
    throws FileNotFoundException, JAXBException {
    return getGraphForString(
        new Scanner(file, "UTF-8").useDelimiter("\\A").next(), graphClass);
  }

  /**
   * Returns the graph for stream.
   *
   * @param <T> the generic type
   * @param in the in
   * @param graphClass the graph class
   * @return the graph for stream
   * @throws FileNotFoundException the file not found exception
   * @throws JAXBException the JAXB exception
   */
  @SuppressWarnings("resource")
  public static <T> T getGraphForStream(InputStream in, Class<T> graphClass)
    throws FileNotFoundException, JAXBException {
    return getGraphForString(
        new Scanner(in, "UTF-8").useDelimiter("\\A").next(), graphClass);
  }

  /**
   * Returns the XML string for for graph object.
   *
   * @param object the object
   * @return the string for for graph
   * @throws JAXBException the JAXB exception
   */
  public static String getStringForGraph(Object object) throws JAXBException {
    StringWriter writer = new StringWriter();
    JAXBContext jaxbContext = null;
    jaxbContext = JAXBContext.newInstance(object.getClass());
    Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
    jaxbMarshaller.marshal(object, writer);
    return writer.toString();
  }

  /**
   * Returns the json for graph.
   *
   * @param object the object
   * @return the json for graph
   * @throws Exception the exception
   */
  public static String getJsonForGraph(Object object) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    AnnotationIntrospector introspector =
        new JakartaXmlBindAnnotationIntrospector(mapper.getTypeFactory());
    mapper.setAnnotationIntrospector(introspector);
    return mapper.writeValueAsString(object);
  }

  /**
   * Returns the node for string.
   *
   * @param xml the xml
   * @return the node for string
   * @throws ParserConfigurationException the parser configuration exception
   * @throws SAXException the SAX exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static Node getNodeForString(String xml)
    throws ParserConfigurationException, SAXException, IOException {

    InputStream in =
        new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    // Parse XML file.
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document document = db.parse(in);
    Node rootNode = document.getFirstChild();
    return rootNode;
  }

  /**
   * Returns the node for file.
   *
   * @param file the file
   * @return the node for file
   * @throws ParserConfigurationException the parser configuration exception
   * @throws SAXException the SAX exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static Node getNodeForFile(File file)
    throws ParserConfigurationException, SAXException, IOException {
    try (InputStream in = newInputStream(file, "XML file")) {
      // Parse XML file.
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document document = db.parse(in);
      Node rootNode = document.getFirstChild();
      return rootNode;
    }
  }

  /**
   * Returns the string for node.
   *
   * @param root the root node
   * @return the string for node
   * @throws TransformerException the transformer exception
   * @throws ParserConfigurationException the parser configuration exception
   */
  public static String getStringForNode(Node root)
    throws TransformerException, ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document document = builder.newDocument();
    document.appendChild(document.importNode(root, true));
    DOMSource source = new DOMSource(document);
    StringWriter out = new StringWriter();
    StreamResult result = new StreamResult(out);
    transformer.transform(source, result);
    return out.toString();
  }

  /**
   * Returns the graph for node.
   *
   * @param node the node
   * @param graphClass the graph class
   * @return the graph for node
   * @throws JAXBException the JAXB exception
   * @throws TransformerException the transformer exception
   * @throws ParserConfigurationException the parser configuration exception
   */
  public static Object getGraphForNode(Node node, Class<?> graphClass)
    throws JAXBException, TransformerException, ParserConfigurationException {
    return getGraphForString(getStringForNode(node), graphClass);
  }

  /**
   * Returns the node for graph.
   *
   * @param object the object
   * @return the node for graph
   * @throws ParserConfigurationException the parser configuration exception
   * @throws SAXException the SAX exception
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws JAXBException the JAXB exception
   */
  public static Node getNodeForGraph(Object object)
    throws ParserConfigurationException, SAXException, IOException,
    JAXBException {
    return getNodeForString(getStringForGraph(object));
  }

  /**
   * Pretty format.
   *
   * @param input the input
   * @param indent the indent
   * @return the string
   */
  public static String prettyFormat(String input, int indent) {
    try {
      Source xmlInput = new StreamSource(new StringReader(input));
      StringWriter stringWriter = new StringWriter();
      StreamResult xmlOutput = new StreamResult(stringWriter);
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      transformerFactory.setAttribute("indent-number", indent);
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.transform(xmlInput, xmlOutput);
      return xmlOutput.getWriter().toString();
    } catch (Exception e) {
      // simple exception handling, please review it
      throw new RuntimeException(e);
    }
  }

  /**
   * Merge-sort two files.
   * 
   * @param files1 the first set of files
   * @param files2 the second set of files
   * @param comp the comparator
   * @param dir the sort dir
   * @param headerLine the header_line
   * @return the sorted {@link File}
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static File mergeSortedFiles(File files1, File files2,
    Comparator<String> comp, File dir, String headerLine) throws IOException {

    final File outFile = File.createTempFile("t+~", ".tmp", dir);

    try (BufferedReader in1 = Files.newBufferedReader(files1.toPath(),
        StandardCharsets.UTF_8);
        BufferedReader in2 = Files.newBufferedReader(files2.toPath(),
            StandardCharsets.UTF_8);
        BufferedWriter out = Files.newBufferedWriter(outFile.toPath(),
            StandardCharsets.UTF_8)) {

      String line1 = in1.readLine();
      String line2 = in2.readLine();
      String line = null;
      if (!headerLine.isEmpty()) {
        line = headerLine;
        out.write(line);
        out.newLine();
      }
      while (line1 != null || line2 != null) {
        if (line1 == null) {
          line = line2;
          line2 = in2.readLine();
        } else if (line2 == null) {
          line = line1;
          line1 = in1.readLine();
        } else if (comp.compare(line1, line2) < 0) {
          line = line1;
          line1 = in1.readLine();
        } else {
          line = line2;
          line2 = in2.readLine();
        }
        // if a header line, do not write
        if (!line.startsWith("id")) {
          out.write(line);
          out.newLine();
        }
      }
      out.flush();
    }
    return outFile;
  }

  /**
   * Delete directory.
   *
   * @param path the path
   * @return true, if successful
   */
  public static boolean deleteDirectory(File path) {
    // Deletes are limited by callers to validated directories.
    // codeql[java/path-injection]
    if (path == null || !path.exists()) {
      return true;
    }
    boolean success = true;
    // Deletes are limited by callers to validated directories.
    // codeql[java/path-injection]
    if (path.isDirectory()) {
      final File[] files = path.listFiles();
      if (files == null) {
        return false;
      }
      for (int i = 0; i < files.length; i++) {
        if (files[i].isDirectory()) {
          success &= deleteDirectory(files[i]);
        } else {
          // Deletes are limited by callers to validated directories.
          // codeql[java/path-injection]
          success &= files[i].delete() || !files[i].exists();
        }
      }
    }
    // Deletes are limited by callers to validated directories.
    // codeql[java/path-injection]
    return (path.delete() || !path.exists()) && success;
  }

  /**
   * Ensures the directory exists.
   *
   * @param dir the directory
   * @throws IOException if the directory cannot be created
   */
  public static void ensureDirectoryExists(File dir) throws IOException {
    if (dir == null) {
      throw new IOException("Directory must not be null");
    }
    // Directory creation is limited by callers to validated paths.
    // codeql[java/path-injection]
    Files.createDirectories(dir.toPath());
    // Directory creation is limited by callers to validated paths.
    // codeql[java/path-injection]
    if (!dir.isDirectory()) {
      throw new IOException("Could not create directory " + dir);
    }
  }

  /**
   * Ensures the file exists.
   *
   * @param file the file
   * @throws IOException if the file cannot be created
   */
  public static void ensureFileExists(File file) throws IOException {
    if (file == null) {
      throw new IOException("File must not be null");
    }
    final File parentFile = file.getParentFile();
    if (parentFile != null) {
      ensureDirectoryExists(parentFile);
    }
    // File creation is limited by callers to validated paths.
    // codeql[java/path-injection]
    if (!file.exists()) {
      // File creation is limited by callers to validated paths.
      // codeql[java/path-injection]
      Files.createFile(file.toPath());
    }
    // File creation is limited by callers to validated paths.
    // codeql[java/path-injection]
    if (!file.isFile()) {
      throw new IOException("Could not create file " + file);
    }
  }

  /**
   * Deletes the file if it exists.
   *
   * @param file the file
   * @throws IOException if the file cannot be deleted
   */
  public static void deleteFileIfExists(File file) throws IOException {
    if (file != null) {
      // File deletion is limited by callers to validated paths.
      // codeql[java/path-injection]
      Files.deleteIfExists(file.toPath());
    }
  }

  /**
   * Renames a file or directory.
   *
   * @param source the source
   * @param target the target
   * @throws IOException if the rename cannot be completed
   */
  public static void renameFile(File source, File target) throws IOException {
    if (source == null || target == null) {
      throw new IOException("Source and target must not be null");
    }
    Files.move(source.toPath(), target.toPath());
  }

  /**
   * Explicitly initializes a Hibernate proxy or collection.
   *
   * @param value the value to initialize
   */
  public static void initializeLazy(Object value) {
    if (value != null) {
      Hibernate.initialize(value);
    }
  }

  /**
   * Sends email.
   *
   * @param subject the subject
   * @param from the from
   * @param recipients the recipients
   * @param body the body
   * @param details the details
   * @throws Exception the exception
   */
  public static void sendEmail(String subject, String from, String recipients,
    String body, Properties details) throws Exception {
    // avoid sending mail if disabled
    if ("false".equals(details.getProperty("mail.enabled"))) {
      // do nothing
      return;
    }
    Session session = null;
    final Properties config = PropertyUtility.getProperties();
    if ("true".equals(config.get("mail.smtp.auth"))) {
      Authenticator auth = new SMTPAuthenticator();
      session = Session.getInstance(details, auth);
    } else {
      session = Session.getInstance(details);
    }

    MimeMessage msg = new MimeMessage(session);
    if (body.contains("<html")) {
      msg.setContent(body.toString(), "text/html; charset=utf-8");
    } else {
      msg.setText(body.toString());
    }
    msg.setSubject(subject);
    msg.setFrom(new InternetAddress(from));
    final String[] recipientsArray = recipients.split(";");
    for (final String recipient : recipientsArray) {
      msg.addRecipient(Message.RecipientType.TO,
          new InternetAddress(recipient));
    }
    Transport.send(msg);
  }

  /**
   * Sends email with attachment.
   *
   * @param subject the subject
   * @param from the from
   * @param recipients the recipients
   * @param body the body
   * @param details the details
   * @param attachmentFileName the attachment file name
   * @throws Exception the exception
   */
  public static void sendEmail(String subject, String from, String recipients,
    String body, Properties details, String attachmentFileName) throws Exception {
    // avoid sending mail if disabled
    if ("false".equals(details.getProperty("mail.enabled"))) {
      // do nothing
      return;
    }
    Session session = null;
    final Properties config = PropertyUtility.getProperties();
    if ("true".equals(config.get("mail.smtp.auth"))) {
      Authenticator auth = new SMTPAuthenticator();
      session = Session.getInstance(details, auth);
    } else {
      session = Session.getInstance(details);
    }

    Multipart multipart = new MimeMultipart();

    MimeMessage msg = new MimeMessage(session);
    msg.setSubject(subject);
    msg.setFrom(new InternetAddress(from));
    final String[] recipientsArray = recipients.split(";");
    for (final String recipient : recipientsArray) {
      msg.addRecipient(Message.RecipientType.TO,
          new InternetAddress(recipient));
    }
    
    // Create the message part
    BodyPart messageBodyPart = new MimeBodyPart();
    
    if (body.contains("<html")) {
      messageBodyPart.setContent(body.toString(), "text/html; charset=utf-8");
    } else {
      messageBodyPart.setText(body.toString());
    }
    
    multipart.addBodyPart(messageBodyPart);

    // Part two is attachment
    messageBodyPart = new MimeBodyPart();
    DataSource source = new FileDataSource(attachmentFileName);
    messageBodyPart.setDataHandler(new DataHandler(source));
    messageBodyPart.setFileName(attachmentFileName);
    multipart.addBodyPart(messageBodyPart);

    // Send the complete message parts
    msg.setContent(multipart);    
    
    // Send message
    Transport.send(msg);
  }
  
  
  /**
   * SMTPAuthenticator.
   */
  public static class SMTPAuthenticator extends jakarta.mail.Authenticator {

    /**
     * Instantiates an empty {@link SMTPAuthenticator}.
     */
    public SMTPAuthenticator() {
      // do nothing
    }

    /* see superclass */
    @Override
    public PasswordAuthentication getPasswordAuthentication() {
      Properties config = null;
      try {
        config = PropertyUtility.getProperties();
      } catch (Exception e) {
        // do nothing
      }
      if (config == null) {
        return null;
      } else {
        return new PasswordAuthentication(config.getProperty("mail.smtp.user"),
            config.getProperty("mail.smtp.password"));
      }
    }
  }

  /**
   * Reflection sort.
   *
   * @param <T> the
   * @param classes the classes
   * @param clazz the clazz
   * @param sortField the sort field
   * @throws Exception the exception
   */
  public static <T> void reflectionSort(List<T> classes, Class<T> clazz,
    String sortField) throws Exception {

    final Method getMethod = clazz.getMethod("get"
        + sortField.substring(0, 1).toUpperCase() + sortField.substring(1));
    if (getMethod.getReturnType().isAssignableFrom(Comparable.class)) {
      throw new Exception("Referenced sort field is not comparable");
    }
    Collections.sort(classes, new Comparator<T>() {
      @SuppressWarnings({
          "rawtypes", "unchecked"
      })
      @Override
      public int compare(T o1, T o2) {
        try {
          Comparable f1 = (Comparable) getMethod.invoke(o1, new Object[] {});
          Comparable f2 = (Comparable) getMethod.invoke(o2, new Object[] {});
          return f1.compareTo(f2);
        } catch (Exception e) {
          // do nothing
        }
        return 0;
      }
    });
  }

  /**
   * To arabic.
   *
   * @param number the number
   * @return the int
   * @throws Exception the exception
   */
  public static int toArabic(String number) throws Exception {
    if (number.isEmpty())
      return 0;
    if (number.startsWith("M"))
      return 1000 + toArabic(number.substring(1));
    if (number.startsWith("CM"))
      return 900 + toArabic(number.substring(2));
    if (number.startsWith("D"))
      return 500 + toArabic(number.substring(1));
    if (number.startsWith("CD"))
      return 400 + toArabic(number.substring(2));
    if (number.startsWith("C"))
      return 100 + toArabic(number.substring(1));
    if (number.startsWith("XC"))
      return 90 + toArabic(number.substring(2));
    if (number.startsWith("L"))
      return 50 + toArabic(number.substring(1));
    if (number.startsWith("XL"))
      return 40 + toArabic(number.substring(2));
    if (number.startsWith("X"))
      return 10 + toArabic(number.substring(1));
    if (number.startsWith("IX"))
      return 9 + toArabic(number.substring(2));
    if (number.startsWith("V"))
      return 5 + toArabic(number.substring(1));
    if (number.startsWith("IV"))
      return 4 + toArabic(number.substring(2));
    if (number.startsWith("I"))
      return 1 + toArabic(number.substring(1));
    throw new Exception("something bad happened");
  }

  /**
   * Indicates whether or not roman numeral is the case.
   *
   * @param number the number
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public static boolean isRomanNumeral(String number) {
    return number
        .matches("^M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$");
  }

  /**
   * Returns the indent for level.
   *
   * @param level the level
   * @return the indent for level
   */
  public static String getIndentForLevel(int level) {

    final StringBuilder sb = new StringBuilder().append("  ");
    for (int i = 0; i < level; i++) {
      sb.append("  ");
    }
    return sb.toString();
  }

  /**
   * This method is intended to bypass some incorrect static code analysis from
   * the FindBugs Eclipse plugin.
   *
   * @param o the o
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public static boolean isNull(Object o) {
    return o == null;
  }

  /**
   * Capitalize.
   *
   * @param value the value
   * @return the string
   */
  public static String capitalize(String value) {
    if (value == null) {
      return value;
    }
    return value.substring(0, 1).toUpperCase() + value.substring(1);
  }

  /**
   * Converts string field to case-insensitive string of tokens with punctuation
   * removed For example, "HIV Infection" becomes "hiv infection", while
   * "1,2-hydroxy" becomes "1 2 hydroxy".
   *
   * @param value the value
   * @return the string
   */
  public static String normalize(String value) {

    final String[] splitStrs = value.toLowerCase().split(PUNCTUATION_REGEX);
    return String.join(" ", splitStrs).trim().replaceAll(" +", " ");
  }

  /**
   * Gets the base index directory.
   *
   * @return the base index directory
   * @throws Exception the exception
   */
  public static String getBaseIndexDirectory() throws Exception {
    return PropertyUtility.getProperty("hibernate.search.backend.directory.root");
  }

  /**
   * Gets the expression index directory name.
   *
   * @param terminology the terminology
   * @param version the version
   * @return the expression index directory name
   * @throws Exception the exception
   */
  public static String getExpressionIndexDirectoryName(String terminology,
    String version) throws Exception {
    return getExpressionIndexDirectory(terminology, version).getPath()
        + File.separator;
  }

  /**
   * Gets the expression index directory.
   *
   * @param terminology the terminology
   * @param version the version
   * @return the expression index directory
   * @throws Exception the exception
   */
  private static File getExpressionIndexDirectory(final String terminology,
    final String version) throws Exception {

    final File baseDir = validateOrCreateDirectoryPath(getBaseIndexDirectory(),
        "base index directory");
    return resolvePathUnderDirectory(baseDir, "expression index directory",
        "expr", validateSafeFileName(terminology, "terminology"),
        validateSafeFileName(version, "version"));
  }

  /**
   * Create expression index directory.
   *
   * @param terminology the terminology
   * @param version the version
   * @throws Exception the exception
   */
  public static void createExpressionIndexDirectory(String terminology,
    String version) throws Exception {

    // remove directory (if it exists)
    removeExpressionIndexDirectory(terminology, version);

    // create the directory structure
    File eclDir = getExpressionIndexDirectory(terminology, version);
    ensureDirectoryExists(eclDir);
  }

  /**
   * Remove expression index directory.
   *
   * @param terminology the terminology
   * @param version the version
   * @throws Exception the exception
   */
  public static void removeExpressionIndexDirectory(String terminology,
    String version) throws Exception {
    File exprDir = getExpressionIndexDirectory(terminology, version);
    if (exprDir.exists()) {
      if (!exprDir.isDirectory()) {
        throw new Exception(
            "Cannot delete expression indexes: path is not a directory: "
                + exprDir.getAbsolutePath());
      }
      deleteDirectory(exprDir);
    }
  }

  /**
   * Get the lucene max boolean clause count.
   *
   * @return the max clause count
   * @throws NumberFormatException the number format exception
   * @throws Exception the exception
   */
  public static int getLuceneMaxClauseCount()
    throws NumberFormatException, Exception {
    final String maxClauseCount =
        PropertyUtility.getProperty("org.apache.lucene.search.BooleanQuery.maxClauseCount");
    if (maxClauseCount == null) {
      return 100000;
    }
    return Integer.valueOf(maxClauseCount);
  }

  /**
   * Indicates whether or not a string is empty.
   *
   * @param str the str
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public static boolean isEmpty(String str) {
    return str == null || str.isEmpty();
  }

  /**
   * Returns the md5.
   *
   * @param str the str
   * @return the static string
   */
  public static String getMd5(String str) {
    return DigestUtils.md5Hex(str);
  }

  /**
   * Returns the upload dir.
   *
   * @return the upload dir
   * @throws Exception the exception
   */
  public static String getUploadDir() throws Exception {
    return getSourceDataDirectory().getPath();
  }

  /**
   * Compose url.
   *
   * @param clauses the clauses
   * @return the string
   * @throws Exception the exception
   */
  public static String composeUrl(Map<String, String> clauses)
    throws Exception {
    final StringBuilder sb = new StringBuilder();
    for (final String key : clauses.keySet()) {
      if (ConfigUtility.isEmpty(clauses.get(key))) {
        continue;
      }
      if (sb.length() > 1) {
        sb.append("&");
      }
      sb.append(key).append("=");
      final String value = clauses.get(key);
      if (value.matches("^[0-9a-zA-Z\\-\\.]*$")) {
        sb.append(value);
      } else {
        sb.append(URLEncoder.encode(value, "UTF-8").replaceAll("\\+", "%20"));
      }
    }
    return (sb.length() > 0 ? "?" + sb.toString() : "");
  }

  /**
   * Compose query from a list of possibly empty/null clauses and an operator
   * (typically OR or AND).
   *
   * @param operator the operator
   * @param clauses the clauses
   * @return the string
   */
  public static String composeQuery(String operator, List<String> clauses) {
    final StringBuilder sb = new StringBuilder();
    if (operator.equals("OR")) {
      sb.append("(");
    }
    for (final String clause : clauses) {
      if (ConfigUtility.isEmpty(clause)) {
        continue;
      }
      if (sb.length() > 0 && !operator.equals("OR")) {
        sb.append(" ").append(operator).append(" ");
      }
      if (sb.length() > 1 && operator.equals("OR")) {
        sb.append(" ").append(operator).append(" ");
      }
      sb.append(clause);
    }
    if (operator.equals("OR")) {
      sb.append(")");
    }
    if (operator.equals("OR") && sb.toString().equals("()")) {
      return "";
    }

    return sb.toString();
  }

  /**
   * Compose query.
   *
   * @param operator the operator
   * @param clauses the clauses
   * @return the string
   */
  public static String composeQuery(String operator, String... clauses) {
    final StringBuilder sb = new StringBuilder();
    if (operator.equals("OR")) {
      sb.append("(");
    }
    for (final String clause : clauses) {
      if (ConfigUtility.isEmpty(clause)) {
        continue;
      }
      if (sb.length() > 0 && !operator.equals("OR")) {
        sb.append(" ").append(operator).append(" ");
      }
      if (sb.length() > 1 && operator.equals("OR")) {
        sb.append(" ").append(operator).append(" ");
      }

      sb.append(clause);
    }
    if (operator.equals("OR")) {
      sb.append(")");
    }
    if (operator.equals("OR") && sb.toString().equals("()")) {
      return "";
    }

    return sb.toString();
  }

  /**
   * Compose clause.
   *
   * @param fieldName the field name
   * @param fieldValue the field value
   * @param escapeValue - whether the value can have characters that need to be
   *          escaped
   * @return the string
   * @throws Exception the exception
   */
  public static String composeClause(String fieldName, String fieldValue,
    boolean escapeValue) throws Exception {

    if (!ConfigUtility.isEmpty(fieldValue)) {
      if (escapeValue) {
        return fieldName + ":\"" + QueryParserBase.escape(fieldValue) + "\"";
      } else {
        return fieldName + ":" + fieldValue;
      }
    } else {
      return "NOT " + fieldName + ":[* TO *]";
    }
  }

  /**
   * Returns the name from class by stripping package and putting spaces where
   * CamelCase is used.
   *
   * @param clazz the clazz
   * @return the name from class
   */
  public static String getNameFromClass(Class<?> clazz) {
    return clazz.getName().substring(clazz.getName().lastIndexOf('.') + 1)
        .replaceAll(String.format("%s|%s|%s", "(?<=[A-Z])(?=[A-Z][a-z])",
            "(?<=[^A-Z])(?=[A-Z])", "(?<=[A-Za-z])(?=[^A-Za-z])"), " ");
  }

  /**
   * Returns the byte comparator.
   *
   * @return the byte comparator
   */
  public static Comparator<String> getByteComparator() {
    return new Comparator<String>() {

      /* see superclass */
      @Override
      public int compare(String o1, String o2) {
        return UnsignedBytes.lexicographicalComparator().compare(
            o1.getBytes(StandardCharsets.UTF_8),
            o2.getBytes(StandardCharsets.UTF_8));
      }

    };
  }

  /**
   * Returns the first order field hash.
   *
   * @param c the c
   * @return the first order field hash
   * @throws Exception the exception
   */
  public static String getFirstOrderFieldHash(Component c) throws Exception {
    final StringBuilder sb = new StringBuilder();
    for (final Method m : c.getClass().getMethods()) {
      if (!Collection.class.isAssignableFrom(m.getReturnType())
          && m.getParameterTypes().length == 0
          && m.getName().startsWith("get")) {
        sb.append(m.invoke(c, new Object[] {})).append(",");
      }
    }
    return "";
  }

  /** Size of the buffer to read/write data. */
  private static final int BUFFER_SIZE = 4096;

  /**
   * Extracts a zip file specified by the zipFilePath to a directory specified
   * by destDirectory (will be created if does not exists).
   *
   * @param zipFilePath the zip file path
   * @param destDirectory the dest directory
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static void unzip(String zipFilePath, String destDirectory)
    throws IOException {

    final File destDir =
        validateOrCreateDirectoryPath(destDirectory, "zip destination");
    final File zipFile = validateExistingFilePath(zipFilePath, "zip file");
    try (ZipInputStream zipIn =
        new ZipInputStream(newInputStream(zipFile, "zip file"))) {
      ZipEntry entry = zipIn.getNextEntry();
      while (entry != null) {
        final String entryPath =
            validateZipEntryPath(entry.getName(), "zip entry", false);
        if (!entryPath.isEmpty()) {
          final File output =
              resolvePathUnderDirectory(destDir, "zip entry", entryPath);
          if (entry.isDirectory()) {
            ensureDirectoryExists(output);
          } else {
            final File parent = output.getParentFile();
            if (parent != null) {
              ensureDirectoryExists(parent);
            }
            extractFile(zipIn, output);
          }
        }
        zipIn.closeEntry();
        entry = zipIn.getNextEntry();
      }
    }
  }

  /**
   * Extracts a zip entry (file entry).
   *
   * @param zipIn the zip in
   * @param filePath the file path
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private static void extractFile(ZipInputStream zipIn, File filePath)
    throws IOException {

    try (BufferedOutputStream bos =
        new BufferedOutputStream(Files.newOutputStream(filePath.toPath()))) {
      byte[] bytesIn = new byte[BUFFER_SIZE];
      int read = 0;
      while ((read = zipIn.read(bytesIn)) != -1) {
        bos.write(bytesIn, 0, read);
      }
    }
  }

}
