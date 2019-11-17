package com.uddernetworks.disddit.discord;

import com.uddernetworks.disddit.Disddit;
import com.uddernetworks.disddit.database.DatabaseManager;
import net.dean.jraw.models.Subreddit;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordStateManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordStateManager.class);

    private final Disddit disddit;
    private final DiscordManager discordManager;
    private final DatabaseManager databaseManager;
    private final Map<Guild, Category> categories = new ConcurrentHashMap<>();

    public DiscordStateManager(Disddit disddit, DiscordManager discordManager, DatabaseManager databaseManager) {
        this.disddit = disddit;
        this.discordManager = discordManager;
        this.databaseManager = databaseManager;
    }

    public CompletableFuture<Void> init() {
        return databaseManager.getAllGuildCategories().thenAccept(guildCategoryMap -> {
            synchronized (categories) {
                categories.clear();
                categories.putAll(guildCategoryMap);
            }
            LOGGER.info("Registered {} unique subreddit categories", categories.size());
        }).exceptionally(t -> {
            LOGGER.error("An error occurred while loading database", t);
            return null;
        });
    }

    public CompletableFuture<Category> createCategory(Guild guild) {
        var everyone = guild.getPublicRole();
        var subredditsCategory = guild.createCategory("subreddits")
//                .addPermissionOverride(everyone, Collections.emptyList(), Arrays.asList(MANAGE_CHANNEL, MESSAGE_WRITE, MANAGE_PERMISSIONS)).complete();
                .complete();
        return databaseManager.addGuild(guild, subredditsCategory).thenApply($ -> subredditsCategory);
    }

    public CompletableFuture<Category> getOrCreateCategory(Guild guild) {
        return databaseManager.getGuildCategory(guild).thenApply(optionalCategory ->
                optionalCategory.orElseGet(() -> createCategory(guild).join()));
    }

    public CompletableFuture<TextChannel> addSubredditChannel(Guild guild, Subreddit subreddit) {
        return getOrCreateCategory(guild).thenApply(category ->
                    category.createTextChannel(subreddit.getName()).setTopic("Disddit-managed channel for the r/" + subreddit.getName() + " subreddit").setNSFW(subreddit.isNsfw()).complete());
    }
}