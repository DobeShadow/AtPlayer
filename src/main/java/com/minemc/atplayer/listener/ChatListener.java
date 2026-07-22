package com.minemc.atplayer.listener;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.minemc.atplayer.AtPlayer;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChatListener implements Listener {

    private final AtPlayer plugin;

    public ChatListener(AtPlayer plugin) {
        this.plugin = plugin;
    }

    /**
     * TAB completion for @player in chat.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onTabComplete(AsyncTabCompleteEvent event) {
        if (!(event.getSender() instanceof Player sender)) return;
        if (!sender.hasPermission("atplayer.use")) return;

        // isCommand() may return true for chat in newer Paper — check buffer for @ instead
        String buffer = event.getBuffer();
        plugin.getLogger().info("[DEBUG] TabComplete: isCommand=" + event.isCommand() + " buffer=" + buffer);

        // Only handle buffers containing @ prefix (chat), not commands
        String prefix = plugin.getMentionPrefix();
        if (!buffer.contains(prefix)) return;

        // Get partial name after the last @
        int atIndex = buffer.lastIndexOf(prefix);
        String partial = buffer.substring(atIndex + prefix.length());
        if (partial.contains(" ")) return;

        List<String> completions = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.equals(sender))
                .filter(p -> partial.isEmpty() || p.getName().toLowerCase().startsWith(partial.toLowerCase()))
                .filter(p -> !plugin.getExcludePlayers().contains(p.getName()))
                .map(p -> prefix + p.getName())
                .collect(Collectors.toList());

        event.getCompletions().addAll(completions);
        if (!completions.isEmpty()) {
            event.setHandled(true);
            plugin.getLogger().info("[DEBUG] Added " + completions.size() + " completions: " + completions);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        String prefix = plugin.getMentionPrefix();
        Player sender = event.getPlayer();

        // Find all @mentions: escape prefix for regex, match non-whitespace after it
        String regex = Pattern.quote(prefix) + "(\\S+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(message);

        Set<Player> mentioned = new HashSet<>();

        while (matcher.find()) {
            String name = matcher.group(1);
            // Strip trailing punctuation
            name = name.replaceAll("[,，。.!！？?]+$", "");

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
        if (!mentioned.isEmpty() && !plugin.getHighlightColor().isEmpty()) {
            String colored = message.replaceAll(
                    regex,
                    plugin.getHighlightColor() + prefix + "$1" + "§r"
            );
            event.setMessage(colored);
        }
    }

    private void notifyPlayer(Player target, Player sender) {
        // Title notification
        String title = plugin.getTitleText();
        String subtitle = plugin.getSubtitleText().replace("{sender}", sender.getName());
        target.sendTitle(
                title,
                subtitle,
                plugin.getTitleFadeIn(),
                plugin.getTitleStay(),
                plugin.getTitleFadeOut()
        );

        // Sound notification
        target.playSound(
                target.getLocation(),
                plugin.getSoundType(),
                SoundCategory.MASTER,
                plugin.getSoundVolume(),
                plugin.getSoundPitch()
        );
    }
}
