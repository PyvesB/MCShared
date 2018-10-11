package com.hm.mcshared.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Class to take care of the management of plugin files.
 * <p>
 * For configuration files, this class provides a workaround to keep comments in the file when using Bukkit's
 * YamlConfiguration class (otherwise comments are deleted when the file is modified and saved programatically).
 * <p>
 * Other convenience methods such as doing backups are also provided and can be used for other file types.
 * 
 * @author Pyves
 *
 */
public class FileManager {

	private final JavaPlugin plugin;
	private final String pluginResourceName;

	private File file;
	private int numOfComments;

	/**
	 * Constructs an instance of the manager.
	 * 
	 * @param fileName Name of the file situated in the resource folder of the plugin.
	 * @param plugin The plugin making use of the file.
	 */
	public FileManager(String fileName, JavaPlugin plugin) {
		this(fileName, fileName, plugin);
	}

	/**
	 * Constructs an instance of the manager. This constructor takes a plugin resource name that may be different for
	 * the file name used on the server.
	 * 
	 * @param fileName Name of the file situated in the resource folder of the plugin.
	 * @param pluginResourceName The name of the plugin resource.
	 * @param plugin The plugin making use of the file.
	 */
	public FileManager(String fileName, String pluginResourceName, JavaPlugin plugin) {
		this.plugin = plugin;
		this.pluginResourceName = pluginResourceName;
		if (fileName.startsWith("/")) {
			file = new File(plugin.getDataFolder() + fileName.replace("/", File.separator));
		} else {
			file = new File(plugin.getDataFolder() + File.separator + fileName.replace("/", File.separator));
		}
	}

	/**
	 * Performs a backup of the file contained in the plugin's data folder; the backup simply makes a copy of the file
	 * and adds a .bak extension to it.
	 * 
	 * @throws IOException
	 */
	public void backupFile() throws IOException {
		File backupFile = new File(plugin.getDataFolder(), file.getName() + ".bak");
		// Overwrite previous backup only if a newer version of the file exists.
		if (file.lastModified() > backupFile.lastModified() && file.exists()) {
			try (FileInputStream inStream = new FileInputStream(file);
					FileOutputStream outStream = new FileOutputStream(backupFile)) {
				byte[] buffer = new byte[1024];
				int length;
				while ((length = inStream.read(buffer)) > 0) {
					outStream.write(buffer, 0, length);
				}
			}
		}
	}

	/**
	 * Returns a configuration string with comments reworked as key-value pairs to allow them to be saved when using
	 * Bukkit's YamlConfiguration class.
	 * 
	 * @return String with reworked comments.
	 * @throws IOException
	 */
	protected String getConfigurationWithReworkedComments() throws IOException {
		StringBuilder reworkedConfiguration = new StringBuilder();
		try (FileInputStream fileInputStream = new FileInputStream(file);
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream, "UTF-8"))) {
			numOfComments = 0;
			String currentLine;
			while ((currentLine = bufferedReader.readLine()) != null) {
				if (currentLine.startsWith("#")) {
					// Rework comment line so it becomes a standard key-value pair in the configuration file.
					// This workaround allows the comment to be saved when using Bukkit's YamlConfiguration class.
					reworkedConfiguration.append(currentLine.replace(":", "_COLON_").replace("|", "_VERT_")
							.replace("-", "_HYPHEN_").replace(" ", "_SPACE_")
							.replaceFirst("#", plugin.getDescription().getName() + "_COMMENT_" + numOfComments + ": "));
					numOfComments++;
				} else {
					reworkedConfiguration.append(currentLine);
				}
				reworkedConfiguration.append("\n");
			}
			return reworkedConfiguration.toString();
		}
	}

	/**
	 * Returns the total number of comments in the file. Should be called after the configuration has been parsed by
	 * {@link #getConfigurationWithReworkedComments()}.
	 * 
	 * @return Total number of comments in the file.
	 */
	protected int getNumberOfComments() {
		return numOfComments;
	}

	/**
	 * Saves a configuration string into a file.
	 * 
	 * @param configString The configuration string to save.
	 * @throws IOException
	 */
	protected void saveConfiguration(String configString) throws IOException {
		String configuration = getConfigurationWithRegeneratedComments(configString);
		try (FileOutputStream fileOutputStream = new FileOutputStream(file);
				OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "UTF-8");
				BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter)) {
			bufferedWriter.write(configuration);
			bufferedWriter.flush();
		}
	}

	/**
	 * Writes the configuration file to disk if it does not exist by copying it from the plugin's resources.
	 * 
	 * @throws IOException
	 */
	protected void createConfigurationFileIfNotExists() throws IOException {
		file.getParentFile().mkdirs();
		if (file.createNewFile()) {
			try (OutputStream outputStream = new FileOutputStream(file)) {
				InputStream resource = plugin.getResource(pluginResourceName);
				if (resource != null) {
					int length;
					byte[] buf = new byte[1024];
					while ((length = resource.read(buf)) > 0) {
						outputStream.write(buf, 0, length);
					}
				}
			}
			plugin.getLogger().info("Successfully created " + file.getName() + " file.");
		}
	}

	/**
	 * Reworks the configuration string in order to regenerate comments.
	 * 
	 * @param configString The configuration string with reworked comments.
	 * @return String representing original config file.
	 */
	private String getConfigurationWithRegeneratedComments(String configString) {
		boolean previousLineComment = false;
		String[] lines = configString.split("\n");
		StringBuilder config = new StringBuilder();
		for (String line : lines) {
			if (line.startsWith(plugin.getDescription().getName() + "_COMMENT")) {
				// Rework comment line so it is converted back to a normal comment.
				String comment = ("#" + line.substring(line.indexOf(": ") + 2)).replace("_COLON_", ":")
						.replace("_HYPHEN_", "-").replace("_VERT_", "|").replace("_SPACE_", " ");
				// No empty line between consecutive comment lines or between a comment and its corresponding
				// parameters; empty line between parameter and new comment.
				if (previousLineComment) {
					config.append(comment + "\n");
				} else {
					config.append("\n" + comment + "\n");
				}
				previousLineComment = true;
			} else {
				config.append(line + "\n");
				previousLineComment = false;
			}
		}
		return config.toString();
	}
}
