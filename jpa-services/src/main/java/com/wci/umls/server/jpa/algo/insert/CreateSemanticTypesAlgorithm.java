/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.insert;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;

import com.wci.umls.server.AlgorithmParameter;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.jpa.AlgorithmParameterJpa;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractInsertMaintReleaseAlgorithm;

/**
 * Creates semantic types for sources such as MDR and SNOMEDCT_US where there
 * were additional perl scripts run post insertion and stys are added at the end
 * of the recipe. This algorithm replaces the perl scripts.
 */
public class CreateSemanticTypesAlgorithm extends AbstractInsertMaintReleaseAlgorithm {

	// currently, algorithm will address one terminology at a time - indicate here
	private String terminology = null;
	private String version = null;

	/**
	 * Instantiates an empty {@link CreateSemanticTypesAlgorithm}.
	 * 
	 * @throws Exception if anything goes wrong
	 */
	public CreateSemanticTypesAlgorithm() throws Exception {
		super();
		setActivityId(UUID.randomUUID().toString());
		setWorkId("CREATESEMANTICTYPES");
		setLastModifiedBy("admin");
	}

	/* see superclass */
	@Override
	public ValidationResult checkPreconditions() throws Exception {

		ValidationResult validationResult = new ValidationResultJpa();

		if (getProject() == null) {
			throw new Exception("Create semantic types requires a project to be set");
		}

		// Check the input directories

		final String srcFullPath = ConfigUtility.getConfigProperties().getProperty("source.data.dir") + File.separator
				+ getProcess().getInputPath();

		setSrcDirFile(new File(srcFullPath));

		// check the inversion src folder
		String inversionSrcFolderName = getSrcDirFile().getParentFile() + File.separator + "src";
		File inversionSrcFolder = new File(inversionSrcFolderName);
		if (!inversionSrcFolder.exists()) {
			throw new Exception("Inversion src folder must exist.");
		}

		File file = new File(inversionSrcFolder, "attributes.src");
		if (!file.exists()) {
			throw new Exception("Inversion src folder must contain attributes.src .");
		}

		if (terminology == null) {
			throw new Exception("Create semantic types requires a terminology");
		}

		if (version == null) {
			throw new Exception("Create semantic types requires a version");
		}

		return validationResult;
	}

	/* see superclass */
	@Override
	public void compute() throws Exception {
		logInfo("Starting " + getName());

		// No molecular actions will be generated by this algorithm
		setMolecularActionFlag(false);

		try {

			processSTYs();

			logInfo("Finished " + getName());

		} catch (Exception e) {
			logError("Unexpected problem - " + e.getMessage());
			throw e;
		}

	}

	/* see superclass */
	@Override
	public void reset() throws Exception {
		logInfo("Starting RESET " + getName());
		// n/a - No reset
		logInfo("Finished RESET " + getName());
	}

	/* see superclass */
	@Override
	public void checkProperties(Properties p) throws Exception {

		checkRequiredProperties(new String[] { "terminology", "version" }, p);
	}

	/* see superclass */
	@Override
	public void setProperties(Properties p) throws Exception {
		if (p.getProperty("terminology") != null) {
			terminology = String.valueOf(p.getProperty("terminology"));
		}
		if (p.getProperty("version") != null) {
			version = String.valueOf(p.getProperty("version"));
		}
	}

	/* see superclass */
	@Override
	public List<AlgorithmParameter> getParameters() throws Exception {
		final List<AlgorithmParameter> params = super.getParameters();
		AlgorithmParameter param = new AlgorithmParameterJpa("Terminology", "terminology",
				"Semantic types for this terminology will be created.", "e.g. SNOMEDCT_US", 200,
				AlgorithmParameter.Type.STRING, "");
		params.add(param);

		// Version
		param = new AlgorithmParameterJpa("Version", "version", "The version of the terminology", "e.g. 2017_06D", 40,
				AlgorithmParameter.Type.STRING, "");
		params.add(param);

		return params;
	}

	@Override
	public String getDescription() {
		return "Semantic Types will be created for the given terminology/version";
	}

