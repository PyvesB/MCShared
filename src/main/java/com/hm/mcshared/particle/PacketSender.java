package com.hm.mcshared.particle;

import java.lang.reflect.InvocationTargetException;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.hm.mcshared.particle.ReflectionUtils.PackageType;

/**
 * Class used to send packets to the player; can be titles, json chat messages or action bar messages. All methods are
 * static and this class cannot be instanciated.
 * 
 * @author Pyves
 *
 */
public final class PacketSender {

	private static final byte CHAT_MESSAGE_BYTE = 1;
	private static final byte ACTION_BAR_BYTE = 2;
	private static final int VERSION = Integer.parseInt(PackageType.getServerVersion().split("_")[1]);
	private static final String CLASS_CHAT_BASE_COMPONENT = "IChatBaseComponent";
	private static final String CLASS_CRAFT_PLAYER = "CraftPlayer";
	private static final String CLASS_ENTITY_PLAYER = "EntityPlayer";
	private static final String CLASS_PACKET = "Packet";
	private static final String CLASS_PACKET_PLAY_OUT_CHAT = "PacketPlayOutChat";
	private static final String CLASS_PACKET_PLAY_OUT_TITLE = "PacketPlayOutTitle";
	private static final String CLASS_PLAYER_CONNECTION = "PlayerConnection";
	private static final String ENUM_TITLE_ACTION = "EnumTitleAction";
	private static final String ENUM_CHAT_MESSAGE_TYPE = "ChatMessageType";
	private static final String FIELD_PLAYER_CONNECTION = "playerConnection";
	private static final String METHOD_GET_HANDLE = "getHandle";
	private static final String METHOD_SEND_PACKET = "sendPacket";
	private static final String NESTED_CHAT_SERIALIZER = "ChatSerializer";
	private static final String PACKAGE_ENTITY = "entity";

	private PacketSender() {
		// Not called.
	}

	/**
	 * Sends a chat message packet to the player. Can for instance be used to create hoverable and clickable chat
	 * messages.
	 * 
	 * @param player Online player to send the packet to.
	 * @param json The JSON format message to send to the player. See
	 *            http://minecraft.gamepedia.com/Commands#Raw_JSON_text for more information.
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws NoSuchFieldException
	 */
	public static void sendChatMessagePacket(Player player, String json)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException,
			InstantiationException, NoSuchFieldException {

		sendChatPacket(player, json, CHAT_MESSAGE_BYTE);
	}

	/**
	 * Sends an action bar chat packet.
	 * 
	 * @param player Online player to send the packet to.
	 * @param json The JSON format message to send to the player. See
	 *            http://minecraft.gamepedia.com/Commands#Raw_JSON_text for more information.
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws NoSuchFieldException
	 */
	public static void sendActionBarPacket(Player player, String json)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException,
			InstantiationException, NoSuchFieldException {

		sendChatPacket(player, json, ACTION_BAR_BYTE);
	}

