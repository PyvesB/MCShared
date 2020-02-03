package com.hm.mcshared.file;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Class to take care of Yaml configuration files by providing some additional methods to the standard Bukkit
 * YamlConfiguration class. This class enables more modularity, and contains methods to set new configuration parameters
 * with comments as well as various other convenience methods.
 * 
 * @author Pyves
 *
 */
public class CommentedYamlConfiguration extends YamlConfiguration {

	private final FileManager manager;
	private final JavaPlugin plugin;

	private int numOfComments;

	/**
	 * Creates a new CommentedYamlConfiguration object representing one of the plugin's configuration files.
	 * 
	 * @param fileName Name of the configuration file situated in the resource folder of the plugin.
	 * @param plugin The plugin making use of the configuration file.
	 */
	public CommentedYamlConfiguration(String fileName, JavaPlugin plugin) {
		this(fileName, fileName, plugin);
	}

	/**
	 * Creates a new CommentedYamlConfiguration object representing one of the plugin's configuration files. This
	 * constructor takes a plugin resource name that may be different for the file name used on the server.
	 * 
	 * @param fileName Name of the configuration file situated in the resource folder of the plugin.
	 * @param pluginResourceName The name of the plugin resource.
	 * @param plugin The plugin making use of the configuration file.
	 */
	public CommentedYamlConfiguration(String fileName, String pluginResourceName, JavaPlugin plugin) {
		super();
		if (fileName == null || fileName.isEmpty()) {
			throw new IllegalArgumentException("Invalid file name.");
		}
		this.plugin = plugin;
		manager = new FileManager(fileName, pluginResourceName, plugin);
	}

	/**
	 * Gets a set containing all keys in this section, with only the keys of any direct children, and not their own
	 * children. Helper method.
	 * 
	 * @param path
	 * @return Set containing the shallow configuration keys for the given path.
	 */
	public Set<String> getShallowKeys(String path) {
		return getConfigurationSection(path).getKeys(false);
	}

	/**
	 * Gets the requested ConfigurationSection by path. Returns an empty ConfigurationSection if it does not exist.
	 * 
	 * @param path Path of the ConfigurationSection to get.
	 * @return Requested ConfigurationSection or empty one.
	 */
	@Override
	public ConfigurationSection getConfigurationSection(String path) {
		ConfigurationSection configurationSection = super.getConfigurationSection(path);
		return configurationSection == null ? createSection(path) : configurationSection;
	}

	/**
	 * Gets the requested List by path. Returns a List of type String.
	 * 
	 * @param path Path of the List to get.
	 * @return Requested List of Strings.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<String> getList(String path) {
		List<String> list = (List<String>) super.getList(path);
		if (list != null) {
			return list;
		}
		return new ArrayList<>();
	}

	/**
	 * Sets the specified path to the given value with the given comment.
	 * 
	 * @param path Path of the object to set.
	 * @param value New value to set the path to.
	 * @param comment New comment to set the path to. Ignored if null.
	 */
	public void set(String path, Object value, String comment) {
		if (comment != null) {
			// Insert comment as new value in the file; will be converted back to a comment when saved by the
			// FileManager.
			this.set(plugin.getDescription().getName() + "_COMMENT_" + numOfComments, comment.replace(":", "_COLON_")
					.replace("|", "_VERT_").replace("-", "_HYPHEN_").replace(" ", "_SPACE_"));
			numOfComments++;
		}
		this.set(path, value);
	}

	/**
	 * Sets the specified path to the given value with the given comments (one comment per line).
	 * 
	 * @param path Path of the object to set.
	 * @param value New value to set the path to.
	 * @param comments New comments to set the path to. Ignored if empty.
	 */
	public void set(String path, Object value, String... comments) {
		for (String comment : comments) {
			// Insert comment as new value in the file; will be converted back to a comment when saved.
			this.set(plugin.getDescription().getName() + "_COMMENT_" + numOfComments, comment.replace(":", "_COLON_")
					.replace("|", "_VERT_").replace("-", "_HYPHEN_").replace(" ", "_SPACE_"));
			numOfComments++;
		}
		this.set(path, value);
	}

	/**
	 * Loads or reloads the configuration file from disk.
	 * 
	 * @throws IOException
	 * @throws InvalidConfigurationException
	 */
	public void loadConfiguration() throws IOException, InvalidConfigurationException {
		manager.createConfigurationFileIfNotExists();
		map.clear();
		load(new StringReader(manager.getConfigurationWithReworkedComments()));
		numOfComments = manager.getNumberOfComments();
	}

	/**
	 * Saves the configuration file and any modifications that were performed.
	 * 
	 * @throws IOException
	 */
	public void saveConfiguration() throws IOException {
		String configString = this.saveToString();
		manager.saveConfiguration(configString);
	}

	/**
	 * Performs a backup of the configuration; the backup simply makes a copy of the file and adds a .bak extension to
	 * it.
	 * 
	 * @throws IOException
	 */
	public void backupConfiguration() throws IOException {
		manager.backupFile();
	}
}
