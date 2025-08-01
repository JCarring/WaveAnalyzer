package com.carrington.WIA.IO;

import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.lang3.SerializationException;

import com.carrington.WIA.Utils;
import com.carrington.WIA.Cardio.Wave;
import com.carrington.WIA.DataStructures.InsensitiveNonDupList;
import com.carrington.WIA.DataStructures.WIAData;
import com.carrington.WIA.GUIs.Components.JCLabel;
import com.carrington.WIA.stats.DataCollection;
import com.carrington.WIA.stats.DataType;
import com.carrington.WIA.stats.Outcome;
import com.carrington.WIA.stats.StatisticalComparison;
import com.carrington.WIA.stats.StatisticalException;

/**
 * Class representing statistics run on sets of {@link WIAData} samples.
 * Provides methods to load data files, perform statistical comparisons across
 * treatments and wave groups, and export results to spreadsheets.
 */
public class WIAStats {

	private List<WIAData> wiaData = new ArrayList<WIAData>();
	private List<StandardWave> standardWaves = new ArrayList<StandardWave>();
	private List<StandardWaveGrouping> standardWaveGroups = new ArrayList<StandardWaveGrouping>();
	private List<StandardTreatment> standardTreatments = new ArrayList<StandardTreatment>();

	private List<StatisticalComparison> statsComparisons = new ArrayList<StatisticalComparison>();

	private boolean skipDiameters = false;
	private String name;

	private final int TYPE_ENDO_INDEP = 1;
	private final int TYPE_ENDO_INDEP_ONLY = 2;
	private final int TYPE_ENDO_DEP = 3;
	private final int TYPE_ENDO_DEP_ONLY = 4;
	private final int TYPE_ENDO_BOTH = 5;

	/**
	 * Constructs a new instance. This contains all standard waves, wave groups, and
	 * statistical comparisons.
	 * 
	 * @param nameOfStats name of the {@link WIAStats} object
	 */
	public WIAStats(String nameOfStats) {
		this.name = nameOfStats;
	}

	/**
	 * @return name of this statistics object
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return the raw {@link WIAData} objects loaded into this instance for
	 *         analysis.
	 */
	public List<WIAData> getData() {
		return Collections.unmodifiableList(wiaData);
	}

	/**
	 * Removes a {@link WIAData} sample from this stats object. Also removes any
	 * now-empty wave or treatment entries that referenced it. Does NOT change GUI
	 * components
	 * 
	 * @param data the {@link WIAData} to remove
	 */
	public void removeData(WIAData data) {
		wiaData.remove(data);
		List<StandardWave> wavesToRemove = new ArrayList<StandardWave>();
		for (StandardWave wave : this.standardWaves) {
			wave.removeSample(data);
			if (wave.getCount() == 0) {
				wavesToRemove.add(wave);
			}

		}
		wavesToRemove.stream().forEach(wave -> removeWave(wave));

		List<StandardTreatment> txToRemove = new ArrayList<StandardTreatment>();
		for (StandardTreatment tx : this.standardTreatments) {
			tx.removeSample(data);
			if (tx.getCount() == 0) {
				txToRemove.add(tx);

			}
		}
		txToRemove.stream().forEach(tx -> removeTreatment(tx));

	}

	/**
	 * Defines a new named grouping of standard waves. This does NOT change any GUI
	 * representation of the waves. It also does not check to see if the wave with
	 * the specified name already exists.
	 * 
	 * @param name          the group name
	 * @param standardWaves the set of waves to include in this group
	 */
	public void addWaveGrouping(String name, Set<StandardWave> standardWaves) {
		StandardWaveGrouping group = new StandardWaveGrouping(name, standardWaves);
		standardWaveGroups.add(group);
	}

	/**
	 * Identifies and aggregates all unique waves based on their names and direction
	 * (proximal/distal) across all loaded {@link WIAData} samples.
	 */
	private void _findStandardWaves() {
		standardWaves.clear();
		for (WIAData data : wiaData) {
			for (Wave wave : data.getWaves()) {

				StandardWave standardWave = standardWaves.stream().filter(sw -> sw.matches(wave)).findFirst()
						.orElse(null);

				if (standardWave == null) {
					standardWave = new StandardWave(wave.getAbbrev(), wave.isProximal());
					standardWaves.add(standardWave);

				} else if (standardWave.isProximal() != wave.isProximal()) {
					standardWave = new StandardWave(wave.getAbbrev(), wave.isProximal());
					standardWaves.add(standardWave);
				}
				standardWave.addSample(data);

			}
		}
	}

	/**
	 * @return {@link StandardWave}s which were found when {@link WIAData} were
	 *         loaded.
	 */
	public List<StandardWave> getWaves() {
		return Collections.unmodifiableList(standardWaves);
	}

	/**
	 * @param name query name
	 * @return true if there is a {@link StandardWave} whose name matches the input
	 *         name, ignoring case.
	 */
	public boolean containsWave(String name) {

		if (name == null || name.isBlank())
			return false;

		return standardWaves.stream().filter(str -> name.equalsIgnoreCase(str.waveName)).findFirst().isPresent();

	}

	/**
	 * Identifies and aggregates all unique treatment groups based on the selection
	 * name in each loaded {@link WIAData} sample.
	 */
	private void _findStandardTreatments() {
		standardTreatments.clear();
		for (WIAData data : wiaData) {

			StandardTreatment standardTx = standardTreatments.stream().filter(tx -> tx.matches(data.getSelectionName()))
					.findFirst().orElse(null);

			if (standardTx == null) {
				standardTx = new StandardTreatment(data.getSelectionName());
				standardTreatments.add(standardTx);

			}
			standardTx.addSample(data);

		}
	}

	/**
	 * Sets the treatment name for the specified {@link WIAData}
	 * 
	 * @param name name of treatment
	 * @param data data input
	 * 
	 */
	public void setStandardTreatmentName(String name, WIAData data) {

		if (name.equals(data.getSelectionName())) {
			// no change
			return;
		} else {
			data.setSelectionName(name);
			try {
				WIAData.serialize(data, data.getSerializeFileSource());

			} catch (SerializationException e) {
				Utils.showMessage(JOptionPane.ERROR_MESSAGE, "Unable to save to file. (Msg: " + e.getMessage() + ")", null);
			}
		}

		StandardTreatment st = this.standardTreatments.stream().filter(tx -> tx.getSamples().contains(data)).findFirst()
				.orElse(null);

		if (st.matches(name))
			return; // the new name is same as old (or matches case insensitive if that is set)

		if (st.getCount() == 1) {
			removeTreatment(st);
		} else {
			st.removeSample(data);
		}

		StandardTreatment stNew = this.standardTreatments.stream().filter(tx -> tx.matches(name)).findFirst()
				.orElse(null);
		if (stNew == null) {
			stNew = new StandardTreatment(name);
			this.standardTreatments.add(stNew);
		}

		stNew.addSample(data);

	}

	/**
	 * @return list of {@link StandardTreatment} as were identified by
	 *         {@link WIAData#getSelectionName()}. The treatment names are case
	 *         insensitive.
	 */
	public List<StandardTreatment> getTreatments() {
		return Collections.unmodifiableList(standardTreatments);
	}

	/**
	 * @param name query
	 * @return true if there is a {@link StandardWaveGrouping} with this name, case
	 *         insensitive
	 */
	public boolean containsGroup(String name) {

		return standardWaveGroups.stream().filter(str -> name.equalsIgnoreCase(str.groupName)).findFirst().isPresent();

	}

	/**
	 * @return unmodifiable list of {@link StandardWaveGrouping}
	 */
	public List<StandardWaveGrouping> getWaveGrouping() {
		return Collections.unmodifiableList(standardWaveGroups);
	}

	/**
	 * @param standardTreatment to remove
	 * @return true if removed
	 */
	public boolean removeTreatment(StandardTreatment standardTreatment) {
		return standardTreatments.remove(standardTreatment);
	}

	/**
	 * @param standardWave to remove
	 * @return true if removed
	 */
	public boolean removeWave(StandardWave standardWave) {
		boolean removed = standardWaves.remove(standardWave);
		if (removed) {
			standardWaveGroups.stream().forEach(swp -> swp.standardWavesInGroup.remove(standardWave));
			standardWaveGroups.removeIf(swp -> swp.standardWavesInGroup.isEmpty());
		}
		return removed;
	}

	/**
	 * @param standardWaveGroup to remove
	 * @return true if removed
	 */
	public boolean removeGrouping(StandardWaveGrouping standardWaveGroup) {
		return standardWaveGroups.remove(standardWaveGroup);
	}

	/**
	 * Loads WIA data files. These must have extension ".wia" and be serialized
	 * {@link WIAData} objects.
	 * 
	 * @param file      the file to load with extension ".wia", or a folder
	 * @param recursive true if should get all WIA files within the input
	 *                  {@link File}. Typically this means the input {@link File}
	 *                  should be a folder.
	 * @return errors that occurred, or null if none. This includes being unable to
	 *         read the file system or failure with deserialization of
	 *         {@link WIAData} objects.
	 */
	public String loadFiles(File file, boolean recursive) {

		List<WIAData> listOfWIA = new ArrayList<WIAData>();
		String errors;
		if (recursive) {
			errors = getFilesRecursively(file, listOfWIA);
		} else {
			errors = getFiles(file, listOfWIA);
		}
		for (WIAData data : listOfWIA) {
			data.retryCalculations();
		}
		wiaData.addAll(listOfWIA);

		_findStandardWaves();
		_findStandardTreatments();
		addCalculatedFields();

		return errors;

	}

	/**
	 * Collects .wia files from a specified directory or loads a single .wia file.
	 * This method is not recursive.
	 *
	 * @param file The directory or file to inspect.
	 * @param set  The list to populate with deserialized {@link WIAData} objects.
	 * @return An error message if reading fails, otherwise null.
	 */
	private String getFiles(File file, List<WIAData> set) {
		String errorRead = Utils.checkCanReadFile(file);
		if (errorRead != null)
			return errorRead;

		return null;

	}

	/**
	 * Recursively collects .wia files from a root directory and all its
	 * subdirectories.
	 * 
	 * @param file The root directory to start searching from.
	 * @param set  The list to populate with deserialized {@link WIAData} objects.
	 * @return An error message if reading fails at any point, otherwise null.
	 */
	private String getFilesRecursively(File file, List<WIAData> set) {

		String errorRead = Utils.checkCanReadFile(file);
		if (errorRead != null) {
			return errorRead;

		}

		if (!file.isDirectory()) {

			if (Utils.hasOkayExtension(file, ".wia")) {
				try {
					WIAData data = WIAData.deserialize(file);
					set.add(data);

				} catch (SerializationException e) {
					return e.getMessage();
				}
			}
			return null;
		}

		for (File qFile : file.listFiles()) {

			errorRead = Utils.checkCanReadFile(file);
			if (errorRead != null) {
				return errorRead;
			}

			if (qFile.isDirectory()) {

				String errors = getFilesRecursively(qFile, set);
				if (errors != null) {

					return errors;
				}

			} else if (Utils.hasOkayExtension(qFile, ".wia")) {

				try {
					WIAData data = WIAData.deserialize(qFile);
					set.add(data);

				} catch (SerializationException e) {

					return e.getMessage();
				}
			}

		}

		return null;

	}

