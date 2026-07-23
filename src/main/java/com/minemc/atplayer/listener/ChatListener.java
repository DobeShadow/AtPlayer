package com.minemc.atplayer.listener;

import com.minemc.atplayer.AtPlayer;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener implements Listener {

    private final AtPlayer plugin;

    public ChatListener(AtPlayer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        String prefix = plugin.getMentionPrefix();
        Player sender = event.getPlayer();

        String regex = Pattern.quote(prefix) + "(\\S+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(message);

        Set<Player> mentioned = new HashSet<>();
        boolean atAllTriggered = false;

        while (matcher.find()) {
            String name = matcher.group(1);
            name = name.replaceAll("[,，。.!！？?]+$", "");

            // Check for @all
            if (plugin.isAtAllEnabled() && name.equalsIgnoreCase(plugin.getAtAllKeyword())) {
                if (!atAllTriggered) {
                    atAllTriggered = true;
                    notifyAll(sender, message);
                }
                continue;
            }

            Player target = Bukkit.getPlayer(name);
            if (target == null) continue;
            if (!target.isOnline()) continue;
            if (!target.hasPermission("atplayer.notify")) continue;
            if (!plugin.isAtEnabled(target)) continue;
            if (target.equals(sender)) continue;
            if (plugin.getExcludePlayers().contains(target.getName())) continue;

            if (mentioned.add(target)) {
                notifyPlayer(target, sender);
            }
        }

        // Highlight @mentions in chat message
        if (!plugin.getHighlightColor().isEmpty() && (!mentioned.isEmpty() || atAllTriggered)) {
            String colored = message.replaceAll(
                    regex,
                    plugin.getHighlightColor() + prefix + "$1" + "§r"
            );
            event.setMessage(colored);
        }
    }

    private void notifyPlayer(Player target, Player sender) {
        String title = plugin.getTitleText();
        String subtitle = plugin.getSubtitleText().replace("{sender}", sender.getName());
        target.sendTitle(
                title, subtitle,
                plugin.getTitleFadeIn(),
                plugin.getTitleStay(),
                plugin.getTitleFadeOut()
        );

        target.playSound(
                target.getLocation(),
                plugin.getSoundType(),
                SoundCategory.MASTER,
                plugin.getSoundVolume(),
                plugin.getSoundPitch()
        );
    }

    private void notifyAll(Player sender, String message) {
        // Permission check
        String perm = plugin.getAtAllPermission();
        if (!perm.isEmpty() && !sender.hasPermission(perm)) {
            sender.sendMessage(AtPlayer.colorize("&c你没有权限使用 @所有人！"));
            return;
        }

        String title = plugin.getAtAllTitle();
        String subtitle = plugin.getAtAllSubtitle().replace("{sender}", sender.getName());
        int online = 0;

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.hasPermission("atplayer.notify")) continue;
            if (!plugin.isAtEnabled(target)) continue;
            if (target.equals(sender)) continue;

            target.sendTitle(
                    title, subtitle,
                    plugin.getTitleFadeIn(),
                    plugin.getTitleStay(),
                    plugin.getTitleFadeOut()
            );
            target.playSound(
                    target.getLocation(),
                    plugin.getSoundType(),
                    SoundCategory.MASTER,
                    plugin.getSoundVolume(),
                    plugin.getSoundPitch()
            );
            online++;
        }

        sender.sendMessage(AtPlayer.colorize("&a已通知 &e" + online + " &a名在线玩家"));
    }
}