	public void processSTYs() throws Exception {
		String ptree, psaid;
		String tty, sdui, rel;
		int aid = 0;
		Map<String, List<String>> said2stys = new HashMap<>();
		Map<String, String> saidsNeedStys = new HashMap<>();
		Map<String, Integer> smqSaids = new HashMap<>();
		Map<String, String> ttySdui2Said = new HashMap<>();

		// first, get current max id from attributes.src file
		final String attributesFile = getSrcDirFile() + File.separator + "attributes.src";

		try {
			BufferedReader in = new BufferedReader(new FileReader(attributesFile));
			String line;
			while ((line = in.readLine()) != null) {
				String[] parts = line.split("\\|");
				int id = Integer.parseInt(parts[0]);
				if (id > aid) {
					aid = id;
				}
			}
			in.close();
			logInfo("max attribute id: " + aid);
		} catch (Exception e) {
			logError("Could not open attributes.src file to discover max attributeId.");
			return;
		}

		// first prepare said2stys from the generated file sty_term_ids.
		// this file is generated during test insertion.
		try {
			final String styTermIdsFile = getSrcDirFile() + File.separator + "sty_term_ids";

			BufferedReader in = new BufferedReader(new FileReader(styTermIdsFile));
			String line;
			while ((line = in.readLine()) != null) {
				String[] parts = line.split("\\|");
				String said = parts[0];
				String sty = parts[1];
				if (!said2stys.containsKey(said)) {
					said2stys.put(said, new ArrayList<>());
				}
				said2stys.get(said).add(sty);
			}
			in.close();
		} catch (IOException e) {
			logError("Could not open sty_term_ids file.");
			return;
		}

		// now read atoms file
		// remember saids whose stys are not yet populated
		final String classesAtomsFile = getSrcDirFile() + File.separator + "classes_atoms.src";

		try {
			BufferedReader in = new BufferedReader(new FileReader(classesAtomsFile));
			String line;
			while ((line = in.readLine()) != null) {
				String[] parts = line.split("\\|");
				if (!parts[1].equals(terminology + "_" + version)) {
					continue;
				}
				String said = parts[0];
				String[] subParts = parts[2].split("/");
				tty = subParts[0];
				sdui = parts[11];
				// if sty_term_ids didn't have each said from classes_atoms, add it to needing
				// list
				if (!said2stys.containsKey(said)) {
					// store its tty and sdui(12th field).
					saidsNeedStys.put(said, tty + "|" + sdui);
					if (parts[2].contains("SMQ")) {
						smqSaids.put(said, smqSaids.getOrDefault(said, 0) + 1);
					}
				} else {
					ttySdui2Said.put(tty + "|" + sdui, said);
				}
			}
			in.close();
		} catch (IOException e) {
			logError("Could not open classes_atoms.src file.");
			return;
		}

		try {
			FileWriter out = new FileWriter(getSrcDirFile() + File.separator + "new_sty_defaults");

			FileWriter fw = new FileWriter(getSrcDirFile() + File.separator + "attributes.src", true);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter pw = new PrintWriter(bw);

			/// for these walk through the context trees and assign stys.
			final String contextsFile = getSrcDirFile() + File.separator + "contexts.src";

			BufferedReader in = new BufferedReader(new FileReader(contextsFile));
			String line;
			int count = 0;
			while ((line = in.readLine()) != null) {
				String[] parts = line.split("\\|");
				String said = parts[0];
				rel = parts[1];
				ptree = parts[7];
				if (!rel.equals("PAR")) {
					continue;
				}
				if (said2stys.containsKey(said)) {
					continue;
				}
				// this said doesn't have any assigned stys. so walk the tree and assign
				// one.
				String[] psaids = ptree.split("\\.");
				for (int i = psaids.length - 1; i >= 0; i--) {
					psaid = psaids[i];
					if (said2stys.containsKey(psaid)) {
						said2stys.put(said, new ArrayList<>(said2stys.get(psaid)));
						String ign = saidsNeedStys.get(said);
						ttySdui2Said.put(ign, said);
						saidsNeedStys.remove(said);
						count++;
						out.write("\t" + said + "|" + ign + " added(tree): " + said2stys.get(said) + "\n");
						break;
					}
				}
			}
			logInfo("Added(tree) count: " + count);
			in.close();

			// now find stys via sdui for the missing ones.
			// do it in two stages: first assign stys for LLT/OL and then for mth atoms.
			count = 0;
			for (String said : saidsNeedStys.keySet()) {
				String[] parts = saidsNeedStys.get(said).split("\\|");
				tty = parts[0];
				sdui = parts[1];
				if (tty.matches(".*MTH.*")) {
					continue;
				}
				if (tty.equals("LLT") || tty.equals("OL")) {
					psaid = ttySdui2Said.get("PT|" + sdui);
					if (said2stys.containsKey(psaid)) {
						said2stys.put(said, new ArrayList<>(said2stys.get(psaid)));
						String ign = saidsNeedStys.get(said);
						ttySdui2Said.put(ign, said);
						saidsNeedStys.remove(said);
						count++;
						out.write("\t" + said + "|" + ign + " added(LLT/OL): " + said2stys.get(said) + "\n");
					}
				}
			}
			logInfo("Added(LLT/OL) count: " + count);

			// now assign stys for mth atoms via sdui
			count = 0;
			for (String said : saidsNeedStys.keySet()) {
				String[] parts = saidsNeedStys.get(said).split("\\|");
				tty = parts[0];
				sdui = parts[1];
				if (!tty.matches(".*MTH.*")) {
					continue;
				}
				tty = tty.replace("MTH_", "");
				psaid = ttySdui2Said.get(tty + "|" + sdui);
				if (said2stys.containsKey(psaid)) {
					said2stys.put(said, new ArrayList<>(said2stys.get(psaid)));
					String ign = saidsNeedStys.get(said);
					ttySdui2Said.put(ign, said);
					saidsNeedStys.remove(said);
					count++;
					out.write("\t" + said + "|" + ign + " added(MTH): " + said2stys.get(said) + "\n");

				}
			}
			logInfo("Added(MTH) count: " + count);

			// add "Classification" as sty for all SMQ atoms
			count = 0;
			for (String said : smqSaids.keySet()) {
				if (!said2stys.containsKey(said)) {
					said2stys.put(said, new ArrayList<>());
				}
				said2stys.get(said).add("Classification");
				if (saidsNeedStys.containsKey(said)) {
					String ign = saidsNeedStys.get(said);
					ttySdui2Said.put(ign, said);
					saidsNeedStys.remove(said);
					count++;
					out.write("\t" + said + "|" + ign + " added(SMQ): " + said2stys.get(said) + "\n");

				}
			}
			logInfo("Added(SMQ) count: " + count);

			// now append stys as attributes.
			List<String> sortedSaids = new ArrayList<String>(said2stys.keySet());
			Collections.sort(sortedSaids);
			for (String said : sortedSaids) {
				for (String sty : said2stys.get(said)) {
					out.write(said + "|" + sty + "\n");
					attAppender(pw, ++aid, said, sty);
				}
			}

			// whatever is remaining in saidsNeedStys, we need to do something.
			int ct = saidsNeedStys.size();
			logInfo("The " + ct + " saids are remaining without semantic types.");
			for (String skey : saidsNeedStys.keySet()) {
				logInfo("\t" + skey + "|" + saidsNeedStys.get(skey));
			}

			pw.flush();
			out.close();
			fw.close();
			pw.close();
			bw.close();

			// attributes.src was modified in the getSrcDir folder (inversion test
			// directory)
			// now it needs to be copied to the inversion src folder
			// but first backup the attributes.src file in the inversion src folder

			// get inversion src folder
			String inversionSrcFolderName = getSrcDirFile().getParentFile() + File.separator + "src";
			File inversionSrcFolder = new File(inversionSrcFolderName);
			if (!inversionSrcFolder.exists()) {
				throw new Exception("attributes.src modified in " + getSrcDirFile()
						+ " but unable to access the inversion src folder.");
			}
			// File with old name
			File file = new File(inversionSrcFolder, "attributes.src");

			// File with new name
			File file2 = new File(inversionSrcFolder, "attributes.orig");

			if (file2.exists())
				throw new java.io.IOException(file2.getAbsolutePath() + " already exists");

			// Rename file (or directory)
			boolean success = file.renameTo(file2);

			if (!success) {
				// attributes.src was not successfully renamed
				throw new Exception(
						"unable to make backup copy of " + inversionSrcFolder + File.separator + "attributes.src");
			}

			// copy modified test/attributes.src to the inversionSrcFolder
			logInfo("copying modified file: " + attributesFile + " to inversion src folder: " + file);
			FileUtils.copyFile(new File(attributesFile), file);

		} catch (Exception e) {
			logError("Error appending to attributes.src file. " + e.getMessage());
			throw e;
		}
	}

	private void attAppender(PrintWriter pw, int aid, String said, String sty) throws Exception {
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(sty.getBytes());
		byte[] digest = md.digest();
		String myHash = DatatypeConverter.printHexBinary(digest).toLowerCase();
		pw.println(aid + "|" + said + "|C|SEMANTIC_TYPE|" + sty + "|E-" + terminology + "_" + version
				+ "|N|Y|N|N|SRC_ATOM_ID|||" + myHash + "|");
	}
}