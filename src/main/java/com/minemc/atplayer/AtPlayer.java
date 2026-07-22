package com.minemc.atplayer;

import com.minemc.atplayer.listener.ChatListener;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class AtPlayer extends JavaPlugin {

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

    @Override
    public void onEnable() {
        instance = this;
        loadConfig();
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getLogger().info("AtPlayer v" + getPluginMeta().getVersion() + " 已启动！");
    }

    @Override
    public void onDisable() {
        instance = null;
        getLogger().info("AtPlayer 已卸载！");
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
