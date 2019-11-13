package com.uddernetworks.reddicord.discord.command;

import com.uddernetworks.reddicord.Reddicord;
import com.uddernetworks.reddicord.discord.EmbedUtils;
import com.uddernetworks.reddicord.discord.reddicord.SubredditLink;
import com.uddernetworks.reddicord.discord.reddicord.SubredditManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

public class SubredditCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubredditCommand.class);

    private final Reddicord reddicord;
    private SubredditManager subredditManager;

    public SubredditCommand(Reddicord reddicord) {
        super("subreddit");
        this.reddicord = reddicord;
        this.subredditManager = reddicord.getSubredditManager();
    }

    @Override
    public void onCommand(Member author, TextChannel channel, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            LOGGER.info("Help!");
            subredditHelp(author, channel);
            return;
        }

        if (!author.hasPermission(Permission.ADMINISTRATOR)) {
            EmbedUtils.error(channel, author, "You must be administrator for that!");
            return;
        }

        var guild = channel.getGuild();

        switch (args[0].toLowerCase()) {
            case "list":
                EmbedUtils.sendEmbed(channel, author, "Subreddits", embedBuilder -> {
                    embedBuilder.setDescription("The following are all the subreddits in the Discord:\n\n**" +
                            subredditManager.getSubreddits(channel.getGuild()).stream()
                                    .map(SubredditLink::getSubreddit)
                                    .map("#"::concat)
                                    .collect(Collectors.joining("\n"))
                            + "**");
                });
                break;
            case "add":
                if (args.length != 2) {
                    subredditHelp(author, channel);
                    return;
                }

                var subreddit = args[1];
                if (!StringUtils.isAlphanumeric(subreddit)) {
                    EmbedUtils.error(channel, author, "That's not a valid subreddit name!");
                    return;
                }

                subredditManager.addSubreddit(guild, args[1])
                        .thenRun(() -> channel.sendMessage("Added subreddit '" + args[1] + "'").queue());
                break;
            case "remove":
                if (args.length != 2) {
                    subredditHelp(author, channel);
                    return;
                }

                subreddit = args[1];
                if (!StringUtils.isAlphanumeric(subreddit)) {
                    EmbedUtils.error(channel, author, "That's not a valid subreddit name!");
                    return;
                }

                subredditManager.removeSubreddit(guild, args[1])
                        .thenRun(() -> channel.sendMessage("Removed subreddit '" + args[1] + "'").queue());
                break;
        }
    }

    private void subredditHelp(Member author, TextChannel textChannel) {
        EmbedUtils.sendEmbed(textChannel, author, "Subreddit help", embed ->
                embed.setDescription("Help for the /subreddit command")
                        .addField("**/subreddit help**", "Show this help menu", false)
                        .addField("**/subreddit list**", "Lists the active subreddits", false)
                        .addField("**/subreddit add [subreddit]**", "Adds the given subreddit", false)
                        .addField("**/subreddit remove [subreddit]**", "Removes the given subreddit", false)
        );
    }
}