	/**
	 * Sends a chat packet.
	 * 
	 * @param player
	 * @param json
	 * @param type
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws NoSuchFieldException
	 */
	private static void sendChatPacket(Player player, String json, byte type)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException,
			InstantiationException, NoSuchFieldException {

		// Retrieve a CraftPlayer instance and its PlayerConnection instance.
		Object craftPlayer = PackageType.CRAFTBUKKIT.getClass(PACKAGE_ENTITY + "." + CLASS_CRAFT_PLAYER).cast(player);
		Object craftHandle = PackageType.CRAFTBUKKIT.getClass(PACKAGE_ENTITY + "." + CLASS_CRAFT_PLAYER)
				.getMethod(METHOD_GET_HANDLE).invoke(craftPlayer);
		Object playerConnection = PackageType.MINECRAFT_SERVER.getClass(CLASS_ENTITY_PLAYER)
				.getField(FIELD_PLAYER_CONNECTION).get(craftHandle);

		// Parse the json message.
		Object parsedMessage;
		try {
			// Since 1.8.3
			parsedMessage = Class
					.forName(PackageType.MINECRAFT_SERVER + "." + CLASS_CHAT_BASE_COMPONENT + "$"
							+ NESTED_CHAT_SERIALIZER)
					.getMethod("a", String.class)
					.invoke(null, ChatColor.translateAlternateColorCodes('&', json));
		} catch (ClassNotFoundException e) {
			// Older versions of the game.
			parsedMessage = PackageType.MINECRAFT_SERVER.getClass(NESTED_CHAT_SERIALIZER).getMethod("a", String.class)
					.invoke(null, ChatColor.translateAlternateColorCodes('&', json));
		}

		Object packetPlayOutChat;
		if (VERSION < 12) {
			packetPlayOutChat = PackageType.MINECRAFT_SERVER.getClass(CLASS_PACKET_PLAY_OUT_CHAT)
					.getConstructor(PackageType.MINECRAFT_SERVER.getClass(CLASS_CHAT_BASE_COMPONENT), byte.class)
					.newInstance(parsedMessage, type);
		} else {
			// New method uses the ChatMessageType enum rather than a byte.
			Class<?> chatMessageTypeClass = PackageType.MINECRAFT_SERVER.getClass(ENUM_CHAT_MESSAGE_TYPE);
			Enum<?> chatType = null;
			for (Object chatMessageType : chatMessageTypeClass.getEnumConstants()) {
				Enum<?> e = (Enum<?>) chatMessageType;
				if ("SYSTEM".equalsIgnoreCase(e.name()) && type == CHAT_MESSAGE_BYTE
						|| "GAME_INFO".equalsIgnoreCase(e.name()) && type == ACTION_BAR_BYTE) {
					chatType = e;
					break;
				}
			}
			packetPlayOutChat = PackageType.MINECRAFT_SERVER.getClass(CLASS_PACKET_PLAY_OUT_CHAT)
					.getConstructor(PackageType.MINECRAFT_SERVER.getClass(CLASS_CHAT_BASE_COMPONENT),
							chatMessageTypeClass)
					.newInstance(parsedMessage, chatType);
		}

		// Send the message packet through the PlayerConnection.
		PackageType.MINECRAFT_SERVER.getClass(CLASS_PLAYER_CONNECTION)
				.getMethod(METHOD_SEND_PACKET, PackageType.MINECRAFT_SERVER.getClass(CLASS_PACKET))
				.invoke(playerConnection, packetPlayOutChat);
	}