	/**
	 * Performs statistics based on the currently loaded {@link WIAData} files.
	 * 
	 * @throws StatisticalException if there are too few comparison groups (only 1
	 *                              treatment), too few samples (less than 2 per
	 *                              sample)
	 */
	public void runStats() throws StatisticalException {

		this.statsComparisons.clear();
		int numTx = this.standardTreatments.size();

		// comparison between treatments, pairwise
		if (numTx > 1) {
			for (int i = 0; i < numTx - 1; i++) {
				for (int j = i + 1; j < numTx; j++) {
					LinkedHashMap<String, Collection<WIAData>> compar = new LinkedHashMap<String, Collection<WIAData>>();
					StandardTreatment tx1 = this.standardTreatments.get(i);
					StandardTreatment tx2 = this.standardTreatments.get(j);
					compar.put(tx1.getName(), tx1.getSamples());
					compar.put(tx2.getName(), tx2.getSamples());
					try {
						_compareStats(tx1.getName() + " vs " + tx2.getName(), compar, true);

					} catch (Exception e) {
						e.printStackTrace();
						throw new StatisticalException(e.getMessage());
					}
				}
			}
		}

		// comparison between ALL treatments at same time
		System.out.println("Called 1");
		if (this.standardTreatments.size() > 2) {
			System.out.println("Called 2");
			LinkedHashMap<String, Collection<WIAData>> compar = new LinkedHashMap<String, Collection<WIAData>>();
			for (StandardTreatment tx : this.standardTreatments) {
				compar.put(tx.getName(), tx.getSamples());
			}
			try {
				_compareStats("Comparison of all treatments (" + String.join(", ", compar.keySet()) + ")", compar,
						true);
			} catch (Exception e) {
				e.printStackTrace();
				throw new StatisticalException(e.getMessage());
			}
		}

		// comparison between ALL treatments at same time, for each subgroup
		if (this.standardTreatments.size() > 2) {
			LinkedHashMap<String, Collection<WIAData>> comparCMD = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparNonCMD = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparEndothDep = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparEndothIndep = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparEndothDepFunc = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparEndothDepStruct = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparEndothIndepDepFunc = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparEndothIndepDepStruct = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparFunc = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparStruct = new LinkedHashMap<String, Collection<WIAData>>();

			for (StandardTreatment tx : this.standardTreatments) {
				comparCMD.put(tx.getName(), _subsetCMD(tx.getSamples(), true));
				comparNonCMD.put(tx.getName(), _subsetCMD(tx.getSamples(), false));
				comparEndothDep.put(tx.getName(), _subsetCMDEndothelialDep(tx.getSamples(), TYPE_ENDO_DEP));
				comparEndothIndep.put(tx.getName(), _subsetCMDEndothelialDep(tx.getSamples(), TYPE_ENDO_INDEP));
				comparEndothDepFunc.put(tx.getName(),
						_subsetCMDEndothelialDepFunc(tx.getSamples(), TYPE_ENDO_DEP, true));
				comparEndothDepStruct.put(tx.getName(),
						_subsetCMDEndothelialDepFunc(tx.getSamples(), TYPE_ENDO_DEP, false));
				comparEndothIndepDepFunc.put(tx.getName(),
						_subsetCMDEndothelialDepFunc(tx.getSamples(), TYPE_ENDO_INDEP, true));
				comparEndothIndepDepStruct.put(tx.getName(),
						_subsetCMDEndothelialDepFunc(tx.getSamples(), TYPE_ENDO_INDEP, false));
				comparFunc.put(tx.getName(), _subsetCMDFunctional(tx.getSamples(), true));
				comparStruct.put(tx.getName(), _subsetCMDFunctional(tx.getSamples(), false));

			}

			try {
				_compareStats(
						"Comparison of all treatments (" + String.join(", ", comparNonCMD.keySet()) + ") (no CMD)",
						comparCMD, false);

				_compareStats("Comparison of all treatments (" + String.join(", ", comparCMD.keySet()) + ") (CMD)",
						comparNonCMD, false);

				_compareStats("Comparison of all treatments (" + String.join(", ", comparEndothDep.keySet())
						+ ") (Endothelium-dependent CMD)", comparEndothDep, false);

				_compareStats("Comparison of all treatments (" + String.join(", ", comparEndothIndep.keySet())
						+ ") (Endothelium-independent CMD)", comparEndothIndep, false);

				_compareStats("Comparison of all treatments (" + String.join(", ", comparEndothDepFunc.keySet())
						+ ") (Endothelium-dependent, functional CMD)", comparEndothDepFunc, false);

				_compareStats("Comparison of all treatments (" + String.join(", ", comparEndothDepStruct.keySet())
						+ ") (Endothelium-dependent, structural CMD)", comparEndothDepStruct, false);

				_compareStats("Comparison of all treatments (" + String.join(", ", comparEndothIndepDepFunc.keySet())
						+ ") (Endothelium-independent, functional CMD)", comparEndothIndepDepFunc, false);

				_compareStats("Comparison of all treatments (" + String.join(", ", comparEndothIndepDepStruct.keySet())
						+ ") (Endothelium-independent, structural CMD)", comparEndothIndepDepStruct, false);

				_compareStats("Comparison of all treatments (" + String.join(", ", comparFunc.keySet())
						+ ") (Functional CMD)", comparFunc, false);

				_compareStats("Comparison of all treatments (" + String.join(", ", comparStruct.keySet())
						+ ") (Structural CMD)", comparStruct, false);

			} catch (Exception e) {
				e.printStackTrace();
				throw new StatisticalException(e.getMessage());
			}

		}

		// comparison of waves within each treatment (including ALL)
		for (StandardTreatment st : this.standardTreatments) {
			try {
				_compareWavesWithinGroup(st.getName() + " treatment wave comparison", st.getSamples());

			} catch (Exception e) {
				e.printStackTrace();
				throw new StatisticalException(e.getMessage());
			}
		}

		// comparison of waves within each treatment (including ALL), among the subtypes
		for (StandardTreatment st : this.standardTreatments) {
			try {
				_compareWavesWithinGroup(st.getName() + " treatment wave comparison (No CMD)",
						_subsetCMD(st.getSamples(), false));
				_compareWavesWithinGroup(st.getName() + " treatment wave comparison (CMD)",
						_subsetCMD(st.getSamples(), true));
				_compareWavesWithinGroup(st.getName() + " treatment wave comparison (Endothelium-dependent CMD)",
						_subsetCMDEndothelialDep(st.getSamples(), TYPE_ENDO_DEP));
				_compareWavesWithinGroup(st.getName() + " treatment wave comparison (Endothelium-independent CMD)",
						_subsetCMDEndothelialDep(st.getSamples(), TYPE_ENDO_INDEP));
				_compareWavesWithinGroup(
						st.getName() + " treatment wave comparison (Endothelium-dependent, functional CMD)",
						_subsetCMDEndothelialDepFunc(st.getSamples(), TYPE_ENDO_DEP, true));
				_compareWavesWithinGroup(
						st.getName() + " treatment wave comparison (Endothelium-dependent, structural CMD)",
						_subsetCMDEndothelialDepFunc(st.getSamples(), TYPE_ENDO_DEP, false));
				_compareWavesWithinGroup(
						st.getName() + " treatment wave comparison (Endothelium-independent, functional CMD)",
						_subsetCMDEndothelialDepFunc(st.getSamples(), TYPE_ENDO_INDEP, true));
				_compareWavesWithinGroup(
						st.getName() + " treatment wave comparison (Endothelium-independent, structural CMD)",
						_subsetCMDEndothelialDepFunc(st.getSamples(), TYPE_ENDO_INDEP, false));
				_compareWavesWithinGroup(st.getName() + " treatment wave comparison (Functional CMD)",
						_subsetCMDFunctional(st.getSamples(), true));
				_compareWavesWithinGroup(st.getName() + " treatment wave comparison (Structural CMD)",
						_subsetCMDFunctional(st.getSamples(), false));

			} catch (Exception e) {
				e.printStackTrace();
				throw new StatisticalException(e.getMessage());
			}
		}

		// Compare between treatments for CMD, between treatment for non-CMD.
		// Same with Endothelium-dependent vs -independent, and functional vs
		// structural.
		if (numTx > 1) {
			for (int i = 0; i < numTx - 1; i++) {
				for (int j = i + 1; j < numTx; j++) {

					StandardTreatment tx1 = this.standardTreatments.get(i);
					StandardTreatment tx2 = this.standardTreatments.get(j);
					LinkedHashMap<String, Collection<WIAData>> comparCMD = new LinkedHashMap<String, Collection<WIAData>>();
					LinkedHashMap<String, Collection<WIAData>> comparNonCMD = new LinkedHashMap<String, Collection<WIAData>>();
					LinkedHashMap<String, Collection<WIAData>> comparEndothDep = new LinkedHashMap<String, Collection<WIAData>>();
					LinkedHashMap<String, Collection<WIAData>> comparEndothIndep = new LinkedHashMap<String, Collection<WIAData>>();
					LinkedHashMap<String, Collection<WIAData>> comparEndothDepFunc = new LinkedHashMap<String, Collection<WIAData>>();
					LinkedHashMap<String, Collection<WIAData>> comparEndothDepStruct = new LinkedHashMap<String, Collection<WIAData>>();
					LinkedHashMap<String, Collection<WIAData>> comparEndothIndepDepFunc = new LinkedHashMap<String, Collection<WIAData>>();
					LinkedHashMap<String, Collection<WIAData>> comparEndothIndepDepStruct = new LinkedHashMap<String, Collection<WIAData>>();
					LinkedHashMap<String, Collection<WIAData>> comparFunc = new LinkedHashMap<String, Collection<WIAData>>();
					LinkedHashMap<String, Collection<WIAData>> comparStruct = new LinkedHashMap<String, Collection<WIAData>>();

					comparCMD.put(tx1.getName(), _subsetCMD(tx1.getSamples(), true));
					comparCMD.put(tx2.getName(), _subsetCMD(tx2.getSamples(), true));
					comparNonCMD.put(tx1.getName(), _subsetCMD(tx1.getSamples(), false));
					comparNonCMD.put(tx2.getName(), _subsetCMD(tx2.getSamples(), false));
					comparEndothDep.put(tx1.getName(), _subsetCMDEndothelialDep(tx1.getSamples(), TYPE_ENDO_DEP));
					comparEndothDep.put(tx2.getName(), _subsetCMDEndothelialDep(tx2.getSamples(), TYPE_ENDO_DEP));
					comparEndothIndep.put(tx1.getName(), _subsetCMDEndothelialDep(tx1.getSamples(), TYPE_ENDO_INDEP));
					comparEndothIndep.put(tx2.getName(), _subsetCMDEndothelialDep(tx2.getSamples(), TYPE_ENDO_INDEP));
					comparEndothDepFunc.put(tx1.getName(),
							_subsetCMDEndothelialDepFunc(tx1.getSamples(), TYPE_ENDO_DEP, true));
					comparEndothDepFunc.put(tx2.getName(),
							_subsetCMDEndothelialDepFunc(tx2.getSamples(), TYPE_ENDO_DEP, true));
					comparEndothDepStruct.put(tx1.getName(),
							_subsetCMDEndothelialDepFunc(tx1.getSamples(), TYPE_ENDO_DEP, false));
					comparEndothDepStruct.put(tx2.getName(),
							_subsetCMDEndothelialDepFunc(tx2.getSamples(), TYPE_ENDO_DEP, false));
					comparEndothIndepDepFunc.put(tx1.getName(),
							_subsetCMDEndothelialDepFunc(tx1.getSamples(), TYPE_ENDO_INDEP, true));
					comparEndothIndepDepFunc.put(tx2.getName(),
							_subsetCMDEndothelialDepFunc(tx2.getSamples(), TYPE_ENDO_INDEP, true));
					comparEndothIndepDepStruct.put(tx1.getName(),
							_subsetCMDEndothelialDepFunc(tx1.getSamples(), TYPE_ENDO_INDEP, false));
					comparEndothIndepDepStruct.put(tx2.getName(),
							_subsetCMDEndothelialDepFunc(tx2.getSamples(), TYPE_ENDO_INDEP, false));
					comparFunc.put(tx1.getName(), _subsetCMDFunctional(tx1.getSamples(), true));
					comparFunc.put(tx2.getName(), _subsetCMDFunctional(tx2.getSamples(), true));
					comparStruct.put(tx1.getName(), _subsetCMDFunctional(tx1.getSamples(), false));
					comparStruct.put(tx2.getName(), _subsetCMDFunctional(tx2.getSamples(), false));

					try {

						_compareStats(tx1.getName() + " vs " + tx2.getName() + " (CMD)", comparCMD, false);
						_compareStats(tx1.getName() + " vs " + tx2.getName() + " (non-CMD)", comparNonCMD, false);
						_compareStats(tx1.getName() + " vs " + tx2.getName() + " (Endothelium-dependent CMD)",
								comparEndothDep, false);
						_compareStats(tx1.getName() + " vs " + tx2.getName() + " (Endothelium-independent CMD)",
								comparEndothIndep, false);
						_compareStats(
								tx1.getName() + " vs " + tx2.getName() + " (Endothelium-dependent, functional CMD)",
								comparEndothDepFunc, false);
						_compareStats(
								tx1.getName() + " vs " + tx2.getName() + " (Endothelium-dependent, structural CMD)",
								comparEndothDepStruct, false);
						_compareStats(
								tx1.getName() + " vs " + tx2.getName() + " (Endothelium-independent, functional CMD)",
								comparEndothIndepDepFunc, false);
						_compareStats(
								tx1.getName() + " vs " + tx2.getName() + " (Endothelium-independent, structural CMD)",
								comparEndothIndepDepStruct, false);
						_compareStats(tx1.getName() + " vs " + tx2.getName() + " (Functional CMD)", comparFunc, false);
						_compareStats(tx1.getName() + " vs " + tx2.getName() + " (Structural CMD)", comparStruct,
								false);

					} catch (Exception e) {
						e.printStackTrace();
						throw new StatisticalException(e.getMessage());
					}
				}
			}
		}

		// comparison between ALL treatments at same time for the above
		if (this.standardTreatments.size() > 2) {
			LinkedHashMap<String, Collection<WIAData>> comparCMD = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparNonCMD = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparEndothDep = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparEndothIndep = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparEndothDepFunc = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparEndothDepStruct = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparEndothIndepDepFunc = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparEndothIndepDepStruct = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparFunc = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparStruct = new LinkedHashMap<String, Collection<WIAData>>();

			for (StandardTreatment tx : this.standardTreatments) {
				comparCMD.put(tx.getName(), _subsetCMD(tx.getSamples(), true));
				comparNonCMD.put(tx.getName(), _subsetCMD(tx.getSamples(), false));
				comparEndothDep.put(tx.getName(), _subsetCMDEndothelialDep(tx.getSamples(), TYPE_ENDO_DEP));
				comparEndothIndep.put(tx.getName(), _subsetCMDEndothelialDep(tx.getSamples(), TYPE_ENDO_INDEP));
				comparEndothDepFunc.put(tx.getName(),
						_subsetCMDEndothelialDepFunc(tx.getSamples(), TYPE_ENDO_DEP, true));
				comparEndothDepStruct.put(tx.getName(),
						_subsetCMDEndothelialDepFunc(tx.getSamples(), TYPE_ENDO_DEP, false));
				comparEndothIndepDepFunc.put(tx.getName(),
						_subsetCMDEndothelialDepFunc(tx.getSamples(), TYPE_ENDO_INDEP, true));
				comparEndothIndepDepStruct.put(tx.getName(),
						_subsetCMDEndothelialDepFunc(tx.getSamples(), TYPE_ENDO_INDEP, false));
				comparFunc.put(tx.getName(), _subsetCMDFunctional(tx.getSamples(), true));
				comparStruct.put(tx.getName(), _subsetCMDFunctional(tx.getSamples(), false));

			}
			try {

				_compareStats("Comparison of all treatments (" + String.join(", ", comparCMD.keySet()) + ") (CMD)",
						comparCMD, false);
				_compareStats(
						"Comparison of all treatments (" + String.join(", ", comparNonCMD.keySet()) + ") (non-CMD)",
						comparNonCMD, false);
				_compareStats("Comparison of all treatments (" + String.join(", ", comparEndothDep.keySet())
						+ ") (Endothelium-dependent CMD)", comparEndothDep, false);
				_compareStats("Comparison of all treatments (" + String.join(", ", comparEndothIndep.keySet())
						+ ") (Endothelium-independent CMD)", comparEndothIndep, false);
				_compareStats("Comparison of all treatments (" + String.join(", ", comparEndothDepFunc.keySet())
						+ ") (Endothelium-dependent, functional CMD)", comparEndothDepFunc, false);
				_compareStats("Comparison of all treatments (" + String.join(", ", comparEndothDepStruct.keySet())
						+ ") (Endothelium-dependent, structural CMD)", comparEndothDepStruct, false);
				_compareStats("Comparison of all treatments (" + String.join(", ", comparEndothIndepDepFunc.keySet())
						+ ") (Endothelium-independent, functional CMD)", comparEndothIndepDepFunc, false);
				_compareStats("Comparison of all treatments (" + String.join(", ", comparEndothIndepDepStruct.keySet())
						+ ") (Endothelium-independent, structural CMD)", comparEndothIndepDepStruct, false);
				_compareStats("Comparison of all treatments (" + String.join(", ", comparFunc.keySet())
						+ ") (Functional CMD)", comparFunc, false);
				_compareStats("Comparison of all treatments (" + String.join(", ", comparStruct.keySet())
						+ ") (Structural CMD)", comparStruct, false);

			} catch (Exception e) {
				e.printStackTrace();
				throw new StatisticalException(e.getMessage());
			}
		}

		// Now compared CMD vs non CMD within treatment. Same for endothelial dependent
		// CMD and functional CMD
		for (StandardTreatment tx : this.standardTreatments) {

			// Multiple tx at once

			LinkedHashMap<String, Collection<WIAData>> comparCMDMultiIndepDepNo = new LinkedHashMap<String, Collection<WIAData>>();
			comparCMDMultiIndepDepNo.put("No CMD", _subsetCMD(tx.getSamples(), false));
			comparCMDMultiIndepDepNo.put("Only endo-dep CMD",
					_subsetCMDEndothelialDep(tx.getSamples(), TYPE_ENDO_DEP_ONLY));
			comparCMDMultiIndepDepNo.put("Only endo-indep CMD",
					_subsetCMDEndothelialDep(tx.getSamples(), TYPE_ENDO_INDEP_ONLY));
			comparCMDMultiIndepDepNo.put("Both endo-dep and indep CMD",
					_subsetCMDEndothelialDep(tx.getSamples(), TYPE_ENDO_BOTH));
			_compareStats(tx.getName()
					+ ", comparison of multiple subtype (No CMD, Only endo-indep CMD, Only endo-dep CMD, Both endo-dep and indep CMD) comparison",
					comparCMDMultiIndepDepNo, false);

			LinkedHashMap<String, Collection<WIAData>> comparCMDMultiIndepDep = new LinkedHashMap<String, Collection<WIAData>>();
			comparCMDMultiIndepDep.put("Only endo-dep CMD",
					_subsetCMDEndothelialDep(tx.getSamples(), TYPE_ENDO_DEP_ONLY));
			comparCMDMultiIndepDep.put("Only endo-indep CMD",
					_subsetCMDEndothelialDep(tx.getSamples(), TYPE_ENDO_INDEP_ONLY));
			comparCMDMultiIndepDep.put("Both endo-dep and indep CMD",
					_subsetCMDEndothelialDep(tx.getSamples(), TYPE_ENDO_BOTH));
			_compareStats(tx.getName()
					+ ", comparison of multiple subtype (Only endo-indep CMD, Only endo-dep CMD, Both endo-dep and indep CMD) comparison",
					comparCMDMultiIndepDep, false);

			LinkedHashMap<String, Collection<WIAData>> comparCMDDepStructFunc = new LinkedHashMap<String, Collection<WIAData>>();
			comparCMDDepStructFunc.put("No CMD", _subsetCMD(tx.getSamples(), false));
			comparCMDDepStructFunc.put("Only endo-dep func CMD",
					_subsetCMDEndothelialDepFunc(tx.getSamples(), TYPE_ENDO_DEP, true));
			comparCMDDepStructFunc.put("Only endo-dep struct CMD",
					_subsetCMDEndothelialDepFunc(tx.getSamples(), TYPE_ENDO_DEP, false));
			_compareStats(
					tx.getName()
							+ ", comparison of multiple subtype  (No CMD, Endo dep struct, Endo dep func) comparison",
					comparCMDDepStructFunc, false);

			LinkedHashMap<String, Collection<WIAData>> comparCMDIndepStructFunc = new LinkedHashMap<String, Collection<WIAData>>();
			comparCMDIndepStructFunc.put("No CMD", _subsetCMD(tx.getSamples(), false));
			comparCMDIndepStructFunc.put("Only endo-indep funct CMD",
					_subsetCMDEndothelialDepFunc(tx.getSamples(), TYPE_ENDO_INDEP, true));
			comparCMDIndepStructFunc.put("Only endo-indep struct CMD",
					_subsetCMDEndothelialDepFunc(tx.getSamples(), TYPE_ENDO_INDEP, false));
			_compareStats(tx.getName()
					+ ", comparison of multiple subtype  (No CMD, Endo indep struct, Endo indep func) comparison",
					comparCMDIndepStructFunc, false);

			LinkedHashMap<String, Collection<WIAData>> comparCMDStructFunc = new LinkedHashMap<String, Collection<WIAData>>();
			comparCMDStructFunc.put("No CMD", _subsetCMD(tx.getSamples(), false));
			comparCMDStructFunc.put("Functional CMD", _subsetCMDFunctional(tx.getSamples(), true));
			comparCMDStructFunc.put("Structural CMD", _subsetCMDFunctional(tx.getSamples(), false));
			_compareStats(
					tx.getName()
							+ ", comparison of multiple subtype  (No CMD, all functional, all structural) comparison",
					comparCMDStructFunc, false);

			// pairwise treatments
			LinkedHashMap<String, Collection<WIAData>> comparCMDvsNonCMD = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparEndothDepVsNone = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparEndothIndepVsNone = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparEndothBothVsNone = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparEndothDepVsIndep = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparEndothDepFunctVsStruct = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparEndothIndepFunctVsStruct = new LinkedHashMap<String, Collection<WIAData>>();
			LinkedHashMap<String, Collection<WIAData>> comparFuncVsStruct = new LinkedHashMap<String, Collection<WIAData>>();

			comparCMDvsNonCMD.put(tx.getName() + " (CMD)", _subsetCMD(tx.getSamples(), true));
			comparCMDvsNonCMD.put(tx.getName() + " (Non-CMD)", _subsetCMD(tx.getSamples(), false));
			comparEndothDepVsNone.put(tx.getName() + " (Endothelium-dependent only CMD)",
					_subsetCMDEndothelialDep(tx.getSamples(), TYPE_ENDO_DEP_ONLY));
			comparEndothDepVsNone.put(tx.getName() + " (Non-CMD)", _subsetCMD(tx.getSamples(), false));
			comparEndothIndepVsNone.put(tx.getName() + " (Endothelium-independent only CMD)",
					_subsetCMDEndothelialDep(tx.getSamples(), TYPE_ENDO_INDEP_ONLY));
			comparEndothIndepVsNone.put(tx.getName() + " (Non-CMD)", _subsetCMD(tx.getSamples(), false));
			comparEndothBothVsNone.put(tx.getName() + " (Endothelium-independent AND -dependent CMD)",
					_subsetCMDEndothelialDep(tx.getSamples(), TYPE_ENDO_BOTH));
			comparEndothBothVsNone.put(tx.getName() + " (Non-CMD)", _subsetCMD(tx.getSamples(), false));

			comparEndothDepVsIndep.put(tx.getName() + " (Endothelium-dependent only CMD)",
					_subsetCMDEndothelialDep(tx.getSamples(), TYPE_ENDO_DEP_ONLY));
			comparEndothDepVsIndep.put(tx.getName() + " (Endothelium-independent only CMD)",
					_subsetCMDEndothelialDep(tx.getSamples(), TYPE_ENDO_INDEP_ONLY));

			comparEndothDepFunctVsStruct.put(tx.getName() + " (Endothelium-dependent, structural CMD)",
					_subsetCMDEndothelialDepFunc(tx.getSamples(), TYPE_ENDO_DEP, false));
			comparEndothDepFunctVsStruct.put(tx.getName() + " (Endothelium-dependent, functional CMD)",
					_subsetCMDEndothelialDepFunc(tx.getSamples(), TYPE_ENDO_DEP, true));
			comparEndothIndepFunctVsStruct.put(tx.getName() + " (Endothelium-independent, structural CMD)",
					_subsetCMDEndothelialDepFunc(tx.getSamples(), TYPE_ENDO_INDEP, false));
			comparEndothIndepFunctVsStruct.put(tx.getName() + " (Endothelium-independent, functional CMD)",
					_subsetCMDEndothelialDepFunc(tx.getSamples(), TYPE_ENDO_INDEP, true));
			comparFuncVsStruct.put(tx.getName() + " (Functional CMD)", _subsetCMDFunctional(tx.getSamples(), true));
			comparFuncVsStruct.put(tx.getName() + " (Structural CMD)", _subsetCMDFunctional(tx.getSamples(), false));
			try {
				_compareStats(tx.getName() + ", comparison of CMD vs non-CMD", comparCMDvsNonCMD, true);
				_compareStats(tx.getName() + ", comparison of Endo-dependent only CMD vs non-CMD",
						comparEndothDepVsNone, true);
				_compareStats(tx.getName() + ", comparison of Endo-independent only CMD vs non-CMD",
						comparEndothIndepVsNone, true);
				_compareStats(tx.getName() + ", comparison of both endo-indep and -dep vs non-CMD",
						comparEndothBothVsNone, true);
				_compareStats(
						tx.getName()
								+ ", comparison of only endothelium-dependent vs only endodothelium-independent CMD",
						comparEndothDepVsIndep, true);
				_compareStats(tx.getName() + ", comparison of Endothelium-dependent structural vs functional CMD",
						comparEndothDepFunctVsStruct, true);
				_compareStats(tx.getName() + ", comparison of Endothelium-independent structural vs functional CMD",
						comparEndothIndepFunctVsStruct, true);
				_compareStats(tx.getName() + ", comparison of Functional vs Structural CMD", comparFuncVsStruct, true);

			} catch (Exception e) {
				e.printStackTrace();
				throw new StatisticalException(e.getMessage());
			}

		}

		// Now compare the % wave change between rest -> adenosine and rest ->
		// acetylcholine, between CMD / non CMD and subtypes
		StandardTreatment stRest = standardTreatments.stream()
				.filter(tx -> tx.getTreatmentType() == StandardTreatmentType.REST).findFirst().orElse(null);

		if (stRest != null && standardTreatments.size() > 1) {

			for (StandardTreatment stOther : standardTreatments) {
				if (stOther == stRest) {
					continue;
				}

				// rest vs the other treatment
				LinkedHashMap<String, Collection<WIAData[]>> comparCMDvsNonCMD = new LinkedHashMap<String, Collection<WIAData[]>>();
				LinkedHashMap<String, Collection<WIAData[]>> comparEndothDepVsNonCMD = new LinkedHashMap<String, Collection<WIAData[]>>();
				LinkedHashMap<String, Collection<WIAData[]>> comparEndothIndepVsNonCMD = new LinkedHashMap<String, Collection<WIAData[]>>();
				LinkedHashMap<String, Collection<WIAData[]>> comparEndothBothVsNonCMD = new LinkedHashMap<String, Collection<WIAData[]>>();
				LinkedHashMap<String, Collection<WIAData[]>> comparEndothDepIndepNonCMD = new LinkedHashMap<String, Collection<WIAData[]>>();

				LinkedHashMap<String, Collection<WIAData[]>> comparEndothDepVsIndep = new LinkedHashMap<String, Collection<WIAData[]>>();
				LinkedHashMap<String, Collection<WIAData[]>> comparEndothDepFunctVsStruct = new LinkedHashMap<String, Collection<WIAData[]>>();
				LinkedHashMap<String, Collection<WIAData[]>> comparEndothIndepFunctVsStruct = new LinkedHashMap<String, Collection<WIAData[]>>();
				LinkedHashMap<String, Collection<WIAData[]>> comparFuncVsStruct = new LinkedHashMap<String, Collection<WIAData[]>>();

				String name = stRest.getName() + " to " + stOther.getName() + " wave % increase";

				comparCMDvsNonCMD.put(name + " (CMD)",
						_subsetCMDMatched(stRest.getSamples(), stOther.getSamples(), true));
				comparCMDvsNonCMD.put(name + " (Non-CMD)",
						_subsetCMDMatched(stRest.getSamples(), stOther.getSamples(), false));
				comparEndothDepVsNonCMD.put(name + " (Endothelium-dependent CMD)",
						_subsetCMDEndothelialDepMatches(stRest.getSamples(), stOther.getSamples(), TYPE_ENDO_DEP));
				comparEndothDepVsNonCMD.put(name + " (Non-CMD)",
						_subsetCMDMatched(stRest.getSamples(), stOther.getSamples(), false));
				comparEndothIndepVsNonCMD.put(name + " (Endothelium-independent CMD)",
						_subsetCMDEndothelialDepMatches(stRest.getSamples(), stOther.getSamples(), TYPE_ENDO_INDEP));
				comparEndothIndepVsNonCMD.put(name + " (Non-CMD)",
						_subsetCMDMatched(stRest.getSamples(), stOther.getSamples(), false));
				comparEndothBothVsNonCMD.put(name + " (Endothelium-indep and -dep CMD)",
						_subsetCMDEndothelialDepMatches(stRest.getSamples(), stOther.getSamples(), TYPE_ENDO_BOTH));
				comparEndothBothVsNonCMD.put(name + " (Non-CMD)",
						_subsetCMDMatched(stRest.getSamples(), stOther.getSamples(), false));

				comparEndothDepIndepNonCMD.put(name + " (Non-CMD)",
						_subsetCMDMatched(stRest.getSamples(), stOther.getSamples(), false));
				comparEndothDepIndepNonCMD.put(name + " (Only endothelium-dependent CMD)",
						_subsetCMDEndothelialDepMatches(stRest.getSamples(), stOther.getSamples(), TYPE_ENDO_DEP_ONLY));
				comparEndothDepIndepNonCMD.put(name + " (Only endothelium-independent CMD)",
						_subsetCMDEndothelialDepMatches(stRest.getSamples(), stOther.getSamples(),
								TYPE_ENDO_INDEP_ONLY));
				comparEndothDepIndepNonCMD.put(name + " (Both Endothelium-indep and -dep CMD)",
						_subsetCMDEndothelialDepMatches(stRest.getSamples(), stOther.getSamples(), TYPE_ENDO_BOTH));

				comparEndothDepVsIndep.put(name + " (Endothelium-dependent CMD)",
						_subsetCMDEndothelialDepMatches(stRest.getSamples(), stOther.getSamples(), TYPE_ENDO_DEP));
				comparEndothDepVsIndep.put(name + " (Endothelium-independent CMD)",
						_subsetCMDEndothelialDepMatches(stRest.getSamples(), stOther.getSamples(), TYPE_ENDO_INDEP));
				comparEndothDepFunctVsStruct.put(name + " (Endothelium-dependent, structural CMD)",
						_subsetCMDEndothelialDepFuncsMatches(stRest.getSamples(), stOther.getSamples(), true, false));
				comparEndothDepFunctVsStruct.put(name + " (Endothelium-dependent, functional CMD)",
						_subsetCMDEndothelialDepFuncsMatches(stRest.getSamples(), stOther.getSamples(), true, true));
				comparEndothIndepFunctVsStruct.put(name + " (Endothelium-independent, structural CMD)",
						_subsetCMDEndothelialDepFuncsMatches(stRest.getSamples(), stOther.getSamples(), false, false));
				comparEndothIndepFunctVsStruct.put(name + " (Endothelium-independent, functional CMD)",
						_subsetCMDEndothelialDepFuncsMatches(stRest.getSamples(), stOther.getSamples(), false, true));
				comparFuncVsStruct.put(name + " (Functional CMD)",
						_subsetCMDFunctionalMatched(stRest.getSamples(), stOther.getSamples(), true));
				comparFuncVsStruct.put(name + " (Structural CMD)",
						_subsetCMDFunctionalMatched(stRest.getSamples(), stOther.getSamples(), false));
				try {
					compareStatsMultipleTx(name + ", comparison of CMD vs non-CMD", comparCMDvsNonCMD);
					compareStatsMultipleTx(name + ", comparison of only Endothelium-dependent CMD vs non-CMD",
							comparEndothDepVsNonCMD);
					compareStatsMultipleTx(name + ", comparison of only Endothelium-independent CMD vs non-CMD",
							comparEndothIndepVsNonCMD);
					compareStatsMultipleTx(name + ", comparison of both endotypes vs non-CMD",
							comparEndothBothVsNonCMD);
					compareStatsMultipleTx(name + ", comparison of non-CMD, endo-indep, endo-dep, both",
							comparEndothDepVsNonCMD);
					compareStatsMultipleTx(
							name + ", comparison of Endothelium-dependent vs Endodothelium-independent CMD",
							comparEndothDepVsIndep);
					compareStatsMultipleTx(name + ", comparison of Endothelium-dependent structural vs functional CMD",
							comparEndothDepFunctVsStruct);
					compareStatsMultipleTx(
							name + ", comparison of Endothelium-independent structural vs functional CMD",
							comparEndothIndepFunctVsStruct);
					compareStatsMultipleTx(name + ", comparison of Functional vs Structural CMD", comparFuncVsStruct);

				} catch (Exception e) {
					e.printStackTrace();
					throw new StatisticalException(e.getMessage());
				}
			}

		}

	}

