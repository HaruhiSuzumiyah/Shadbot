package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.image.giphy.Data;
import com.shadorc.shadbot.api.json.image.giphy.GiphyResponse;
import com.shadorc.shadbot.api.json.image.giphy.Images;
import com.shadorc.shadbot.api.json.image.giphy.Original;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.NetUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class GifCmd extends BaseCmd {

    private static final String API_URL = "https://api.giphy.com/v1/gifs";
    private static final String RENDOM_ENDPOINT = String.format("%s/random", API_URL);
    private static final String SEARCH_ENDPOINT = String.format("%s/search", API_URL);

    public GifCmd() {
        super(CommandCategory.IMAGE, "gif", "Search random GIF on Giphy");
        this.addOption("query", "Search for a GIF", false, ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        final Optional<String> query = context.getOption("query");
        return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading gif...", context.getAuthorName())
                .flatMap(messageId -> GifCmd.getGifUrl(query.orElse(""))
                        .flatMap(gifUrl -> context.editFollowupMessage(messageId,
                                ShadbotUtil.getDefaultEmbed(spec -> spec.setImage(gifUrl))))
                        .switchIfEmpty(context.editFollowupMessage(messageId,
                                Emoji.MAGNIFYING_GLASS + " (**%s**) No gifs were found for the search `%s`",
                                context.getAuthorName(), query.orElse("random search"))));
    }

    private static Mono<String> getGifUrl(String query) {
        final String apiKey = CredentialManager.getInstance().get(Credential.GIPHY_API_KEY);
        final String encodedQuery = Objects.requireNonNull(NetUtil.encode(query));

        final String url;
        if (encodedQuery.isBlank()) {
            url = String.format("%s?api_key=%s",
                    RENDOM_ENDPOINT, apiKey);
        } else {
            url = String.format("%s?api_key=%s&q=%s&limit=1&offset=%d",
                    SEARCH_ENDPOINT, apiKey, encodedQuery, ThreadLocalRandom.current().nextInt(25));
        }

        return RequestHelper.fromUrl(url)
                .to(GiphyResponse.class)
                .flatMapIterable(GiphyResponse::getData)
                .next()
                .map(Data::getImages)
                .map(Images::getOriginal)
                .map(Original::getUrl);
    }

}