	/**
	 * Sends a title packet (title and subtitle will appear on screen).
	 * 
	 * @param player Online player to send the packet to.
	 * @param mainJson The title JSON format message to send to the player. See
	 *            http://minecraft.gamepedia.com/Commands#Raw_JSON_text for more information.
	 * @param subJson The subtitle title JSON format message to send to the player. See
	 *            http://minecraft.gamepedia.com/Commands#Raw_JSON_text for more information.
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws NoSuchFieldException
	 */
	public static void sendTitlePacket(Player player, String mainJson, String subJson)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException,
			InstantiationException, NoSuchFieldException {

		// Retrieve a CraftPlayer instance and its PlayerConnection instance.
		Object craftPlayer = PackageType.CRAFTBUKKIT.getClass(PACKAGE_ENTITY + "." + CLASS_CRAFT_PLAYER).cast(player);
		Object craftHandle = PackageType.CRAFTBUKKIT.getClass(PACKAGE_ENTITY + "." + CLASS_CRAFT_PLAYER)
				.getMethod(METHOD_GET_HANDLE).invoke(craftPlayer);
		Object playerConnection = PackageType.MINECRAFT_SERVER.getClass(CLASS_ENTITY_PLAYER)
				.getField(FIELD_PLAYER_CONNECTION).get(craftHandle);

		// Parse the json message.
		Object parsedMainMessage;
		try {
			// Since 1.8.3
			parsedMainMessage = PackageType.MINECRAFT_SERVER
					.getClass(CLASS_CHAT_BASE_COMPONENT + "$" + NESTED_CHAT_SERIALIZER).getMethod("a", String.class)
					.invoke(null, ChatColor.translateAlternateColorCodes('&', mainJson));
		} catch (ClassNotFoundException e) {
			// Older versions of the game.
			parsedMainMessage = PackageType.MINECRAFT_SERVER.getClass(NESTED_CHAT_SERIALIZER)
					.getMethod("a", String.class)
					.invoke(null, ChatColor.translateAlternateColorCodes('&', mainJson));
		}

		// Parse the json message.
		Object parsedSubMessage;
		try {
			// Since 1.8.3
			parsedSubMessage = PackageType.MINECRAFT_SERVER
					.getClass(CLASS_CHAT_BASE_COMPONENT + "$" + NESTED_CHAT_SERIALIZER).getMethod("a", String.class)
					.invoke(null, ChatColor.translateAlternateColorCodes('&', subJson));
		} catch (ClassNotFoundException e) {
			// Older versions of the game.
			parsedSubMessage = PackageType.MINECRAFT_SERVER.getClass(NESTED_CHAT_SERIALIZER)
					.getMethod("a", String.class)
					.invoke(null, ChatColor.translateAlternateColorCodes('&', subJson));
		}

		Class<?> titleClass;
		try {
			// Since 1.8.3
			titleClass = PackageType.MINECRAFT_SERVER.getClass(CLASS_PACKET_PLAY_OUT_TITLE + "$" + ENUM_TITLE_ACTION);
		} catch (ClassNotFoundException e) {
			// Older versions of the game.
			titleClass = PackageType.MINECRAFT_SERVER.getClass(ENUM_TITLE_ACTION);
		}
		// Retrieve parameters for titles and subtitles.
		Enum<?> mainTitleEnumValue = null;
		Enum<?> subTitleEnumValue = null;
		for (Object o : titleClass.getEnumConstants()) {
			Enum<?> e = (Enum<?>) o;
			if ("TITLE".equalsIgnoreCase(e.name())) {
				mainTitleEnumValue = e;
			} else if ("SUBTITLE".equalsIgnoreCase(e.name())) {
				subTitleEnumValue = e;
			}
		}

		Object packetPlayOutChatMainTitle = ReflectionUtils
				.getConstructor(PackageType.MINECRAFT_SERVER.getClass(CLASS_PACKET_PLAY_OUT_TITLE),
						PackageType.MINECRAFT_SERVER.getClass(CLASS_PACKET_PLAY_OUT_TITLE + "$" + ENUM_TITLE_ACTION),
						PackageType.MINECRAFT_SERVER.getClass(CLASS_CHAT_BASE_COMPONENT))
				.newInstance(mainTitleEnumValue, parsedMainMessage);

		// Send the message packet through the PlayerConnection (title).
		PackageType.MINECRAFT_SERVER.getClass(CLASS_PLAYER_CONNECTION)
				.getMethod(METHOD_SEND_PACKET, PackageType.MINECRAFT_SERVER.getClass(CLASS_PACKET))
				.invoke(playerConnection, packetPlayOutChatMainTitle);

		Object packetPlayOutChatSubTitle = ReflectionUtils
				.getConstructor(PackageType.MINECRAFT_SERVER.getClass(CLASS_PACKET_PLAY_OUT_TITLE),
						PackageType.MINECRAFT_SERVER.getClass(CLASS_PACKET_PLAY_OUT_TITLE + "$" + ENUM_TITLE_ACTION),
						PackageType.MINECRAFT_SERVER.getClass(CLASS_CHAT_BASE_COMPONENT))
				.newInstance(subTitleEnumValue, parsedSubMessage);

		// Send the message packet through the PlayerConnection (subtitle).
		PackageType.MINECRAFT_SERVER.getClass(CLASS_PLAYER_CONNECTION)
				.getMethod(METHOD_SEND_PACKET, PackageType.MINECRAFT_SERVER.getClass(CLASS_PACKET))
				.invoke(playerConnection, packetPlayOutChatSubTitle);
	}
}