	/**
	 * Checks whether all data collections in the provided map are non-empty.
	 *
	 * @param <T>   The type of data in the collections.
	 * @param input A map of group names to data collections.
	 * @return true if every collection contains at least one element, false
	 *         otherwise.
	 */
	private <T> boolean _hasData(LinkedHashMap<String, Collection<T>> input) {
		for (Entry<String, Collection<T>> en : input.entrySet()) {
			if (en.getValue().isEmpty())
				return false;
		}
		return true;
	}

	/**
	 * Collects all {@link WIAData} within the passed collection, according to
	 * whether or not they have CMD as indicated by the parameter. If we cannot
	 * determine if a {@link WIAData} is CMD (due to lack of storage of these
	 * variables), then it defaults to be not included in the subset.
	 * 
	 * @param data       input
	 * @param collectCMD true if should gather only CMD patients
	 * @return collection of {@link WIAData} as above.
	 */
	private Collection<WIAData> _subsetCMD(Collection<WIAData> data, boolean collectCMD) {
		List<WIAData> subset = new ArrayList<WIAData>(data);
		Iterator<WIAData> itr = subset.iterator();
		while (itr.hasNext()) {
			Boolean isCMD = itr.next().isCMD();
			if (isCMD == null || (isCMD != collectCMD)) {
				itr.remove();
			}

		}

		return subset;
	}

