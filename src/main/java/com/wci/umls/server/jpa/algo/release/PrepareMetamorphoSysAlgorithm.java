/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.release;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Properties;

import org.codehaus.plexus.util.FileUtils;

import com.wci.umls.server.model.algo.AlgorithmParameter;
import com.wci.umls.server.model.algo.ValidationResult;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.PropertyUtility;
import com.wci.umls.server.jpa.model.AlgorithmParameterJpa;
import com.wci.umls.server.jpa.model.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractAlgorithm;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;

/**
 * Algorithm to prepare MetamorphoSys.
 */
public class PrepareMetamorphoSysAlgorithm extends AbstractAlgorithm {

  /** The email. */
  private String email;
  
  /**
   * Instantiates an empty {@link PrepareMetamorphoSysAlgorithm}.
   *
   * @throws Exception the exception
   */
  public PrepareMetamorphoSysAlgorithm() throws Exception {
    // n/a
  }

  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {

    getExistingProcessInputDirectory();

    return new ValidationResultJpa();
  }

  /* see superclass */
  @Override
  public void compute() throws Exception {
    logInfo("Starting " + getName());
    
    //
    //Prepare mmsys.zip with updated release version
    //
    final File inputPath = getExistingProcessInputDirectory();
    final File pathMeta =
        ConfigUtility.resolvePathUnderDirectory(inputPath,
            "MetamorphoSys META directory", "META");
    final File pathTemp =
        ConfigUtility.resolvePathUnderDirectory(pathMeta,
            "MetamorphoSys temp directory", "x");
    logInfo("  pathTemp absolute: " + pathTemp.getAbsolutePath());
    logInfo("  pathTemp canonical: " + pathTemp.getCanonicalPath());
    
    // If temp dir "path/x  exists already, remove it"
    if (pathTemp.exists()) {
      logInfo("  Remove directory = " + pathTemp);
      FileUtils.deleteDirectory(pathTemp);
    }
    
    // Make backup of mmsys.zip, if it doesn't already exist e.g. mmsys.202106.zip
    final File originalZip =
        ConfigUtility.resolveFileUnderDirectory(pathMeta, "mmsys.zip",
            "MetamorphoSys zip file");
    final File backupZip = ConfigUtility.resolveFileUnderDirectory(pathMeta,
        "mmsys." + getProcess().getVersion() + ".zip",
        "MetamorphoSys backup zip file");
    if (!backupZip.exists()) {
      Path copied = backupZip.toPath();
      Path originalPath = originalZip.toPath();
      Files.copy(originalPath, copied, StandardCopyOption.REPLACE_EXISTING);
    }
    
    // Unzip "path/META/mmsys.zip" into "path/x"
    logInfo("  Unzip " + originalZip);
    commitClearBegin();
    ConfigUtility.unzip(originalZip.getPath(), pathTemp.getPath());

    //"config" (path/META/x/config)
    final File pathConfig =
        ConfigUtility.resolvePathUnderDirectory(pathTemp,
            "MetamorphoSys config directory", "config");
    // get most recent release folder in the config directory
    final File previousReleaseFolder = getLastModified(pathConfig);
    final String previousRelease = previousReleaseFolder.getName();
    final File currentReleaseFolder = ConfigUtility.resolveFileUnderDirectory(
        pathConfig, getProcess().getVersion(), "MetamorphoSys release folder");
    
    //Rename the previous release directory to current release (e.g. % mv 201203 201209)
    ConfigUtility.renameFile(previousReleaseFolder, currentReleaseFolder);
    
    //Edit mmsys.prop to refer to new current release version (e.g. 201209)
    //Edit contents of current release directory to refer to this as the release version
    //    umls.prop
    //    release.dat
    //    user.*.prop
    //    Don't worry about the other contents, the build process will rewrite with corrected config files, the placeholders just need to exist.
    replaceAllInFile(pathConfig, "mmsys.prop", previousRelease,
        currentReleaseFolder.getName());
    replaceAllInFile(currentReleaseFolder, "umls.prop", previousRelease,
        currentReleaseFolder.getName());
    replaceAllInFile(currentReleaseFolder, "user.a.prop", previousRelease,
        currentReleaseFolder.getName());
    replaceAllInFile(currentReleaseFolder, "user.b.prop", previousRelease,
        currentReleaseFolder.getName());
    replaceAllInFile(currentReleaseFolder, "user.c.prop", previousRelease,
        currentReleaseFolder.getName());
    replaceAllInFile(currentReleaseFolder, "user.d.prop", previousRelease,
        currentReleaseFolder.getName());
    replaceAllInFile(currentReleaseFolder, "release.dat", previousRelease,
        currentReleaseFolder.getName());
    
    //Delete original /local/content/MEME/MEME5/mr/META/mmsys.zip
    ConfigUtility.deleteFileIfExists(originalZip);
    
    //Zip the contents of path/x into revised path/META/mmsys.zip 

    ZipParameters params = new ZipParameters();
    params.setIncludeRootFolder(false);
    new ZipFile(originalZip.getPath()).addFolder(pathTemp, params);
    
    logInfo("Finished " + getName());
  }
  
  public void replaceAllInFile(File folder, String file, String previousRelease, String currentRelease) throws Exception {
	  Path path = ConfigUtility.resolveFileUnderDirectory(folder, file,
        "MetamorphoSys config file").toPath();
	    Charset charset = StandardCharsets.UTF_8;

	    String content = new String(Files.readAllBytes(path), charset);
	    content = content.replaceAll(previousRelease, currentRelease);
	    Files.write(path, content.getBytes(charset));
  }

  
  public static File getLastModified(File directoryFile)
  {
      File[] files = directoryFile.listFiles(File::isDirectory);
      long lastModifiedTime = Long.MIN_VALUE;
      File chosenFile = null;

      if (files != null)
      {
          for (File file : files)
          {
              if (file.lastModified() > lastModifiedTime)
              {
                  chosenFile = file;
                  lastModifiedTime = file.lastModified();
              }
          }
      }

      return chosenFile;
  }
  
  /* see superclass */
  @Override
  public void reset() throws Exception {
    logInfo("Starting RESET " + getName());
    // n/a
    logInfo("Finished RESET " + getName());
  }

  /* see superclass */
  @Override
  public void checkProperties(Properties p) throws Exception {
    checkRequiredProperties(new String[] {
        ""
    }, p);
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {
    if (p.getProperty("email") != null) {
      email = p.getProperty("email");
    }
  }

  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters() throws Exception {
    final List<AlgorithmParameter> params = super.getParameters();
    final AlgorithmParameter email = new AlgorithmParameterJpa(
        "Notification emails", "email", "Email addresses for notification",
        "e.g. a@b.com", 4000, AlgorithmParameter.Type.TEXT,
        PropertyUtility.getProperties().getProperty("mail.smtp.to"));
    params.add(email);
    return params;
  }

  /* see superclass */
  @Override
  public String getDescription() {
    return ConfigUtility.getNameFromClass(getClass());
  }

  /**
   * Returns the email.
   *
   * @return the email
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets the email.
   *
   * @param email the email
   */
  public void setEmail(String email) {
    this.email = email;
  }

}
