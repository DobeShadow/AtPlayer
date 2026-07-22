package com.minemc.atplayer;

import com.minemc.atplayer.listener.ChatListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class AtPlayer extends JavaPlugin implements TabCompleter {

    private static AtPlayer instance;

    // Config values
    private String mentionPrefix = "@";
    private String titleText = "&e&l有人@你！";
    private String subtitleText = "&f{sender} &7在聊天中提到了你";
    private int titleFadeIn = 10;
    private int titleStay = 40;
    private int titleFadeOut = 10;
    private Sound soundType = Sound.BLOCK_NOTE_BLOCK_PLING;
    private float soundVolume = 1.0f;
    private float soundPitch = 2.0f;
    private String highlightColor = "&b";
    private List<String> excludePlayers = new ArrayList<>();

    // Players who have @ notifications disabled
    private final Set<UUID> disabledPlayers = new HashSet<>();
    private File dataFile;

    @Override
    public void onEnable() {
        instance = this;
        dataFile = new File(getDataFolder(), "players.yml");
        loadConfig();
        loadPlayerData();
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getCommand("atplayer").setExecutor(this);
        getCommand("atplayer").setTabCompleter(this);
        getLogger().info("AtPlayer v" + getPluginMeta().getVersion() + " 已启动！");
    }

    @Override
    public void onDisable() {
        savePlayerData();
        instance = null;
        getLogger().info("AtPlayer 已卸载！");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(colorize("&c该命令只能由玩家执行！"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("toggle")) {
            UUID uuid = player.getUniqueId();
            if (disabledPlayers.contains(uuid)) {
                disabledPlayers.remove(uuid);
                player.sendMessage(colorize("&a✔ 已开启 @提醒通知"));
            } else {
                disabledPlayers.add(uuid);
                player.sendMessage(colorize("&c✘ 已关闭 @提醒通知"));
            }
            savePlayerData();
            return true;
        }

        if (args[0].equalsIgnoreCase("status")) {
            boolean on = !disabledPlayers.contains(player.getUniqueId());
            player.sendMessage(colorize("&7@提醒状态: " + (on ? "&a开启" : "&c关闭")));
            return true;
        }

        // /at <玩家名> — manually notify a player
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(colorize("&c找不到玩家: " + args[0]));
            return true;
        }
        if (target.equals(player)) {
            player.sendMessage(colorize("&c不能@自己！"));
            return true;
        }
        if (!target.hasPermission("atplayer.notify") || !isAtEnabled(target)) {
            player.sendMessage(colorize("&c该玩家已关闭@提醒或没有权限"));
            return true;
        }

        // Send notification to target
        String title = colorize(titleText);
        String subtitle = colorize(subtitleText.replace("{sender}", player.getName()));
        target.sendTitle(title, subtitle, titleFadeIn, titleStay, titleFadeOut);
        target.playSound(target.getLocation(), soundType, org.bukkit.SoundCategory.MASTER, soundVolume, soundPitch);
        player.sendMessage(colorize("&a已向 &e" + target.getName() + " &a发送@提醒"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.equals(sender))
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(partial))
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());
        }
        return List.of();
    }

    public void loadConfig() {
        saveDefaultConfig();
        reloadConfig();
        FileConfiguration cfg = getConfig();

        mentionPrefix = cfg.getString("mention-prefix", "@");
        titleText = colorize(cfg.getString("title.title", "&e&l有人@你！"));
        subtitleText = colorize(cfg.getString("title.subtitle", "&f{sender} &7在聊天中提到了你"));
        titleFadeIn = cfg.getInt("title.fade-in", 10);
        titleStay = cfg.getInt("title.stay", 40);
        titleFadeOut = cfg.getInt("title.fade-out", 10);

        String soundName = cfg.getString("sound.type", "BLOCK_NOTE_BLOCK_PLING");
        try {
            soundType = Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            getLogger().log(Level.WARNING, "无效的声音类型: " + soundName + "，使用默认值");
            soundType = Sound.BLOCK_NOTE_BLOCK_PLING;
        }
        soundVolume = (float) cfg.getDouble("sound.volume", 1.0);
        soundPitch = (float) cfg.getDouble("sound.pitch", 2.0);

        highlightColor = colorize(cfg.getString("highlight-color", "&b"));
        excludePlayers = cfg.getStringList("exclude-players");
    }

    private void loadPlayerData() {
        if (!dataFile.exists()) return;
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        disabledPlayers.clear();
        for (String uuidStr : data.getStringList("disabled")) {
            try {
                disabledPlayers.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void savePlayerData() {
        FileConfiguration data = new YamlConfiguration();
        List<String> uuidList = disabledPlayers.stream().map(UUID::toString).toList();
        data.set("disabled", uuidList);
        try {
            dataFile.getParentFile().mkdirs();
            data.save(dataFile);
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Failed to save player data", e);
        }
    }

    public boolean isAtEnabled(Player player) {
        return !disabledPlayers.contains(player.getUniqueId());
    }

    public void reload() {
        loadConfig();
    }

    public static AtPlayer getInstance() { return instance; }

    // ---- Config getters ----
    public String getMentionPrefix() { return mentionPrefix; }
    public String getTitleText() { return titleText; }
    public String getSubtitleText() { return subtitleText; }
    public int getTitleFadeIn() { return titleFadeIn; }
    public int getTitleStay() { return titleStay; }
    public int getTitleFadeOut() { return titleFadeOut; }
    public Sound getSoundType() { return soundType; }
    public float getSoundVolume() { return soundVolume; }
    public float getSoundPitch() { return soundPitch; }
    public String getHighlightColor() { return highlightColor; }
    public List<String> getExcludePlayers() { return excludePlayers; }

    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