	/**
	 * Collects all {@link WIAData} within the passed collection, according to
	 * whether or not they have endothelium- dependent CMD as indicated by the
	 * parameter. If we cannot determine if a {@link WIAData} is endothelial CMD
	 * (due to lack of storage of these variables), then it defaults to be not
	 * included in the subset.
	 * 
	 * @param setting one of TYPE_ENDO_DEP, etc
	 * @return collection of {@link WIAData} as above.
	 */
	private Collection<WIAData> _subsetCMDEndothelialDep(Collection<WIAData> data, int setting) {
		List<WIAData> subset = new ArrayList<WIAData>(data);
		Iterator<WIAData> itr = subset.iterator();
		while (itr.hasNext()) {
			switch (setting) {
			case TYPE_ENDO_DEP:
				Boolean isEndothDepCMD = itr.next().isCMDEndothelialDependent(false);
				if (isEndothDepCMD == null || !isEndothDepCMD) {
					itr.remove();
				}
				break;
			case TYPE_ENDO_DEP_ONLY:
				Boolean isEndothDepOnlyCMD = itr.next().isCMDEndothelialDependent(true);
				if (isEndothDepOnlyCMD == null || !isEndothDepOnlyCMD) {
					itr.remove();
				}
				break;
			case TYPE_ENDO_INDEP:
				Boolean isEndothIndepCMD = itr.next().isCMDEndothelialIndependent(false);
				if (isEndothIndepCMD == null || !isEndothIndepCMD) {
					itr.remove();
				}
				break;
			case TYPE_ENDO_INDEP_ONLY:
				Boolean isEndothIndepOnlyCMD = itr.next().isCMDEndothelialIndependent(true);
				if (isEndothIndepOnlyCMD == null || !isEndothIndepOnlyCMD) {
					itr.remove();
				}
				break;
			case TYPE_ENDO_BOTH:
				WIAData wiaData = itr.next();
				Boolean isEndothDepCMDBoth = wiaData.isCMDEndothelialDependent(false);
				Boolean isEndothIndepCMDBoth = wiaData.isCMDEndothelialIndependent(false);

				if (isEndothDepCMDBoth == null || isEndothIndepCMDBoth == null || !isEndothDepCMDBoth
						|| !isEndothIndepCMDBoth) {
					itr.remove();
				}
				break;
			}

		}

		return subset;
	}

	/**
	 * Collects all {@link WIAData} within the passed collection with functional
	 * disease, according to whether or not they have endothelium- dependent or
	 * -independent CMD as indicated by the parameter, as well as if they have
	 * functional or structural disease. If we cannot determine if a {@link WIAData}
	 * is endothelial CMD (due to lack of storage of these variables), then it
	 * defaults to be not included in the subset.
	 * 
	 * @param data                 input
	 * @param endothelialDependent true if should gather only CMD patients with
	 *                             endothelial disease (other independent)
	 * @param functional           true if should gather only functional disease
	 * @return collection of {@link WIAData} as above.
	 */
	private Collection<WIAData> _subsetCMDEndothelialDepFunc(Collection<WIAData> data, int setting,
			boolean functional) {
		List<WIAData> subset = new ArrayList<WIAData>(data);
		Iterator<WIAData> itr = subset.iterator();
		while (itr.hasNext()) {
			WIAData nextData = itr.next();
			Boolean isFuncStruct = functional ? nextData.isCMDFunctional() : nextData.isCMDStructural();
			if (isFuncStruct == null || !isFuncStruct) {
				itr.remove();
				continue;
			}

			// TODO
			switch (setting) {
			case TYPE_ENDO_DEP:
				Boolean isEndothDepCMD = nextData.isCMDEndothelialDependent(false);
				if (isEndothDepCMD == null || !isEndothDepCMD) {
					itr.remove();
				}
				break;
			case TYPE_ENDO_DEP_ONLY:
				Boolean isEndothDepOnlyCMD = nextData.isCMDEndothelialDependent(true);
				if (isEndothDepOnlyCMD == null || !isEndothDepOnlyCMD) {
					itr.remove();
				}
				break;
			case TYPE_ENDO_INDEP:
				Boolean isEndothIndepCMD = nextData.isCMDEndothelialIndependent(false);
				if (isEndothIndepCMD == null || !isEndothIndepCMD) {
					itr.remove();
				}
				break;
			case TYPE_ENDO_INDEP_ONLY:
				Boolean isEndothIndepOnlyCMD = nextData.isCMDEndothelialIndependent(true);
				if (isEndothIndepOnlyCMD == null || !isEndothIndepOnlyCMD) {
					itr.remove();
				}
				break;
			case TYPE_ENDO_BOTH:
				Boolean isEndothDepCMDBoth = nextData.isCMDEndothelialDependent(false);
				Boolean isEndothIndepCMDBoth = nextData.isCMDEndothelialIndependent(false);

				if (isEndothDepCMDBoth == null || isEndothIndepCMDBoth == null || !isEndothDepCMDBoth
						|| !isEndothIndepCMDBoth) {
					itr.remove();
				}
				break;
			}

		}

		return subset;
	}

	/**
	 * Collects all {@link WIAData} within the passed collection, according to
	 * whether or not they have functional CMD as indicated by the parameter. If we
	 * cannot determine if a {@link WIAData} is functional CMD (due to lack of
	 * storage of these variables), then it defaults to be not included in the
	 * subset.
	 * 
	 * @param data                 input
	 * @param collectFunctionalCMD true if should gather only CMD patients with
	 *                             functional disease
	 * @return collection of {@link WIAData} as above.
	 */
	private Collection<WIAData> _subsetCMDFunctional(Collection<WIAData> data, boolean collectFunctionalCMD) {
		List<WIAData> subset = new ArrayList<WIAData>(data);
		Iterator<WIAData> itr = subset.iterator();
		while (itr.hasNext()) {
			Boolean isFuncStructCMD = collectFunctionalCMD ? itr.next().isCMDFunctional()
					: itr.next().isCMDStructural();
			if (isFuncStructCMD == null || isFuncStructCMD == false) {
				itr.remove();
			}
		}

		return subset;
	}

	/**
	 * Collects pairing of {@link WIAData} between the two passed collections,
	 * according to whether or not they have CMD as indicated by the parameter. If
	 * we cannot determine if a {@link WIAData} is CMD (due to lack of storage of
	 * these variables), then it defaults to be not included in the subset.
	 * 
	 * @param txData1    Baseline comparison {@link WIAData}
	 * @param txData2    Comparator {@link WIAData}
	 * @param collectCMD true if should gather only CMD patients
	 * @return collection of {@link WIAData} as above.
	 */
	private Collection<WIAData[]> _subsetCMDMatched(Collection<WIAData> txData1, Collection<WIAData> txData2,
			boolean collectCMD) {

		List<WIAData[]> subset = new ArrayList<WIAData[]>();

		for (WIAData data : txData1) {
			String filePath = data.getData().getFile().getPath();
			WIAData dataComparator = txData2.stream()
					.filter(wia -> wia.getData().getFile().getPath().equalsIgnoreCase(filePath)).findFirst()
					.orElse(null);
			if (dataComparator == null) {
				// WIAData in first collection is not present in the second collection. SKIP
				continue;
			}
			Boolean isCMD1 = data.isCMD();
			Boolean isCMD2 = data.isCMD();

			if (isCMD1 != isCMD2) {
				// WIAData in first collection has different CMD value than the matching WIAData
				// in the second collection. SKIP
				continue;
			}

			// only need to look at one since they should be equal

			if (isCMD1 != null && isCMD1 == collectCMD) {
				subset.add(new WIAData[] { data, dataComparator });
			}

		}

		return subset;
	}

	/**
	 * Collects pairing of {@link WIAData} between the two passed collections,
	 * according to whether or not they have endothelium-dependent CMD as indicated
	 * by the parameter. If we cannot determine if a {@link WIAData} is endothelial
	 * CMD (due to lack of storage of these variables), then it defaults to be not
	 * included in the subset.
	 * 
	 * @param txData1              Baseline comparison {@link WIAData}
	 * @param txData2              Comparator {@link WIAData}
	 * @param endothelialDependent true if should gather only CMD patients
	 * @return collection of {@link WIAData} as above.
	 */
	private Collection<WIAData[]> _subsetCMDEndothelialDepMatches(Collection<WIAData> txData1,
			Collection<WIAData> txData2, int setting) {

		List<WIAData[]> subset = new ArrayList<WIAData[]>();

		for (WIAData data : txData1) {
			String filePath = data.getData().getFile().getPath();
			WIAData dataComparator = txData2.stream()
					.filter(wia -> wia.getData().getFile().getPath().equalsIgnoreCase(filePath)).findFirst()
					.orElse(null);
			if (dataComparator == null) {
				// WIAData in first collection is not present in the second collection. SKIP
				continue;
			}

			Boolean isCMD1 = null;
			Boolean isCMD2 = null;

			switch (setting) {
			case TYPE_ENDO_DEP:
				isCMD1 = data.isCMDEndothelialDependent(false);
				isCMD2 = dataComparator.isCMDEndothelialDependent(false);

				break;
			case TYPE_ENDO_DEP_ONLY:
				isCMD1 = data.isCMDEndothelialDependent(true);
				isCMD2 = dataComparator.isCMDEndothelialDependent(true);

				break;
			case TYPE_ENDO_INDEP:
				isCMD1 = data.isCMDEndothelialIndependent(false);
				isCMD2 = dataComparator.isCMDEndothelialIndependent(false);

				break;
			case TYPE_ENDO_INDEP_ONLY:
				isCMD1 = data.isCMDEndothelialIndependent(true);
				isCMD2 = dataComparator.isCMDEndothelialIndependent(true);

				break;
			case TYPE_ENDO_BOTH:
				isCMD1 = data.isCMDEndotheliumIndepAndDep();
				isCMD2 = dataComparator.isCMDEndotheliumIndepAndDep();
				break;

			}

			if (isCMD1 != null && isCMD2 != null && isCMD1 && isCMD2) {
				subset.add(new WIAData[] { data, dataComparator });

			}

		}

		return subset;
	}

	/**
	 * Collects pairing of {@link WIAData} between the two passed collections,
	 * according to whether or not they have endothelium-dependent CMD and
	 * functional vs structural disease as indicated by the parameter. If we cannot
	 * determine if a {@link WIAData} is endothelial CMD or structural vs functinoal
	 * CMD (due to lack of storage of these variables), then it defaults to be not
	 * included in the subset.
	 * 
	 * @param txData1              Baseline comparison {@link WIAData}
	 * @param txData2              Comparator {@link WIAData}
	 * @param endothelialDependent true if should gather only CMD patients
	 * @param functional           true if should gather only functional MCD
	 *                             patients
	 * @return collection of {@link WIAData} as above.
	 */
	private Collection<WIAData[]> _subsetCMDEndothelialDepFuncsMatches(Collection<WIAData> txData1,
			Collection<WIAData> txData2, boolean endothelialDependent, boolean functional) {

		List<WIAData[]> subset = new ArrayList<WIAData[]>();

		for (WIAData data : txData1) {
			String filePath = data.getData().getFile().getPath();
			WIAData dataComparator = txData2.stream()
					.filter(wia -> wia.getData().getFile().getPath().equalsIgnoreCase(filePath)).findFirst()
					.orElse(null);
			if (dataComparator == null) {
				// WIAData in first collection is not present in the second collection. SKIP
				continue;
			}

			Boolean endothelialState1 = endothelialDependent ? data.isCMDEndothelialDependent(false)
					: data.isCMDEndothelialIndependent(false);
			Boolean endothelialState2 = endothelialDependent ? dataComparator.isCMDEndothelialDependent(false)
					: dataComparator.isCMDEndothelialIndependent(false);
			Boolean isFuncStruct1 = functional ? data.isCMDFunctional() : data.isCMDStructural();
			Boolean isFuncStruct2 = functional ? dataComparator.isCMDFunctional() : dataComparator.isCMDStructural();

			if (endothelialState1 == null || endothelialState2 == null || isFuncStruct1 == null
					|| isFuncStruct2 == null) {
				// Either endothelial state OR functional / structural was not stored for either
				// baseline or comparator WIAData. SKIP
				continue;
			}

			if (endothelialState1 != endothelialState2 || isFuncStruct1 != isFuncStruct2) {
				// WIAData in first collection has different endothelial or
				// structural/functional value than the matching WIAData in the second
				// collection. SKIP
				continue;
			}

			// only need to look at one since they should be equal

			if (endothelialState1 == true && isFuncStruct1 == true) {
				subset.add(new WIAData[] { data, dataComparator });
			}

		}

		return subset;
	}

	/**
	 * Collects pairing of {@link WIAData} between the two passed collections,
	 * according to whether or not they have functional CMD as indicated by the
	 * parameter. If we cannot determine if a {@link WIAData} is functiona or
	 * structural CMD (due to lack of storage of these variables), then it defaults
	 * to be not included in the subset.
	 * 
	 * @param txData1              Baseline comparison {@link WIAData}
	 * @param txData2              Comparator {@link WIAData}
	 * @param collectFunctionalCMD true if should gather only functional CMD
	 *                             patients, otherwise structural CMD patients
	 * @return collection of {@link WIAData} as above.
	 */
	private Collection<WIAData[]> _subsetCMDFunctionalMatched(Collection<WIAData> txData1, Collection<WIAData> txData2,
			boolean collectFunctionalCMD) {

		List<WIAData[]> subset = new ArrayList<WIAData[]>();

		for (WIAData data : txData1) {
			String filePath = data.getData().getFile().getPath();
			WIAData dataComparator = txData2.stream()
					.filter(wia -> wia.getData().getFile().getPath().equalsIgnoreCase(filePath)).findFirst()
					.orElse(null);
			if (dataComparator == null) {
				// WIAData in first collection is not present in the second collection. SKIP
				continue;
			}
			Boolean isCMDFuncStruct1 = collectFunctionalCMD ? data.isCMDFunctional() : data.isCMDStructural();
			Boolean isCMDFuncStruct2 = collectFunctionalCMD ? dataComparator.isCMDFunctional()
					: dataComparator.isCMDStructural();

			if (isCMDFuncStruct1 == null || isCMDFuncStruct2 == null) {
				// Either functional / structural was not stored for either baseline or
				// comparator WIAData. SKIP
				continue;
			}
			if (isCMDFuncStruct1 != isCMDFuncStruct2) {
				// WIAData in first collection has different CMD value than the matching WIAData
				// in the second collection. SKIP
				continue;
			}

			// only need to look at one since they should be equal

			if (isCMDFuncStruct1 == true) {
				subset.add(new WIAData[] { data, dataComparator });
			}

		}

		return subset;
	}
	// TODO: other subset builders

	/**
	 * Tries to add calculations for CFR, hMR, % increase in flow with ACh, CMD,
	 * Endoithelium-dependent CMD, and Functional (vs structural) CMD.
	 */
	public void addCalculatedFields() {
		final StandardTreatment restTreatment = standardTreatments.stream()
				.filter(tx -> tx.getTreatmentType() == StandardTreatmentType.REST).findFirst().orElse(null);
		final StandardTreatment adenoTreatment = standardTreatments.stream()
				.filter(tx -> tx.getTreatmentType() == StandardTreatmentType.ADENOSINE).findFirst().orElse(null);
		final StandardTreatment achTreatment = standardTreatments.stream()
				.filter(tx -> tx.getTreatmentType() == StandardTreatmentType.ACETYLCHOLINE).findFirst().orElse(null);

		Map<String, Double> hMRCalculations = new HashMap<String, Double>();
		Map<String, Double> cfrCalculations = new HashMap<String, Double>();
		Map<String, Double> achFlowIncCalculations = new HashMap<String, Double>();

		if (adenoTreatment != null) {

			for (WIAData adenoData : adenoTreatment.getSamples()) {

				hMRCalculations.put(adenoData.getFileName(),
						adenoData.getAvgPressure(true) / adenoData.getAvgFlow(true));

			}

			if (restTreatment != null) {
				for (WIAData adenoData : adenoTreatment.getSamples()) {
					String adenoDataName = adenoData.getFileName();
					WIAData restData = restTreatment.getSamples().stream()
							.filter(dat -> dat.getFileName().equals(adenoDataName)).findFirst().orElse(null);
					if (restData == null) {
						continue;
					}
					cfrCalculations.put(adenoData.getFileName(),
							adenoData.getAvgFlow(false) / restData.getAvgFlow(false));

				}
			}

		}

		if (restTreatment != null && achTreatment != null) {
			for (WIAData achData : achTreatment.getSamples()) {
				String achDataName = achData.getFileName();
				WIAData restData = restTreatment.getSamples().stream()
						.filter(dat -> dat.getFileName().equals(achDataName)).findFirst().orElse(null);

				if (restData == null) {
					continue;
				}

				// we have the matching ACh (achData) + rest (restData) conditions for a same
				// patient
				double achVelocity = achData.getAvgFlow(false);
				Double achDiameter = achData.getVesselDiameter();
				double restVelocity = restData.getAvgFlow(false);
				Double restDiameter = restData.getVesselDiameter();

				// If diameters are missing and not already skipping, prompt the user
				if (!skipDiameters && (achDiameter == null || restDiameter == null)) {
					DoubleResponse response = promptTwoDoubles(
							"<htmL>Enter vessel diameter (i.e. obtained in mid-LAD at end diastole) to calculate volumetric flow for:<br><br>"
									+ restData.getSerializeFileSource() + "</html>",
							"Resting diameter (mm):", restDiameter, "Acetylcholine diameter (mm):", achDiameter,
							"Skip and use linear velocity instead", null);

					if (response.skip) {
						skipDiameters = true;
					} else {
						restDiameter = (response.double1 != null) ? response.double1 : restDiameter;
						achDiameter = (response.double2 != null) ? response.double2 : achDiameter;
					}
				}

				// Decide which calculation to use
				if (skipDiameters || achDiameter == null || restDiameter == null) {
					// Use velocity-based calculation
					achFlowIncCalculations.put(achData.getFileName(),
							((achVelocity - restVelocity) / restVelocity) * 100);
				} else {
					// Use diameter-based flow calculation
					double achFlow = 0.5 * achVelocity * (Math.PI * Math.pow(achDiameter / 10.0, 2) / 4.0);
					double restFlow = 0.5 * restVelocity * (Math.PI * Math.pow(restDiameter / 10.0, 2) / 4.0);
					achFlowIncCalculations.put(achData.getFileName(), ((achFlow - restFlow) / restFlow) * 100);
				}

			}
		}

		// update values in all of the WIAData
		for (WIAData data : this.wiaData) {
			Double hMR = hMRCalculations.get(data.getFileName());
			Double cfr = cfrCalculations.get(data.getFileName());
			Double achFlowInc = achFlowIncCalculations.get(data.getFileName());

			if (hMR != null) {
				data.setHMR(hMR);
			}
			if (cfr != null) {
				data.setCFR(cfr);
			}
			if (achFlowInc != null) {
				data.setPercIncACh(achFlowInc);
			}

		}
	}

	/**
	 * Gets the statistics for comparison groups. Notably, the {@link #runStats()}
	 * method must have been called already
	 * 
	 * @return statistical comparisons, if have been performed. This list is
	 *         immutable.
	 */
	public List<StatisticalComparison> getStats() {
		return Collections.unmodifiableList(this.statsComparisons);
	}

	/**
	 * Saves the current statistics summary to a spreadsheet file. Prompts before
	 * overwriting existing files.
	 * 
	 * @param fileToSave target file path
	 * @throws IOException if there are issues with writing the data
	 */
	public void save(File fileToSave) throws IOException {
		if (fileToSave == null)
			throw new IllegalArgumentException("File to save shouldn't be null");
		if (fileToSave.exists()) {
			if (!Utils.confirmAction("Confirm Overwrite",
					"The file " + fileToSave.getName() + " already exists at " + fileToSave.getPath() + ". Overwrite?",
					null))
				return;
		}
		if (statsComparisons.isEmpty())
			return;

		SheetWriter writer = new SheetWriter();
		writer.newSheet("Summary");

		for (StatisticalComparison sc : this.statsComparisons) {
			sc.write(writer);

		}

		writer.saveFile(fileToSave);

	}

	/**
	 * @return All data within this stats object, in a 2D array with rows and
	 *         columns in the array corresponding to the rows and columns in a CSV /
	 *         excel file. the 2D array will not have null values.
	 */
	public String[][] getDataArray() {
		InsensitiveNonDupList headers = new InsensitiveNonDupList();

		List<LinkedHashMap<String, String>> allData = new ArrayList<LinkedHashMap<String, String>>();

		for (WIAData data : wiaData) {
			LinkedHashMap<String, String> dataValues = data.toPrintableMap();
			allData.add(dataValues);
			for (Entry<String, String> dataValuesEn : dataValues.entrySet()) {
				headers.add(dataValuesEn.getKey());
			}
		}

		List<String[]> rows = new ArrayList<String[]>();
		rows.add(headers.toArray(new String[0]));
		for (LinkedHashMap<String, String> data : allData) {
			List<String> row = new ArrayList<String>();
			for (String header : headers) {
				String value = Utils.getCaseInsensitive(data, header);
				if (value == null)
					value = "";
				row.add(value);
			}
			rows.add(row.toArray(new String[0]));
		}

		return rows.toArray(new String[0][]);

	}

	/**
	 * Performs comparison between the waves (including wave groups) for a group of
	 * {@link WIAData}, stores it
	 * 
	 * @param nameOfComparison name of comparison
	 * @param data             data to compare
	 * @throws IllegalArgumentException if there are too few samples.
	 */
	private void _compareWavesWithinGroup(String nameOfComparison, Collection<WIAData> data) {

		StatisticalComparison comparison = new StatisticalComparison(nameOfComparison);

		int numWaves = standardWaves.size();
		for (int i = 0; i < numWaves - 1; i++) {
			for (int j = i + 1; j < numWaves; j++) {
				StandardWave sw1 = standardWaves.get(i);
				StandardWave sw2 = standardWaves.get(j);
				Outcome waveSepCum = new Outcome(sw1.getName() + " vs " + sw2.getName() + " Cumul Intensity");
				Outcome wavePeak = new Outcome(sw1.getName() + " vs " + sw2.getName() + " Peak Intensity");
				DataCollection sw1WaveSepCumDC = new DataCollection(sw1.getName() + " Cumul Intensity",
						DataType.CONTINUOUS, false);
				DataCollection sw1WavePeakDC = new DataCollection(sw1.getName() + " Peak Intensity",
						DataType.CONTINUOUS, false);
				DataCollection sw2WaveSepCumDC = new DataCollection(sw2.getName() + " Cumul Intensity",
						DataType.CONTINUOUS, false);
				DataCollection sw2WavePeakDC = new DataCollection(sw2.getName() + " Peak Intensity",
						DataType.CONTINUOUS, false);

				for (Entry<WIAData, Wave> wavEnSW1 : sw1.getWaves(data).entrySet()) {
					sw1WaveSepCumDC.addValue(Math.abs(wavEnSW1.getValue().getCumulativeIntensity()));
					sw1WavePeakDC.addValue(Math.abs(wavEnSW1.getValue().getPeak()));
				}
				for (Entry<WIAData, Wave> wavEnSW2 : sw2.getWaves(data).entrySet()) {
					sw2WaveSepCumDC.addValue(Math.abs(wavEnSW2.getValue().getCumulativeIntensity()));
					sw2WavePeakDC.addValue(Math.abs(wavEnSW2.getValue().getPeak()));
				}
				waveSepCum.addDataSet(sw1.getName(), sw1WaveSepCumDC);
				waveSepCum.addDataSet(sw2.getName(), sw2WaveSepCumDC);
				wavePeak.addDataSet(sw1.getName(), sw1WavePeakDC);
				wavePeak.addDataSet(sw2.getName(), sw2WavePeakDC);

				comparison.addOutcome(waveSepCum, wavePeak);
			}
		}

		int numSWGs = this.standardWaveGroups.size();

		for (int i = 0; i < numSWGs; i++) {
			StandardWaveGrouping swg1 = this.standardWaveGroups.get(i);

			// compare with other groups
			for (int j = i + 1; j < numSWGs; j++) {
				StandardWaveGrouping swg2 = this.standardWaveGroups.get(i);

				Outcome waveSepCum = new Outcome(swg1.getName() + " vs " + swg2.getName() + " Cumul Intensity");
				DataCollection swg1WaveSepCumDC = new DataCollection(swg1.getName() + " Cumul Intensity",
						DataType.CONTINUOUS, false);
				DataCollection swg2WaveSepCumDC = new DataCollection(swg2.getName() + " Cumul Intensity",
						DataType.CONTINUOUS, false);

				for (Entry<WIAData, List<Wave>> wavEnSW1 : swg1.getWaves(data).entrySet()) {

					double waveSepCumVal = 0;

					for (Wave wave : wavEnSW1.getValue()) {
						waveSepCumVal += Math.abs(wave.getCumulativeIntensity());
					}
					swg1WaveSepCumDC.addValue(Math.abs(waveSepCumVal));
				}
				for (Entry<WIAData, List<Wave>> wavEnSW2 : swg2.getWaves(data).entrySet()) {

					double waveSepCumVal = 0;

					for (Wave wave : wavEnSW2.getValue()) {
						waveSepCumVal += Math.abs(wave.getCumulativeIntensity());
					}
					swg2WaveSepCumDC.addValue(Math.abs(waveSepCumVal));
				}

				waveSepCum.addDataSet(swg1.getName(), swg1WaveSepCumDC);
				waveSepCum.addDataSet(swg2.getName(), swg2WaveSepCumDC);
				comparison.addOutcome(waveSepCum);

			}

			for (StandardWave swOther : this.standardWaves) {
				if (!swg1.getStandardWaves().contains(swOther)) {

					Outcome waveSepCum = new Outcome(swg1.getName() + " vs " + swOther.getName() + " Cumul Intensity");
					DataCollection swg1WaveSepCumDC = new DataCollection(swg1.getName() + " Cumul Intensity",
							DataType.CONTINUOUS, false);
					DataCollection swOtherWaveSepCumDC = new DataCollection(swOther.getName() + " Cumul Intensity",
							DataType.CONTINUOUS, false);

					for (Entry<WIAData, List<Wave>> wavEnSW1 : swg1.getWaves(data).entrySet()) {

						double waveSepCumVal = 0;

						for (Wave wave : wavEnSW1.getValue()) {
							waveSepCumVal += Math.abs(wave.getCumulativeIntensity());
						}
						swg1WaveSepCumDC.addValue(Math.abs(waveSepCumVal));
					}
					for (Entry<WIAData, Wave> wavEnSW2 : swOther.getWaves(data).entrySet()) {
						swOtherWaveSepCumDC.addValue(Math.abs(wavEnSW2.getValue().getCumulativeIntensity()));
					}

					waveSepCum.addDataSet(swg1.getName(), swg1WaveSepCumDC);
					waveSepCum.addDataSet(swOther.getName(), swOtherWaveSepCumDC);
					comparison.addOutcome(waveSepCum);

				}
			}
		}

		if (this.standardWaves.size() > 2) {
			// Compare all waves
			Outcome waveSepCum = new Outcome("All Waves Cumul Intensity");
			Outcome wavePeak = new Outcome("All Waves Peak Intensity");

			for (StandardWave sw : this.standardWaves) {

				DataCollection swWaveSepCumDC = new DataCollection(sw.getName() + " Cumul Intensity",
						DataType.CONTINUOUS, false);
				DataCollection swWavePeakDC = new DataCollection(sw.getName() + " Peak Intensity", DataType.CONTINUOUS,
						false);

				for (Entry<WIAData, Wave> wavEnSW1 : sw.getWaves(data).entrySet()) {
					swWaveSepCumDC.addValue(Math.abs(wavEnSW1.getValue().getCumulativeIntensity()));
					swWavePeakDC.addValue(Math.abs(wavEnSW1.getValue().getPeak()));
				}
				waveSepCum.addDataSet(sw.getName(), swWaveSepCumDC);
				wavePeak.addDataSet(sw.getName(), swWavePeakDC);
			}
			comparison.addOutcome(waveSepCum, wavePeak);

			if (this.standardWaveGroups.size() > 0) {
				// compare all waves including wave groups

				Outcome waveSepCumGr = new Outcome("All Waves + Groups Cumul Intensity");

				List<StandardWave> swlist = new ArrayList<StandardWave>(this.standardWaves);
				for (StandardWaveGrouping swg : this.standardWaveGroups) {
					swlist.removeAll(swg.getStandardWaves());

					DataCollection swWaveSepCumDC = new DataCollection(swg.getName() + " Cumul Intensity",
							DataType.CONTINUOUS, false);

					for (Entry<WIAData, List<Wave>> wavEnSW1 : swg.getWaves(data).entrySet()) {

						double waveSepCumVal = 0;

						for (Wave wave : wavEnSW1.getValue()) {
							waveSepCumVal += Math.abs(wave.getCumulativeIntensity());
						}
						swWaveSepCumDC.addValue(waveSepCumVal);
					}
					waveSepCumGr.addDataSet(swg.getName(), swWaveSepCumDC);
				}

				for (StandardWave sw : this.standardWaves) {

					DataCollection swWaveSepCumDC = new DataCollection(sw.getName() + " Cumul Intensity",
							DataType.CONTINUOUS, false);

					for (Entry<WIAData, Wave> wavEnSW1 : sw.getWaves(data).entrySet()) {
						swWaveSepCumDC.addValue(Math.abs(wavEnSW1.getValue().getCumulativeIntensity()));
					}
					waveSepCumGr.addDataSet(sw.getName(), swWaveSepCumDC);
				}
				comparison.addOutcome(waveSepCumGr);

			}

		}

		statsComparisons.add(comparison);

	}

	/**
	 * Compares two or more groups of {@link WIAData}. If there are less than two
	 * {@link WIAData} in either comparison group, then statistics will not be run
	 * 
	 * @param nameOfComparison The name of the comparison being run
	 * @param groups           2+ groups to compare. Key is the group name, value is
	 *                         the {@link WIAData} files associated
	 * @throws IllegalArgumentException if group numbers is < 2.
	 */
	private void _compareStats(String nameOfComparison, LinkedHashMap<String, Collection<WIAData>> groups,
			boolean includeCMDCalc) {

		if (!_hasData(groups)) {
			System.out.println("Called " + nameOfComparison);
			return;
		}

		if (groups.size() < 2) {
			throw new IllegalArgumentException("Too few groups for comparison '" + nameOfComparison + "'");
		}

		StatisticalComparison comparison = new StatisticalComparison(nameOfComparison);

		Outcome sumWaveSpeed = getOutcomeByFunctionDouble("Single Point Wave Speed (C) (m/s)", groups,
				DataType.CONTINUOUS, false, (wiadata) -> {
					return wiadata.getWaveSpeed();
				});

		Outcome avgPressure = getOutcomeByFunctionDouble("Avg pressure in Cycle (mmHg)", groups, DataType.CONTINUOUS,
				false, (wiadata) -> {
					return wiadata.getAvgPressure(true);
				});
		Outcome maxPressure = getOutcomeByFunctionDouble("Max pressure in Cycle (mmHg)", groups, DataType.CONTINUOUS,
				false, (wiadata) -> {
					return wiadata.getMaxPressure(true);
				});
		Outcome minPressure = getOutcomeByFunctionDouble("Min pressure in Cycle (mmHg)", groups, DataType.CONTINUOUS,
				false, (wiadata) -> {
					return wiadata.getMinPressure(true);
				});

		Outcome avgFlow = getOutcomeByFunctionDouble("Avg doppler velocity in Cycle (cm/s)", groups,
				DataType.CONTINUOUS, false, (wiadata) -> {
					return Math.abs(wiadata.getAvgFlow(true));
				});
		Outcome maxFlow = getOutcomeByFunctionDouble("Max doppler velocity in Cycle (cm/s)", groups,
				DataType.CONTINUOUS, false, (wiadata) -> {
					return wiadata.getMaxFlow(true);
				});
		Outcome minFlow = getOutcomeByFunctionDouble("Min doppler velocity in Cycle (cm/s)", groups,
				DataType.CONTINUOUS, false, (wiadata) -> {
					return wiadata.getMinFlow(true);
				});

		Outcome resist = getOutcomeByFunctionDouble("Resistance (mmHg/cm/s)", groups, DataType.CONTINUOUS, false,
				(wiadata) -> {
					return Math.abs(wiadata.getResistanceOverall());
				});
		Outcome cumNet = getOutcomeByFunctionDouble("Cumulative Net Intensity", groups, DataType.CONTINUOUS, false,
				(wiadata) -> {
					return wiadata.getCumWINet();
				});
		Outcome cumForw = getOutcomeByFunctionDouble("Cumulative Sep Forward Intensity", groups, DataType.CONTINUOUS,
				false, (wiadata) -> {
					return Math.abs(wiadata.getCumWIForward());
				});
		Outcome cumBack = getOutcomeByFunctionDouble("Cumulative Sep Backward Intensity", groups, DataType.CONTINUOUS,
				false, (wiadata) -> {
					return Math.abs(wiadata.getCumWIForward());
				});

		comparison.addOutcome(sumWaveSpeed, avgPressure, maxPressure, minPressure, avgFlow, maxFlow, minFlow, resist,
				cumNet, cumForw, cumBack);

		// add calculated fields
		boolean hasCMDData = true;
		for (Collection<WIAData> datas : groups.values()) {
			for (WIAData data : datas) {
				if (data.getCFR() == null || data.getHMR() == null || data.getPercIncACh() == null) {
					hasCMDData = false;
					break;
				}
			}
		}
		if (hasCMDData && includeCMDCalc) {
			Outcome cfr = getOutcomeByFunctionDouble("CFR", groups, DataType.CONTINUOUS, false, (wiadata) -> {
				return wiadata.getCFR();
			});
			Outcome hmr = getOutcomeByFunctionDouble("HMR (mmHg cm s)", groups, DataType.CONTINUOUS, false,
					(wiadata) -> {
						return wiadata.getHMR();
					});
			Outcome percIncACh = getOutcomeByFunctionDouble("Flow Increase with ACh (%)", groups, DataType.CONTINUOUS,
					false, (wiadata) -> {
						return wiadata.getPercIncACh();
					});

			Outcome CMD = getOutcomeByFunctionBoolean("CMD", groups, DataType.DISCRETE_BOOLEAN, false, (wiadata) -> {
				return wiadata.isCMD();
			});
			Outcome EndothDep = getOutcomeByFunctionBoolean("Endothelium-dependent CMD", groups,
					DataType.DISCRETE_BOOLEAN, false, (wiadata) -> {
						return wiadata.isCMDEndothelialDependent(false);
					});
			Outcome EndothIndep = getOutcomeByFunctionBoolean("Endothelium-independent CMD", groups,
					DataType.DISCRETE_BOOLEAN, false, (wiadata) -> {
						return wiadata.isCMDEndothelialIndependent(false);
					});
			Outcome EndothDepFunc = getOutcomeByFunctionBoolean("Endothelium-dependent CMD, functional subtype", groups,
					DataType.DISCRETE_BOOLEAN, false, (wiadata) -> {
						return wiadata.isCMDEndothelialDependent(false) && wiadata.isCMDFunctional();
					});
			Outcome EndothDepStruct = getOutcomeByFunctionBoolean("Endothelium-dependent CMD, strutural subtype",
					groups, DataType.DISCRETE_BOOLEAN, false, (wiadata) -> {
						return wiadata.isCMDEndothelialDependent(false) && wiadata.isCMDStructural();
					});
			Outcome EndothIndepFunc = getOutcomeByFunctionBoolean("Endothelium-independent CMD, functional subtype",
					groups, DataType.DISCRETE_BOOLEAN, false, (wiadata) -> {
						return wiadata.isCMDEndothelialIndependent(false) && wiadata.isCMDFunctional();
					});
			Outcome EndothIndepStruct = getOutcomeByFunctionBoolean("Endothelium-independent CMD, structural subtype",
					groups, DataType.DISCRETE_BOOLEAN, false, (wiadata) -> {
						return wiadata.isCMDEndothelialIndependent(false) && wiadata.isCMDStructural();
					});
			Outcome Functional = getOutcomeByFunctionBoolean("Functional CMD", groups, DataType.DISCRETE_BOOLEAN, false,
					(wiadata) -> {
						return wiadata.isCMDFunctional();
					});
			Outcome Structural = getOutcomeByFunctionBoolean("Structural CMD", groups, DataType.DISCRETE_BOOLEAN, false,
					(wiadata) -> {
						return wiadata.isCMDStructural();
					});

			comparison.addOutcome(cfr, hmr, percIncACh, CMD, EndothDep, EndothIndep, EndothDepFunc, EndothDepStruct,
					EndothIndepFunc, EndothIndepStruct, Functional, Structural);

		}

		for (StandardWaveGrouping swg : standardWaveGroups) {

			Outcome waveSepCum = new Outcome(swg.getName() + " Cumul Intensity");
			Outcome waveSepCumRatio = swg.isProximal() != null
					? new Outcome(swg.getName() + " Cumul Intensity / Total " + (swg.isProximal() ? "Forw" : "Back")
							+ " Intensity")
					: null;

			for (Entry<String, Collection<WIAData>> group : groups.entrySet()) {

				DataCollection waveSepCumDC = new DataCollection(
						group.getKey() + " " + swg.getName() + " Cumul Intensity", DataType.CONTINUOUS, false);
				DataCollection waveSepCumRatioDC = swg.isProximal() != null
						? new DataCollection(swg.getName() + " Cumul Intensity / Total "
								+ (swg.isProximal() ? "Forw" : "Back") + " Intensity", DataType.CONTINUOUS, true)
						: null;

				for (Entry<WIAData, List<Wave>> wavesEn : swg.getWaves(group.getValue()).entrySet()) {

					if (wavesEn.getValue().isEmpty())
						continue;

					double waveSepCumVal = 0;

					for (Wave wave : wavesEn.getValue()) {
						waveSepCumVal += Math.abs(wave.getCumulativeIntensity());
					}
					waveSepCumDC.addValue(waveSepCumVal);

					// this is null if the grouped waves include both proximal and distal
					if (waveSepCumRatio != null) {
						double cumIntensSep = swg.isProximal() ? wavesEn.getKey().getCumWIForward()
								: wavesEn.getKey().getCumWIBackward();
						waveSepCumRatioDC.addValue(Math.abs(waveSepCumVal / cumIntensSep));

					}

				}

				waveSepCum.addDataSet(group.getKey(), waveSepCumDC);
				if (waveSepCumRatio != null) {
					waveSepCumRatio.addDataSet(group.getKey(), waveSepCumRatioDC);

				}

			}

			comparison.addOutcome(waveSepCum);
			if (waveSepCumRatio != null) {
				comparison.addOutcome(waveSepCumRatio);
			}

		}
		for (StandardWave sw : standardWaves) {

			Outcome waveSepCum = new Outcome(sw.getName() + " Cumul Intensity");
			Outcome waveSepCumRatio = new Outcome(
					sw.getName() + " Cumul Intensity / Total " + (sw.isProximal() ? "Forw" : "Back") + " Intensity");

			for (Entry<String, Collection<WIAData>> group : groups.entrySet()) {
				DataCollection waveSepCumDC = new DataCollection(
						group.getKey() + " " + sw.getName() + " Cumul Intensity", DataType.CONTINUOUS, false);

				DataCollection waveSepCumRatioDC = new DataCollection(
						sw.getName() + " Cumul Intensity / Total " + (sw.isProximal() ? "Forw" : "Back") + " Intensity",
						DataType.CONTINUOUS, true);

				for (Entry<WIAData, Wave> waveEn : sw.getWaves(group.getValue()).entrySet()) {

					double cumIntensWave = waveEn.getValue().getCumulativeIntensity();
					double cumIntensSep = sw.isProximal() ? waveEn.getKey().getCumWIForward()
							: waveEn.getKey().getCumWIBackward();

					waveSepCumDC.addValue(Math.abs(cumIntensWave));
					waveSepCumRatioDC.addValue(Math.abs(cumIntensWave / cumIntensSep));

				}

				waveSepCum.addDataSet(group.getKey(), waveSepCumDC);
				waveSepCumRatio.addDataSet(group.getKey(), waveSepCumRatioDC);

			}

			comparison.addOutcome(waveSepCum);
			comparison.addOutcome(waveSepCumRatio);

		}

		statsComparisons.add(comparison);
	}

	private void compareStatsMultipleTx(String nameOfComparison, LinkedHashMap<String, Collection<WIAData[]>> groups) {

		if (!_hasData(groups)) {
			return;
		}

		StatisticalComparison comparison = new StatisticalComparison(nameOfComparison);

		for (StandardWaveGrouping swg : standardWaveGroups) {

			Outcome waveSepCum = new Outcome(swg.getName() + " Cumul Intensity % increase (baseline-comparison)");

			for (Entry<String, Collection<WIAData[]>> group : groups.entrySet()) {

				DataCollection waveSepCumDC = new DataCollection(
						group.getKey() + " " + swg.getName() + " Cumul Intensity", DataType.CONTINUOUS, false);

				List<WIAData>[] arrays = _separateWIAArray(group.getValue());
				List<WIAData> baselineTxWIAData = arrays[0];
				Map<WIAData, List<Wave>> baselineTxWaves = swg.getWaves(baselineTxWIAData);
				List<WIAData> comparisonTxWIAData = arrays[1];
				Map<WIAData, List<Wave>> comparisonTxWaves = swg.getWaves(comparisonTxWIAData);

				for (int counter = 0; counter < baselineTxWIAData.size(); counter++) {

					List<Wave> wavesBase = baselineTxWaves.get(baselineTxWIAData.get(counter));
					List<Wave> wavesCompar = comparisonTxWaves.get(comparisonTxWIAData.get(counter));

					if (wavesBase.isEmpty() || wavesCompar.isEmpty())
						continue;

					double waveSepCumValBase = 0;
					double waveSepCumValCompar = 0;

					for (Wave wave : wavesBase) {
						waveSepCumValBase += Math.abs(wave.getCumulativeIntensity());
					}

					for (Wave wave : wavesCompar) {
						waveSepCumValCompar += Math.abs(wave.getCumulativeIntensity());
					}

					double percIncrease = ((waveSepCumValCompar - waveSepCumValBase) / waveSepCumValBase) * 100.0;
					waveSepCumDC.addValue(percIncrease);

				}

				waveSepCum.addDataSet(group.getKey(), waveSepCumDC);

			}

			comparison.addOutcome(waveSepCum);

		}
		for (StandardWave sw : standardWaves) {

			Outcome waveSepCum = new Outcome(sw.getName() + " Cumul Intensity % increase (baseline-comparison)");

			for (Entry<String, Collection<WIAData[]>> group : groups.entrySet()) {
				DataCollection waveSepCumDC = new DataCollection(
						group.getKey() + " " + sw.getName() + " Cumul Intensity", DataType.CONTINUOUS, false);

				List<WIAData>[] arrays = _separateWIAArray(group.getValue());
				List<WIAData> baselineTxWIAData = arrays[0];
				Map<WIAData, Wave> baselineTxWaves = sw.getWaves(baselineTxWIAData);
				List<WIAData> comparisonTxWIAData = arrays[1];
				Map<WIAData, Wave> comparisonTxWaves = sw.getWaves(comparisonTxWIAData);

				for (int counter = 0; counter < baselineTxWIAData.size(); counter++) {
					double waveIntensBase = baselineTxWaves.get(baselineTxWIAData.get(counter))
							.getCumulativeIntensity();

					double waveIntensTx = comparisonTxWaves.get(comparisonTxWIAData.get(counter))
							.getCumulativeIntensity();

					double percIncrease = ((waveIntensTx - waveIntensBase) / waveIntensBase) * 100.0;
					waveSepCumDC.addValue(percIncrease);

				}

				waveSepCum.addDataSet(group.getKey(), waveSepCumDC);

			}

			comparison.addOutcome(waveSepCum);

		}

		this.statsComparisons.add(comparison);
	}

	@SuppressWarnings("unchecked")
	private List<WIAData>[] _separateWIAArray(Collection<WIAData[]> input) {
		List<WIAData> firstElements = new ArrayList<>();
		List<WIAData> secondElements = new ArrayList<>();

		// Iterate over each array in the input collection
		for (WIAData[] array : input) {
			if (array.length == 2) { // Ensure each array has exactly 2 elements
				firstElements.add(array[0]); // Add the first element to firstElements list
				secondElements.add(array[1]); // Add the second element to secondElements list
			} else {
				throw new IllegalArgumentException("Each array must be of size 2");
			}
		}

		// Return an array containing both lists
		return new List[] { firstElements, secondElements };
	}

	/**
	 * Creates the outcome with specified name.
	 * 
	 * @param name     Name of the outcome
	 * @param data     Data for the outcome. 2+ groups to compare. Key is the group
	 *                 name, value is the {@link WIAData} files associated
	 * @param dataType
	 * @param func     Function to be run in order to obtain data for this outcome
	 * @return new {@link Outcome}
	 */
	private Outcome getOutcomeByFunctionDouble(String name, LinkedHashMap<String, Collection<WIAData>> data,
			DataType dataType, boolean perc, FieldExtractorFunctionDouble func) {

		Outcome outcome = new Outcome(name);

		// Key = comparison group; value = data functions
		for (Entry<String, Collection<WIAData>> entr : data.entrySet()) {
			DataCollection col = new DataCollection(entr.getKey() + " " + name, dataType, perc);
			entr.getValue().stream().forEach(dat -> col.addValue(func.getDataPoint(dat)));
			outcome.addDataSet(entr.getKey(), col);
		}

		return outcome;

	}

	/**
	 * Creates the outcome with specified name.
	 * 
	 * @param name     Name of the outcome
	 * @param data     Data for the outcome. 2+ groups to compare. Key is the group
	 *                 name, value is the {@link WIAData} files associated
	 * @param dataType
	 * @param func     Function to be run in order to obtain data for this outcome
	 * @return new {@link Outcome}
	 */
	private Outcome getOutcomeByFunctionBoolean(String name, LinkedHashMap<String, Collection<WIAData>> data,
			DataType dataType, boolean perc, FieldExtractorFunctionBoolean func) {

		Outcome outcome = new Outcome(name);

		// Key = comparison group; value = data functions
		for (Entry<String, Collection<WIAData>> entr : data.entrySet()) {
			DataCollection col = new DataCollection(entr.getKey() + " " + name, dataType, perc);
			entr.getValue().stream().forEach(dat -> col.addValue(func.getDataPoint(dat)));
			outcome.addDataSet(entr.getKey(), col);
		}

		return outcome;

	}

	/**
	 * A simple data structure to hold the response from the
	 * {@link Utils#promptTwoDoubles(String, String, Double, String, Double, String, Component)}
	 * method.
	 */
	private static class DoubleResponse {
		/** First double prompted */
		public Double double1;
		/** Second double prompted */

		public Double double2;
		public boolean skip;
	}

	/**
	 * Prompts the user for two double values with customizable input field labels
	 * and default values. The prompt allows the user to input values, skip the
	 * request, or check a box to ignore future prompts.
	 *
	 * @param msg              The main message displayed at the top of the dialog.
	 * @param firstMsgPrompt   The label for the first input field.
	 * @param firstMsgDefault  The default value prefilled in the first input field
	 *                         (nullable).
	 * @param secondMsgPrompt  The label for the second input field.
	 * @param secondMsgDefault The default value prefilled in the second input field
	 *                         (nullable).
	 * @param skipMessage      The message for the "Skip" button, allowing the user
	 *                         to bypass input.
	 * @param parent           The parent component for positioning the dialog
	 *                         (nullable).
	 * @return A {@link DoubleResponse} object containing the two entered double
	 *         values or {@code null} if skipped.
	 */
	private static DoubleResponse promptTwoDoubles(String msg, String firstMsgPrompt, Double firstMsgDefault,
			String secondMsgPrompt, Double secondMsgDefault, String skipMessage, Component parent) {

		// Create UI components
		JPanel panel = new JPanel();
		GroupLayout layout = new GroupLayout(panel);
		panel.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		JCLabel promptLabel = new JCLabel(
				"<html><div style='text-align: center; width: 300px; '>" + msg + "</div></html>",
				JCLabel.LABEL_SUB_SUBTITLE);

		JLabel spaceLabel = new JLabel(" "); // Spacer for visual separation

		JCLabel label1 = new JCLabel(firstMsgPrompt, JCLabel.LABEL_TEXT_BOLD);
		JCLabel label2 = new JCLabel(secondMsgPrompt, JCLabel.LABEL_TEXT_BOLD);
		JTextField inputField1 = new JTextField(10);
		if (firstMsgDefault != null && !firstMsgDefault.isNaN()) {
			inputField1.setText(firstMsgDefault.toString());
		}

		JTextField inputField2 = new JTextField(10);
		if (secondMsgDefault != null && !secondMsgDefault.isNaN()) {
			inputField2.setText(secondMsgDefault.toString());
		}
		JCheckBox ignoreCheckBox = new JCheckBox(
				skipMessage != null && !skipMessage.isBlank() ? skipMessage : "Do not show this prompt again");
		ignoreCheckBox.setFont(Utils.getTextFont(false));
		// Error label (always present but initially hidden)
		JCLabel errorLabel = new JCLabel("Invalid input. Please enter two decimal numbers.", JCLabel.LABEL_TEXT_PLAIN);
		errorLabel.setForeground(new Color(0, 0, 0, 0));

		// Align labels and fields correctly
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER).addComponent(promptLabel)
				.addComponent(spaceLabel) // Spacer before first input

				.addGroup(layout.createSequentialGroup()
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING) // Right-align labels
								.addComponent(label1).addComponent(label2))
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING) // Left-align fields
								.addComponent(inputField1).addComponent(inputField2)))
				.addComponent(ignoreCheckBox).addComponent(errorLabel));

		layout.setVerticalGroup(layout.createSequentialGroup().addComponent(promptLabel).addComponent(spaceLabel) // Spacer
																													// before
																													// first
																													// input
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(label1)
						.addComponent(inputField1))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(label2)
						.addComponent(inputField2))
				.addComponent(errorLabel).addComponent(ignoreCheckBox));

		while (true) {
			String[] options = { "OK", "Skip" };
			int result = JOptionPane.showOptionDialog(parent, panel, "Enter Vessel Diameter",
					JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

			if (result != 0) {
				DoubleResponse response = new DoubleResponse();
				response.double1 = null;
				response.double2 = null;
				response.skip = ignoreCheckBox.isSelected();
				return response;
			}

			try {
				Double num1 = Double.parseDouble(inputField1.getText().trim());
				Double num2 = Double.parseDouble(inputField2.getText().trim());

				// Save ignore setting
				DoubleResponse response = new DoubleResponse();
				response.double1 = num1;
				response.double2 = num2;
				response.skip = ignoreCheckBox.isSelected();
				return response;
			} catch (NumberFormatException e) {
				// Show error label without recreating dialog
				errorLabel.setForeground(Color.RED);
				panel.revalidate();
				panel.repaint();
			}
		}
	}

	/**
	 * Used to retrieve data to build an {@link Outcome}
	 */
	private interface FieldExtractorFunctionDouble {
		Double getDataPoint(WIAData data);
	}

	/**
	 * Used to retrieve data to build an {@link Outcome}
	 */
	private interface FieldExtractorFunctionBoolean {
		boolean getDataPoint(WIAData data);
	}

	/**
	 * Representation of a Wave. Notably different {@link Wave}'s within
	 * {@link WIAData} may have slightly different names but ultimately be
	 * considered the same wave, which is the purpose of this class.
	 */
	public static class StandardWave {

		/*
		 * Current wave name, which starts as original wave name, user may change. Is
		 * NOT used for hashing / testing equality
		 */
		private String waveName;
		/*
		 * The original wave name (which does NOT change when user types something
		 * different). This is used for hash.
		 */
		private String originalWaveName;
		private List<WIAData> samples = new ArrayList<WIAData>();
		private boolean proximal;

		/** Used for testing equality, includes -P or -D for direction of the wave */
		private String hashName;

		private StandardWave(String name, boolean proximal) {
			this.waveName = name;
			this.hashName = name + (proximal ? "-P" : "-D");
			this.originalWaveName = name;
			this.proximal = proximal;
		}

		/**
		 * Used for when the name is manually changed in the GUI. Notably, the original
		 * name is still stored as it is used to compute the hashcode value (which
		 * should not change)
		 * 
		 * @param name new name
		 */
		public void setName(String name) {
			this.waveName = name;
			this.hashName = name + (proximal ? "-P" : "-D");
		}

		/**
		 * @return the <i>current</i> name, NOT the original name used to create this
		 *         {@link StandardWave}
		 */
		public String getName() {
			return waveName;
		}

		/**
		 * @return true if this standard wave is proximal, false if distal
		 */
		public boolean isProximal() {
			return proximal;
		}

		/**
		 * @return the <i>original</i> name used to create this {@link StandardWave},
		 *         which may be different from the updated name set by the user with
		 *         {@link #setName(String)}
		 */
		public String getOriginalWaveName() {
			return originalWaveName;
		}

		public boolean equals(Object other) {
			if (other != null && other instanceof StandardWave && ((StandardWave) other).hashName.equals(this.hashName))
				return true;
			else
				return false;
		}

		public int hashCode() {
			return waveName.hashCode();
		}

		public String toString() {
			return waveName;
		}

		/**
		 * @param data to add. Does NOT check if the {@link WIAData} is already included
		 */
		private void addSample(WIAData data) {
			this.samples.add(data);
		}

		/**
		 * @param data to remove
		 * @return true if the WIAData was removed
		 */
		private boolean removeSample(WIAData data) {
			return this.samples.remove(data);
		}

		/**
		 * @return list of {@link WIAData} that have this {@link StandardWave}. The
		 *         return list is unmodifiable.
		 */
		public List<WIAData> getSamples() {
			return Collections.unmodifiableList(samples);
		}

		/**
		 * @return names of the {@link WIAData} which have this {@link StandardWave}, in
		 *         the format of 'original_file_name: selection_name'
		 */
		public List<String> getSampleNames() {

			List<String> list = samples.stream().map(data -> data.getFileName()).toList();

			return Collections.unmodifiableList(list);
		}

		/**
		 * @return total number of {@link WIAData} with this wave
		 */
		public int getCount() {
			return samples.size();
		}

		/**
		 * @param wave to check
		 * @return true if the name of the passed {@link Wave} equals (case insensitive)
		 *         the current wave name (may be different from original name), AND if
		 *         the direction is the same.
		 */
		public boolean matches(Wave wave) {
			return wave.getAbbrev().equalsIgnoreCase(waveName) && proximal == wave.isProximal();
		}

		/**
		 * @return the {@link Wave} within each {@link WIAData} which match this
		 *         {@link StandardWave} (by the method {@link #matches(Wave)}. If the
		 *         any of the {@link WIAData} passed as argument do not have a matching
		 *         {@link Wave}, will not be included in returned result
		 */
		public Map<WIAData, Wave> getWaves(Collection<WIAData> data) {

			Map<WIAData, Wave> containedWaves = new HashMap<WIAData, Wave>();

			for (WIAData qData : samples.stream().filter(dat -> data.contains(dat)).collect(Collectors.toList())) {

				Wave wave = qData.getWaves().stream().filter(wav -> matches(wav)).findFirst().orElse(null);

				if (wave == null) {
					continue;
				} else {

					containedWaves.put(qData, wave);
				}

			}

			return containedWaves;

		}

		/**
		 * Does the same as {@link #getWaves(Collection)}, except for a single
		 * {@link WIAData}
		 */
		public Wave getWave(WIAData data) {
			Wave wave = data.getWaves().stream().filter(wav -> originalWaveName.equalsIgnoreCase(wav.getAbbrev()))
					.findFirst().orElse(null);

			return wave;
		}

		/**
		 * Does the same as {@link #getWaves(Collection)}, except for a single
		 * {@link WIAData}
		 */
		public boolean hasWave(WIAData data) {
			return data.getWaves().stream().anyMatch(wav -> originalWaveName.equalsIgnoreCase(wav.getAbbrev()));

		}

	}

	/**
	 * Represents a grouping of {@link StandardWave}s. Can have a direction or no
	 * direction (no direction if there are both proximal and distal
	 * {@link StandardWave}s.
	 */
	public static class StandardWaveGrouping {

		private String groupName;
		private Set<StandardWave> standardWavesInGroup;
		private int direction = -2;
		private static final int BACKWARD = -1;
		private static final int BOTH = 0;
		private static final int FORWARD = 1;

		private StandardWaveGrouping(String groupName, Set<StandardWave> waveClassificationName) {
			this.groupName = groupName;
			this.standardWavesInGroup = waveClassificationName;

			for (StandardWave sw : waveClassificationName) {
				if (direction == -2) {
					direction = sw.isProximal() ? FORWARD : BACKWARD;
				} else if (direction != BOTH) {
					int nextDir = sw.isProximal() ? FORWARD : BACKWARD;
					if (nextDir != direction) {
						direction = BOTH;
					}
				}

			}

		}

		/**
		 * @return name of this grouping, which can be changed.
		 */
		public String getName() {
			return groupName;
		}

		/**
		 * Set the name of this grouping. It CANNOT be blank or null (will throw an
		 * {@link IllegalArgumentException})
		 */
		public void setName(String name) {
			if (name == null || name.isBlank())
				throw new IllegalArgumentException("Cannot set a blank name for a wave grouping");
			this.groupName = name;
		}

		/**
		 * @return unmodifiable set of {@link StandardWave}s which compose this
		 *         {@link StandardWaveGrouping}.
		 */
		public Set<StandardWave> getStandardWaves() {
			return Collections.unmodifiableSet(standardWavesInGroup);
		}

		/**
		 * 
		 * @return String with names of the {@link StandardWave}s in this grouping,
		 *         separated by a comma.
		 */
		public String getStandardWavesString() {
			StringBuilder sb = new StringBuilder();
			if (standardWavesInGroup.isEmpty()) {
				return "<none>";
			} else {
				boolean first = true;
				for (StandardWave wave : standardWavesInGroup) {
					if (first) {
						sb.append(wave.toString());
						first = false;
					} else {
						sb.append(", ").append(wave.toString());
					}
				}
				return sb.toString();
			}
		}

		/**
		 * @return number of {@link WIAData} in this group (sum of the {@link WIAData}
		 *         in each {@link StandardWave}).
		 */
		public int getCount() {

			return standardWavesInGroup.stream().mapToInt(StandardWave::getCount).sum();
		}

		/**
		 * @return true if proximal wave, false if distal wave, or null if both.
		 */
		public Boolean isProximal() {
			switch (direction) {
			case FORWARD:
				return true;
			case BACKWARD:
				return false;
			default:
				return null;
			}
		}

		/**
		 * Gets a list of the included {@link Wave}s based on a collection of input
		 * {@link WIAData}.
		 * 
		 * @param data input
		 * @return mapping, each input {@link WIAData} is a key and value is a list of
		 *         the corresponding {@link Wave}s considered to be in this
		 *         {@link StandardWaveGrouping}. If NO {@link Wave}s are found, there
		 *         will still be an entry but the value {@link List} will be an empty
		 *         list.
		 */
		public Map<WIAData, List<Wave>> getWaves(Collection<WIAData> data) {
			Map<WIAData, List<Wave>> waves = new HashMap<WIAData, List<Wave>>();

			for (WIAData wiadata : data) {
				List<Wave> wavesForWIAData = new ArrayList<Wave>();
				for (StandardWave sw : this.standardWavesInGroup) {
					Wave wave = sw.getWave(wiadata);
					if (wave != null) {
						wavesForWIAData.add(wave);
					}
				}
				waves.put(wiadata, wavesForWIAData);
			}

			return waves;
		}

	}

	/**
	 * Represents a standard treatment (like acetylcholine, adenosine)
	 */
	public static class StandardTreatment {

		private String name;
		private List<WIAData> samples = new ArrayList<WIAData>();
		private StandardTreatmentType txType;

		/**
		 * Constructs a new StandardTreatment.
		 *
		 * @param name The name of the treatment, used to identify its type (e.g.,
		 *             "Rest", "Adenosine").
		 */
		private StandardTreatment(String name) {
			this.name = name;

			txType = StandardTreatmentType.getMatching(name, true);
			if (txType == null) {
				txType = StandardTreatmentType.getMatching(name, false);
			}
			if (txType == null) {
				txType = StandardTreatmentType.OTHER;
			}

		}

		/**
		 * Adds a {@link WIAData} sample to this treatment group.
		 * 
		 * @param data The sample to add.
		 */
		private void addSample(WIAData data) {
			this.samples.add(data);
		}

		/**
		 * Removes a {@link WIAData} sample from this treatment group.
		 * 
		 * @param data The sample to remove.
		 */
		private void removeSample(WIAData data) {
			this.samples.remove(data);
		}

		/**
		 * Gets the file names of the samples in this treatment group.
		 * 
		 * @return An unmodifiable list of sample file names.
		 */
		public List<String> getSampleNames() {
			List<String> list = samples.stream().map(data -> data.getFileName()).toList();

			return Collections.unmodifiableList(list);
		}

		/**
		 * Gets the WIAData samples belonging to this treatment.
		 * 
		 * @return An unmodifiable list of {@link WIAData} objects.
		 */
		public List<WIAData> getSamples() {
			return Collections.unmodifiableList(samples);
		}

		/**
		 * Gets the name of this treatment.
		 * 
		 * @return The treatment name.
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Gets the number of samples in this treatment group.
		 * 
		 * @return The sample count.
		 */
		public int getCount() {
			return this.samples.size();
		}

		/**
		 * Gets the classified type of this treatment.
		 * 
		 * @return The {@link StandardTreatmentType}.
		 */
		public StandardTreatmentType getTreatmentType() {
			return this.txType;
		}

		/**
		 * Sets the classified type for this treatment.
		 * 
		 * @param type The new {@link StandardTreatmentType}.
		 */
		public void setTreatmentType(StandardTreatmentType type) {
			this.txType = type;
		}

		/**
		 * Checks if a given query string matches this treatment's name
		 * (case-insensitive).
		 * 
		 * @param query The string to match against.
		 * @return true if the names match, false otherwise.
		 */
		public boolean matches(String query) {
			return this.name.equalsIgnoreCase(query.trim());
		}

	}

	/**
	 * An enum representing standardized treatment types, used for classification
	 * and analysis. Each type has a display name, a color for UI rendering, and a
	 * list of aliases for matching against raw data labels.
	 */
	@SuppressWarnings("javadoc")
	public static enum StandardTreatmentType {

		REST("Rest", Color.BLUE, Arrays.asList(new String[] { "rest", "base", "baseline" })),
		ADENOSINE("Adenosine", Color.RED,
				Arrays.asList(new String[] { "adeno", "adenosine", "hyperemia", "hyperaemia" })),
		ACETYLCHOLINE("Acetylcholine", Color.GREEN.darker(),
				Arrays.asList(new String[] { "ACh", "Acetyl", "Acetylcholine" })),
		OTHER("Other", Color.BLACK, null);

		private String dispName;
		private List<String> alias;
		private Color color;

		/**
		 * Constructs a StandardTreatmentType enum constant.
		 *
		 * @param dispName The display name for the treatment type.
		 * @param color    The color associated with this treatment type for UI
		 *                 purposes.
		 * @param aliases  A list of alternative names or aliases for this treatment
		 *                 type.
		 */
		private StandardTreatmentType(String dispName, Color color, List<String> aliases) {
			this.alias = aliases;
			this.dispName = dispName;
			this.color = color;

		}

		/**
		 * Gets the color associated with this treatment type.
		 * 
		 * @return The UI color.
		 */
		public Color getColor() {
			return this.color;
		}

		/**
		 * Gets the display name of this treatment type.
		 * 
		 * @return The display name.
		 */
		public String getDisplayName() {
			return this.dispName;
		}

		/**
		 * Gets the display name of this treatment type.
		 * 
		 * @return The display name.
		 */
		public static StandardTreatmentType getMatching(String query, boolean strict) {

			if (strict) {
				for (StandardTreatmentType txType : values()) {

					if (txType.alias == null) {
						continue;
					}
					if (txType.alias.stream().anyMatch(str -> str.equalsIgnoreCase(query)))
						return txType;

				}

			} else {
				for (StandardTreatmentType txType : values()) {

					if (txType.alias == null) {
						continue;
					}
					if (txType.alias.stream().anyMatch(str -> query.toLowerCase().contains(str.toLowerCase())))
						return txType;

				}
			}

			return null;

		}

		@Override
		public String toString() {
			return this.dispName;
		}

	}

}
