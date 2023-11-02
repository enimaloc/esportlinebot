package fr.enimaloc.esportline.commands.slash.game.wakfu;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.enimaloc.enutils.jda.commands.CustomCommandAutoCompleteInteractionEvent;
import fr.enimaloc.enutils.jda.commands.GlobalSlashCommandEvent;
import fr.enimaloc.enutils.jda.commands.GuildSlashCommandEvent;
import fr.enimaloc.enutils.jda.register.annotation.Catch;
import fr.enimaloc.enutils.jda.register.annotation.MethodTarget;
import fr.enimaloc.enutils.jda.register.annotation.On;
import fr.enimaloc.enutils.jda.register.annotation.Slash;
import fr.enimaloc.esportline.api.wakfu.WakfuBreeds;
import fr.enimaloc.esportline.api.wakfu.WakfuClient;
import fr.enimaloc.esportline.api.wakfu.WakfuConstant;
import fr.enimaloc.esportline.api.wakfu.WakfuLocale;
import fr.enimaloc.esportline.api.wakfu.json.CollectibleResource;
import fr.enimaloc.esportline.api.wakfu.json.Resource;
import fr.enimaloc.esportline.api.wakfu.json.WakfuJSON;
import fr.enimaloc.esportline.api.wakfu.json.item.EffectHolder;
import fr.enimaloc.esportline.api.wakfu.json.item.Item;
import fr.enimaloc.esportline.api.wakfu.json.item.ItemHolder;
import fr.enimaloc.esportline.api.wakfu.json.marker.Gfx;
import fr.enimaloc.esportline.api.wakfu.json.recipe.Recipe;
import fr.enimaloc.esportline.api.wakfu.rss.WakfuRSS;
import fr.enimaloc.esportline.utils.BundleUtils;
import fr.enimaloc.esportline.utils.PaginationMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.sf.image4j.codec.ico.ICODecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: 02/11/23 More i18n and argument name
public class Wakfu {
    public static final Logger LOGGER = LoggerFactory.getLogger(Wakfu.class);
    // TODO: 02/11/23 Put this in config file
    public static final long PARTY_CATEGORY_ID = 1169007516164825128L;
    public static final long PARTY_ANNOUNCEMENT_CHANNEL_ID = 1169007658150416464L;

    private final WakfuClient wakfuClient = new WakfuClient();
    private final Optional<RichCustomEmoji> ECNBI_EMOJI, ECNBE_EMOJI;
    private boolean fullyLoaded = false;

    public Wakfu(JDA jda) {
        Thread thread = Executors.defaultThreadFactory().newThread(() -> {
            wakfuClient.getJsonRepository().load();
            fullyLoaded = true;
            jda.getPresence().setPresence(OnlineStatus.ONLINE, null);
        });
        thread.setName("Wakfu data loader");
        thread.start();
        ECNBI_EMOJI = jda.getEmojisByName("wakfu_ecnbi", true)
                .stream()
                .findFirst()
                .or(() -> jda.getEmojisByName("ecnbi", true).stream().findFirst());
        ECNBE_EMOJI = jda.getEmojisByName("wakfu_ecnbe", true)
                .stream()
                .findFirst()
                .or(() -> jda.getEmojisByName("ecnbe", true).stream().findFirst());
        updateDeletionQueue(jda);
        Executors.newSingleThreadScheduledExecutor()
                .schedule(() -> {
                    jda.getEmojis().stream()
                            .filter(emoji -> emoji.getName().startsWith("perm_"))
                            .forEach(emoji -> emoji.delete().queue());
                    List<Map.Entry<String, Integer>> mostUsedEmojis = emojiUsage.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                            .toList();
                    int i = 0;
                    for (Map.Entry<String, Integer> mostUsedEmoji : mostUsedEmojis) {
                        Optional<RichCustomEmoji> emoji = jda.getEmojisByName(mostUsedEmoji.getKey(), false).stream().findFirst();
                        if (emoji.isPresent() && i <= 10) {
                            emoji.get().getManager().setName("perm_" + mostUsedEmoji.getKey()).queue();
                            i++;
                        }
                    }
                    LOGGER.info("Saving {} most used emojis, next update in 6 hours", i);
                }, 6, TimeUnit.HOURS);
    }

    // region Commands /game wakfu items
    @Slash.Sub
    public void items(GlobalSlashCommandEvent event,
                      @Slash.Option(name = "item") @Slash.Option.AutoCompletion(target = @MethodTarget(method = "autocompItems")) String itemId,
                      @Slash.Option(name = "lang") @Slash.Option.AutoCompletion(array = {"en", "fr", "es", "pt"}) Optional<String> lang) throws IOException {
        String bundleLang = lang.orElse(event.getLocale(true).getLocale());
        if (bundleLang.equals("en")) {
            bundleLang = Locale.ENGLISH.toLanguageTag();
        }
        if (itemId.equals("-1")) {
            event.replyEphemeral(BundleUtils.get(getBundle(lang.map(DiscordLocale::from).orElse(event.getLocale(true))),
                            fullyLoaded ? "data.loaded" : "data.loading"))
                    .queue();
            return;
        }
        event.deferReply().queue();
        WakfuLocale[] locales = new WakfuLocale[]{
                lang.map(WakfuLocale::fromCode).orElse(null),
                WakfuLocale.fromLocale(event.getUserLocale().toLocale()),
                WakfuLocale.fromLocale(event.getGuildLocale().toLocale()),
                WakfuLocale.ENGLISH
        };
        buildItemEmbed(event, itemId, locales, getBundle(Locale.forLanguageTag(bundleLang)));
    }

    private void buildItemEmbed(GlobalSlashCommandEvent event, String itemId, WakfuLocale[] locales, ResourceBundle bundle) {
        Item item = wakfuClient.getJsonRepository().getItem(Integer.parseInt(itemId)).orElseThrow();
        EmbedBuilder builder = getDefaultEmbed()
                .setTitle(getOrRetry(item.title(), locales))
                .setDescription(getOrRetry(item.description(), locales))
                .setColor(item.rarity().getColor())
                .setThumbnail("attachment://item.png");

        List<Item> relatedItems = new ArrayList<>();

        if (item.recipe(wakfuClient.getJsonRepository()).isPresent()) {
            Recipe recipe = item.recipe(wakfuClient.getJsonRepository()).get();
            builder.addField(bundle.getString("recipe"),
                    getOrRetry(recipe.category(wakfuClient.getJsonRepository()).title(), locales) + "\n" +
                            Arrays.stream(recipe.ingredients(wakfuClient.getJsonRepository()))
                                    .map(ingredient -> "`x%-4d` %s %s".formatted(ingredient.quantity(),
                                            uploadEmoji(event.getJDA(), ingredient.item(wakfuClient.getJsonRepository())).map(CustomEmoji::getAsMention).orElse(""),
                                            getOrRetry(ingredient.item(wakfuClient.getJsonRepository()).title(), locales)))
                                    .collect(Collectors.joining("\n")),
                    item.recipesRelated(wakfuClient.getJsonRepository()).length != 0);
        }
        if (item.recipesRelated(wakfuClient.getJsonRepository()).length != 0) {
            builder.addField(BundleUtils.get(bundle, "recipe.related"),
                    Arrays.stream(item.recipesRelated(wakfuClient.getJsonRepository()))
                            .map(ri -> ri.recipe(wakfuClient.getJsonRepository()))
                            .map(r -> r.result(wakfuClient.getJsonRepository()))
                            .distinct()
                            .map(rr -> rr.item(wakfuClient.getJsonRepository()))
                            .map(i -> uploadEmoji(event.getJDA(), i).map(CustomEmoji::getAsMention).orElse("") + " " + getOrRetry(i.title(), locales))
                            .collect(Collectors.joining("\n")),
                    false);
        }

        if (item instanceof ItemHolder equipment) {
            StringJoiner joiner = new StringJoiner("\n");
            // region Set
            Optional<ItemHolder[]> setOpt = equipment.definition().item().baseParameters().itemSet(wakfuClient.getJsonRepository());
            if (setOpt.isPresent()) {
                relatedItems.addAll(Arrays.stream(setOpt.get()).collect(Collectors.toList()));
                joiner.add(Arrays.stream(setOpt.get())
                        .map(i -> uploadEmoji(event.getJDA(), i).map(CustomEmoji::getAsMention).orElse("") + " " + getOrRetry(i.title(), locales))
                        .collect(Collectors.joining("\n")));
                builder.addField(BundleUtils.get(bundle, "set"), joiner.toString(), true);
                joiner = new StringJoiner("\n");
            }
            // region Effects
            for (Map.Entry<String, EffectHolder[]> entry : List.of(
                    Map.entry(BundleUtils.get(bundle, "use_effects"), equipment.definition().useEffects()),
                    Map.entry(BundleUtils.get(bundle, "use_critical_effects"), equipment.definition().useCriticalEffects()),
                    Map.entry(BundleUtils.get(bundle, "equip_effects"), equipment.definition().equipEffects())
            )) {
                for (EffectHolder holder : entry.getValue()) {
                    Map[] elements = new Map[6];
                    for (int i = 0; i < elements.length; i++) {
                        if (i == 4) {
                            continue;
                        }
                        Map<WakfuLocale, String> el = new HashMap<>();
                        for (int wl = 0; wl < WakfuLocale.values().length; wl++) {
                            WakfuLocale wakfuLocale = WakfuLocale.values()[wl];
                            el.put(wakfuLocale, BundleUtils.get(bundle, "element." + (i + 1)));
                        }
                        elements[i] = el;
                    }
                    String description = (String) getOrRetry(holder.effect().parsedDescription(wakfuClient.getJsonRepository(), item.level(), elements), locales);
                    if (ECNBE_EMOJI.isPresent() && description.contains("[ecnbe]")) {
                        description = description.replace("[ecnbe]", ECNBE_EMOJI.get().getAsMention());
                    }
                    if (ECNBI_EMOJI.isPresent() && description.contains("[ecnbi]")) {
                        description = description.replace("[ecnbi]", ECNBI_EMOJI.get().getAsMention());
                    }
                    joiner.add("- " + description.replaceAll("(\\d+)", "**$1**").replaceAll(":{2,}", ""));
                }
                if (joiner.length() > 0) {
                    builder.addField(entry.getKey(), joiner.toString(), true);
                    joiner = new StringJoiner("\n");
                }
            }
            // endregion
        }

        //        if (item.getRecipeWhereIngredient().length != 0) {
//            builder.addField("Ingredient of", Arrays.stream(item.getRecipeWhereIngredient())
//                            .map(JSON.RecipeIngredients::fetchRecipe)
//                            .map(JSON.Recipe::fetchResults)
//                            .flatMap(Arrays::stream)
//                            .map(JSON.RecipeResults::fetchItem)
//                            .map(item1 -> item1.title(locales))
//                            .collect(Collectors.joining("\n")),
//                    true);
//        }
//        if (item.getRecipeFrom().isPresent()) {
//            builder.addField("Recipe", Arrays.stream(item.getRecipeFrom().get().fetchRecipe().fetchIngredients())
//                            .map(ingredients -> ingredients.fetchItem().title(locales) + " x" + ingredients.quantity())
//                            .collect(Collectors.joining("\n")),
//                    true);
//        }
//
//        if (item instanceof JSON.IElement equipment) {
//            builder.setColor(equipment.definition().item().baseParameters().rarity().getColor())
//                    .setDescription(equipment.description(locales));
//            StringJoiner joiner = new StringJoiner("\n");
//            for (Map.Entry<String, JSON.IElement.IDefinition.EElement[]> entry : List.of(
//                    Map.entry("Use Effects", equipment.definition().useEffects()),
//                    Map.entry("Use Critical Effects", equipment.definition().useCriticalEffects()),
//                    Map.entry("Equip Effects", equipment.definition().equipEffects())
//            )) {
//                for (JSON.IElement.IDefinition.EElement holder : entry.getValue()) {
//                    String description = holder.effect().definition().fetchAction().description(locales);
//                    double[] params = holder.effect().definition().params();
//                    int level = equipment.definition().item().level();
//                    try {
//                        joiner.add(EffectEvaluator.formatString(description, params, level));
//                    } catch (IndexOutOfBoundsException e) {
//                        joiner.add("Unknown effect ||(" + e + ")||");
//                        new RuntimeException("description = \"" + description + "\" | " +
//                                "params = " + Arrays.toString(params) + " | " +
//                                "level = " + level, e).printStackTrace();
//                    }
//                }
//                if (joiner.length() > 0) {
//                    builder.addField(entry.getKey(), joiner.toString()
//                                    .replace("[el1]", "\uD83D\uDD25") // Fire
//                                    .replace("[el2]", "\uD83D\uDCA7") // Water
//                                    .replace("[el3]", "\uD83C\uDF0E") // Earth
//                                    .replace("[el4]", "\uD83C\uDF2C\uFE0F"), // Air
//                            false);
//                    joiner = new StringJoiner("\n");
//                }
//            }
//        }
//                .setDescription(getOrRetry(item.description(), lang.orElse(null), locales.userLocale(), locales.guildLocale(), "en"))
//                .setColor(item.definition().item().baseParameters().rarity().getColor())
//                .addField("Type", getOrRetry(item.definition().item().baseParameters().fetchItemType().title(), lang.orElse(null), locales.userLocale(), locales.guildLocale(), "en"), true);
//
        relatedItems.addAll(Arrays.stream(wakfuClient.getJsonRepository().getItems().orElseThrow())
                .filter(i -> i.title().equals(item.title())).toList());
        WebhookMessageEditAction<Message> action = event.getHook().editOriginalEmbeds(builder.build())
                .setFiles(FileUpload.fromData(wakfuClient.getAssetsRepository().getAsset(item, true).get(), "item.png"));
        List<Item> finalRelatedItems = new ArrayList<>();
        for (Item relatedItem : relatedItems) {
            if (relatedItem.id() != item.id()
                    && finalRelatedItems.stream().allMatch(fri -> fri.id() != relatedItem.id())
                    && finalRelatedItems.size() <= OptionData.MAX_CHOICES) {
                finalRelatedItems.add(relatedItem);
            }
        }
        List<SelectOption> options;
        if (finalRelatedItems.size() > 0) {
            options = finalRelatedItems.stream()
                    .limit(OptionData.MAX_CHOICES)
                    .map(i -> SelectOption.of((i instanceof ItemHolder e && item instanceof ItemHolder ih && e.isFromSameSet(ih)
                                    ? BundleUtils.get(bundle, "set.same") : "")
                                    + "[Lvl " + i.level() + "] " + getOrRetry(i.title(), locales), String.valueOf(i.id()))
                            .withDescription(getOrRetry(i.description(), locales)
                                    .replaceFirst("(.{95}).*", "$1..."))
                            .withEmoji(uploadEmoji(event.getJDA(), i).orElse(null))
                    )
                    .toList();
        } else {
            options = List.of(SelectOption.of("Non-accessible", "-1"));
        }
        action.setComponents(ActionRow.of(StringSelectMenu.create("wakfu@item[" + itemId + "|" + locales[0] + "]")
                .addOptions(options)
                .setPlaceholder(BundleUtils.get(bundle, "item.related"))
                .setMaxValues(1)
                .setDisabled(finalRelatedItems.size() == 0)
                .build()
        ));
        action.queue();
    }

    public void autocompItems(CustomCommandAutoCompleteInteractionEvent event) {
        if (!fullyLoaded) {
            event.replyChoices(getUnavailableChoice()).queue();
            return;
        }
        event.replyChoices(wakfuClient.getJsonRepository().getItems().stream()
                        .flatMap(Arrays::stream)
                        .map(element -> new Command.Choice(element instanceof ItemHolder equipment
                                ? "[Lvl %-4d] %s".formatted(equipment.definition().item().level(), getOrRetry(element.title(), WakfuLocale.fromLocale(event.getUserLocale().toLocale())))
                                : getOrRetry(element.title(), WakfuLocale.fromLocale(event.getUserLocale().toLocale())),
                                element.id())
                                .setNameLocalizations(
                                        Arrays.stream(WakfuLocale.values())
                                                .map(wakfuLocale -> Map.entry(DiscordLocale.from(wakfuLocale.toLocale()), getOrRetry(element.title(), wakfuLocale)))
                                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                                ))
                        .filter(choice -> choice.getName().toLowerCase().contains(event.getFocusedOption().getValue().toLowerCase()))
                        .limit(OptionData.MAX_CHOICES)
                        .toList())
                .queue(null, null);
    }
    // endregion

    // region Commands /game wakfu resources
    @Slash.Sub
    public void resources(GlobalSlashCommandEvent event,
                          @Slash.Option(name = "resource") @Slash.Option.AutoCompletion(target = @MethodTarget(method = "autocompResources")) String resourceId,
                          @Slash.Option(name = "lang") @Slash.Option.AutoCompletion(array = {"en", "fr", "es", "pt"}) Optional<String> lang) throws IOException {
        String bundleLang = lang.orElse(event.getLocale(true).getLocale());
        if (bundleLang.equals("en")) {
            bundleLang = Locale.ENGLISH.toLanguageTag();
        }
        if (resourceId.equals("-1")) {
            event.replyEphemeral(BundleUtils.get(getBundle(lang.map(DiscordLocale::from).orElse(event.getLocale(true))),
                            fullyLoaded ? "data.loaded" : "data.loading"))
                    .queue();
            return;
        }
        event.deferReply().queue();
        WakfuLocale[] locales = new WakfuLocale[]{
                lang.map(WakfuLocale::fromCode).orElse(null),
                WakfuLocale.fromLocale(event.getUserLocale().toLocale()),
                WakfuLocale.fromLocale(event.getGuildLocale().toLocale()),
                WakfuLocale.ENGLISH
        };
        buildResourceEmbed(event, resourceId, locales, getBundle(Locale.forLanguageTag(bundleLang)));
    }

    private void buildResourceEmbed(GlobalSlashCommandEvent event, String resourceId, WakfuLocale[] locales, ResourceBundle bundle) throws JsonProcessingException {
        Resource resource = wakfuClient.getJsonRepository().getResource(Integer.parseInt(resourceId)).orElseThrow();
        EmbedBuilder builder = getDefaultEmbed()
                .setTitle(getOrRetry(resource.title(), locales))
                .setThumbnail("attachment://resource.png");
        Optional<CollectibleResource[]> collectibles = resource.definition().getCollectibleResource(wakfuClient);
        if (collectibles.isPresent()) {
            Map<Integer, Integer> count = new HashMap<>();
            for (CollectibleResource collectible : collectibles.get()) {
                count.put(collectible.collectItemId(), count.getOrDefault(collectible.collectItemId(), 0) + 1);
            }
            List<Integer> ignore = new ArrayList<>();
            StringJoiner joiner = new StringJoiner("\n");
            for (CollectibleResource loot : collectibles.get()) {
                if (ignore.contains(loot.collectItemId())) {
                    continue;
                }
                Optional<Item> item = loot.collectItem(wakfuClient);
                if (item.isPresent()) {
                    joiner.add("- `x%-4d` %s %s".formatted(
                            count.get(loot.collectItemId()),
                            uploadEmoji(event.getJDA(), item.get()).map(CustomEmoji::getAsMention).orElse(""),
                            getOrRetry(item.get().title(), locales)));
                } else {
                    joiner.add("- `x%-4d` %s %s".formatted(
                            count.get(loot.collectItemId()),
                            EmbedBuilder.ZERO_WIDTH_SPACE,
                            "Unknown item ||(" + loot.collectItemId() + ")||"));
                }
                ignore.add(loot.collectItemId());
            }
            if (joiner.length() > 0) {
                builder.addField(BundleUtils.get(bundle, "resource.loot"), joiner.toString(), true);
            }
        }
        builder.addField(BundleUtils.get(bundle, "resource.type"),
                getOrRetry(resource.definition().resourceType(wakfuClient).title(), locales), true);
        event.getHook()
                .editOriginalEmbeds(builder.build())
                .setFiles(Stream.of(resource.gfxId() == -1 ? null : FileUpload.fromData(wakfuClient.getAssetsRepository().getAsset(resource).get(), "resource.png"),
                                FileUpload.fromData(WakfuJSON.JSON_MAPPER.writeValueAsBytes(resource), "resource.json"),
                                FileUpload.fromData(WakfuJSON.JSON_MAPPER.writeValueAsBytes(resource.definition().resourceType(wakfuClient)), "resource_type.json"),
                                FileUpload.fromData(WakfuJSON.JSON_MAPPER.writeValueAsBytes(resource.definition().getCollectibleResource(wakfuClient).orElse(new CollectibleResource[0])), "collectible_resource.json")
                        )
                        .filter(Objects::nonNull)
                        .toList())
                .queue();
    }

    public void autocompResources(CustomCommandAutoCompleteInteractionEvent event) {
        if (!fullyLoaded) {
            event.replyChoices(getUnavailableChoice()).queue();
            return;
        }
        WakfuLocale[] locales = new WakfuLocale[]{
                WakfuLocale.fromLocale(event.getUserLocale().toLocale()),
                WakfuLocale.fromLocale(event.getGuildLocale().toLocale()),
                WakfuLocale.ENGLISH
        };
        event.replyChoices(wakfuClient.getJsonRepository().getResources()
                        .stream()
                        .flatMap(Arrays::stream)
                        .map(element -> new Command.Choice(getOrRetry(element.title(), locales), element.id())
                                .setNameLocalizations(element.title()
                                        .keySet()
                                        .stream()
                                        .map(wl -> Map.entry(wl, element.title().get(wl)))
                                        .collect(Collectors.toMap(
                                                e -> DiscordLocale.from(e.getKey().toLocale()),
                                                e -> e.getValue()
                                        ))
                                ))
                        .filter(choice -> choice.getName().toLowerCase().contains(event.getFocusedOption().getValue().toLowerCase()))
                        .limit(OptionData.MAX_CHOICES)
                        .toList())
                .queue(null, null);
    }
    // endregion

    // region Commands /game wakfu news
    @Slash.Sub
    public void news(GlobalSlashCommandEvent event,
                     @Slash.Option @Slash.Option.AutoCompletion(array = {"en", "fr", "es", "pt"}) Optional<String> lang) throws IOException {
        String bundleLang = lang.orElse(event.getLocale(true).getLocale());
        WakfuLocale wakfuLocale = WakfuLocale.fromCode(lang.orElse(event.getLocale(true).getLocale()));
        if (wakfuLocale == null) {
            wakfuLocale = WakfuLocale.ENGLISH;
        }
        event.deferReply().queue();
        WakfuRSS.Channel channel = wakfuClient.getRssFlux(wakfuLocale).channel;

        String authorLogoUrl;
        FileUpload file;
        if (channel.url().endsWith(".ico")) {
            file = convertToPng(channel.url(), "author.png");
            authorLogoUrl = "attachment://author.png";
        } else {
            file = null;
            authorLogoUrl = channel.url();
        }

        new PaginationMessage<>(channel.item(), 1)
                .setBundle(getBundle(Locale.forLanguageTag(bundleLang)),
                        "news.pagination.first",
                        "news.pagination.previous",
                        "news.pagination.page",
                        "news.pagination.next",
                        "news.pagination.last"
                )
                .displayEmbed(event.getHook(), news -> {
                            Matcher matcher = IMG_TAG.matcher(news[0].description());
                            Optional<String> firstImage;
                            if (matcher.find()) {
                                firstImage = Optional.of(matcher.group(1));
                            } else {
                                firstImage = Optional.empty();
                            }
                            return getDefaultEmbed()
                                    .setTitle(news[0].title())
                                    .setUrl(news[0].link())
                                    .setDescription(parseHtmlToMarkdown(news[0].description()).replaceFirst("((?s:.){" + (MessageEmbed.DESCRIPTION_MAX_LENGTH - 5) + "})(?s:.*)", "$1..."))
                                    .setTimestamp(news[0].pubDate().toInstant())
                                    .setAuthor(channel.title(), channel.link(), authorLogoUrl)
                                    .setImage(firstImage.orElse(null))
                                    .build();
                        },
                        news -> new FileUpload[]{file});
    }

    Pattern IMG_TAG = Pattern.compile("<img\\s[^>]*?src\\s*=\\s*['\\\"]([^'\\\"]*?)['\\\"][^>]*?>");

    private FileUpload convertToPng(String url, String name) throws IOException {
        URL url0 = new URL(url);
        List<BufferedImage> decoder = ICODecoder.read(url0.openStream());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(decoder.get(0), "png", baos);
        FileUpload fileUpload = FileUpload.fromData(baos.toByteArray(), name);
        return fileUpload;
    }

    private String parseHtmlToMarkdown(String description) {
        return parseHtmlToMarkdown(description, 5);
    }

    private String parseHtmlToMarkdown(String source, int i) {
        for (Map.Entry<String, String> entry : WakfuConstant.HTML_CHARSET.entrySet()) {
            source = source.replace(entry.getKey(), entry.getValue());
        }
        if (i == 5) {
            source = source.replaceAll("\n", "");
        }
        source = source.replaceAll("<iframe .*src=\\\"(?:http://)?([^\\\"]*)?\\\".*>(?:</iframe>)?", "\n[[! External embed]](http://$1)")
                .replaceAll("<img .*src=\\\"([^\\\"]*)?\\\".*>(?:</img>)?", "\n[[! Image]]($1)")
                .replaceAll("<a .*href=\\\"([^\\\"]*)?\\\".*>([^<]*)</a>", "[$2]($1)")
                .replaceAll("<br(?: /)?>", "\n")
                .replaceAll("<strong>([^<]*)</strong>", "**$1**")
                .replaceAll("<b>([^<]*)</b>", "**$1**")
                .replaceAll("<em>([^<]*)</em>", "*$1*")
                .replaceAll("<i>([^<]*)</i>", "*$1*")
                .replaceAll("<u>([^<]*)</u>", "__$1__")
                .replaceAll("<strike>([^<]*)</strike>", "~~$1~~")
                .replaceAll("<code>([^<]*)</code>", "`$1`")
                .replaceAll("<blockquote>([^<]*)</blockquote>", "> $1")
                .replaceAll("<ul>([^<]*)</ul>", "$1")
                .replaceAll(" *<li>([^<]*)</li>", "- $1\n")
                .replaceAll("</?div[^>]*>", "")
                .replaceAll("<h1>([^<]*)</h1>", "\n# $1\n")
                .replaceAll("<h2>([^<]*)</h2>", "\n## $1\n")
                .replaceAll("<h3>([^<]*)</h3>", "\n### $1\n")
                .replaceAll("<p[^>]*>([^<]*)</p>", "$1\n\n")
                .replaceAll("<span[^>]*>([^<]*)</span>", "$1")
                .replaceAll("<!-- .* -->", "");
        return i == 0 ? source : parseHtmlToMarkdown(source, i - 1);
    }
    // endregion

    // region Commands /game wakfu party

    @Slash.Sub
    public void partyCreate(GuildSlashCommandEvent event,
                            @Slash.Option(name = "party-name") String partyName,
                            @Slash.Option(name = "slots") int slots,
                            @Slash.Option(name = "description") String description,
                            @Slash.Option(name = "level") OptionalInt level) {
        if (WakfuParty.REGISTERED_PARTIES.stream().anyMatch(p -> p.getOwnerId() == event.getMember().getIdLong())) {
            event.deferReplyEphemeral().queue();
            event.getHook().editOriginal("You are already the owner of a party").queue();
            return;
        }
        if (WakfuParty.REGISTERED_PARTIES.stream().anyMatch(p -> p.getName().equals(partyName))) {
            event.deferReplyEphemeral().queue();
            event.getHook().editOriginal("A party with this name already exists").queue();
            return;
        }
        event.deferReplyEphemeral().queue();

        WakfuParty createdParty = new WakfuParty(event, event.getMember().getIdLong(), partyName, description, slots, level.orElse(-1));
        event.getHook().editOriginal("Party created, now select your class")
                .setComponents(ActionRow.of(
                        StringSelectMenu.create("wakfu@party[" + createdParty.getName() + "].ownerPickClass")
                                .setMaxValues(18)
                                .addOptions(WakfuBreeds.getAsOption(event.getJDA()))
                                .build()
                ))
                .queue();
    }

    @Slash.Sub
    public void partyJoin(GuildSlashCommandEvent event,
                          @Slash.Option(name = "party-name") String partyName) {
    }

    @Slash.Sub
    public void partyLeave(GuildSlashCommandEvent event,
                           @Slash.Option(name = "party-name") String partyName) {
    }

    @Slash.Sub
    public void partyList(GuildSlashCommandEvent event) {
    }

    @Slash.Sub
    public void partyEditSlot(GuildSlashCommandEvent event, @Slash.Option(name = "new-slot") @Slash.Option.Range(min = 1, max = 99) int slots) {
        event.deferReplyEphemeral().queue();
        WakfuParty.REGISTERED_PARTIES.stream().filter(p -> p.getOwnerId() == event.getMember().getIdLong()).findFirst().ifPresentOrElse(p -> {
            p.setSlots(event.getJDA(), slots);
            event.getHook().editOriginal("Party slots updated").queue();
        }, () -> event.getHook().editOriginal("You are not the owner of any party").queue());
    }

    @Slash.Sub
    public void partyDelete(GuildSlashCommandEvent event) {
        event.deferReplyEphemeral().queue();
        WakfuParty.REGISTERED_PARTIES.stream().filter(p -> p.getOwnerId() == event.getMember().getIdLong()).findFirst().ifPresentOrElse(p -> {
            p.delete(event.getJDA()).queue(unused -> {
                event.getHook().editOriginal("Party deleted").queue();
            });
        }, () -> event.getHook().editOriginal("You are not the owner of any party").queue());
    }

    @On(filter = @MethodTarget(method = "partyInteractionPredicate"))
    public void partySelect(StringSelectInteractionEvent event) {
        String name = event.getComponentId().substring(event.getComponentId().indexOf('[') + 1, event.getComponentId().indexOf(']'));
        WakfuParty.REGISTERED_PARTIES.stream().filter(p -> p.getName().equals(name)).findFirst().ifPresent(p -> p.select(event));
    }

    @On(filter = @MethodTarget(method = "partyInteractionPredicate"))
    public void partyButton(ButtonInteractionEvent event) {
        String name = event.getComponentId().substring(event.getComponentId().indexOf('[') + 1, event.getComponentId().indexOf(']'));
        WakfuParty.REGISTERED_PARTIES.stream().filter(p -> p.getName().equals(name)).findFirst().ifPresent(p -> p.button(event));
    }

    public boolean partyInteractionPredicate(GenericComponentInteractionCreateEvent event) {
        return event.getComponentId().startsWith("wakfu@party");
    }

    // endregion

    // region Catch
    @Catch
    public void catchNoElement(GlobalSlashCommandEvent event, InteractionHook hook, NoSuchElementException t) {
        hook.editOriginal("No item found with this id")
                .queue();
    }
    // endregion

    // region Utils
    private EmbedBuilder getDefaultEmbed() {
        return new EmbedBuilder().setFooter("Data from version " + wakfuClient.getJsonRepository().getCurrentVersion() + "\n" +
                "WAKFU MMORPG : © 2012-" + Calendar.getInstance().get(Calendar.YEAR) + " Ankama Studio. Tous droits réservés.");
    }

    private <K, V> V getOrRetry(Map<K, V> map, K... keys) {
        for (K k : keys) {
            if (map.containsKey(k)) {
                return map.get(k);
            }
        }
        return null;
    }

    private ResourceBundle getBundle(WakfuLocale locale) {
        return getBundle(locale.toLocale());
    }

    private ResourceBundle getBundle(DiscordLocale locale) {
        return getBundle(locale.toLocale());
    }

    private ResourceBundle getBundle(String locale) {
        return getBundle(Locale.forLanguageTag(locale));
    }

    private ResourceBundle getBundle(Locale locale) {
        return ResourceBundle.getBundle("i18n.commands.game.wakfu", locale);
    }

    private Command.Choice[] getUnavailableChoice() {
        List<Command.Choice> choices = new ArrayList<>();
        ResourceBundle defaultBundle = getBundle(DiscordLocale.ENGLISH_UK);
        for (int i = 0; i < 25; i++) {
            int finalI = i;
            choices.add(new Command.Choice(defaultBundle.getString("auto_complete." + (i + 1)), "-1")
                    .setNameLocalizations(Arrays.stream(DiscordLocale.values())
                            .map(dl -> Map.entry(dl, BundleUtils.getOr(getBundle(dl), "auto_complete." + (finalI + 1), "")))
                            .collect(Collectors.toMap(
                                    e -> DiscordLocale.from(e.getKey().toLocale()),
                                    Map.Entry::getValue
                            ))));
        }
        return choices.toArray(new Command.Choice[0]);
    }
    // endregion

    // region Emoji
    private final List<DeletionRequest> deletionQueue = new ArrayList<>();
    private final Map<String, Integer> emojiUsage = new HashMap<>();

    private void updateDeletionQueue(JDA jda) {
        List<Guild> guilds = jda.getGuildsByName("WakfuEmoji", false);
        if (guilds.isEmpty()) {
            LOGGER.warn("No guild named WakfuEmoji found, cannot update deletion queue");
        }
        Guild guild = guilds.get(0);
        if (guild.getEmojis().size() != deletionQueue.size()) {
            LOGGER.warn("Emoji count mismatch [expected {} got {}], updating deletion queue", deletionQueue.size(), guild.getEmojis().size());
            deletionQueue.clear();
            for (RichCustomEmoji emoji : guild.retrieveEmojis().complete()) {
                if (emoji.getName().startsWith("perm_") || (emoji.getOwner().getIdLong() != jda.getSelfUser().getIdLong())) {
                    continue;
                }
                long actual = System.currentTimeMillis() / 1000;
                deletionQueue.add(new DeletionRequest(actual, emoji.getIdLong()));
                emoji.delete()
                        .setCheck(() -> deletionQueue.stream().anyMatch(dr -> dr.emojiId() == emoji.getIdLong() && dr.timestamp() == actual))
                        .queueAfter(1, TimeUnit.HOURS, unused -> deletionQueue.removeIf(dr -> dr.emojiId() == emoji.getIdLong() && dr.timestamp() == actual));
            }
        }
    }

    private Optional<RichCustomEmoji> uploadEmoji(JDA jda, byte[] b, String name) {
        emojiUsage.compute(name, (s, i) -> i == null ? 1 : i + 1);
        if (jda.getEmojisByName("perm_" + name, false).stream().findFirst().isPresent()) {
            return jda.getEmojisByName("perm_" + name, false).stream().findFirst();
        }
        long actual = System.currentTimeMillis() / 1000;
        if (jda.getEmojisByName(name, false).stream().findFirst().isPresent()) {
            RichCustomEmoji emoji = jda.getEmojisByName(name, false).stream().findFirst().get();
            deletionQueue.removeIf(dr -> dr.emojiId() == emoji.getIdLong());
            deletionQueue.add(new DeletionRequest(actual, emoji.getIdLong()));
            emoji.delete()
                    .setCheck(() -> deletionQueue.stream().anyMatch(dr -> dr.emojiId() == emoji.getIdLong() && dr.timestamp() == actual))
                    .queueAfter(1, TimeUnit.HOURS, unused -> deletionQueue.removeIf(dr -> dr.emojiId() == emoji.getIdLong()));
            return jda.getEmojisByName(name, false).stream().findFirst();
        }
        List<Guild> guilds = jda.getGuildsByName("WakfuEmoji", false);
        updateDeletionQueue(jda);
        if (guilds.isEmpty()) {
            LOGGER.warn("No guild named WakfuEmoji found, cannot upload emoji");
            return Optional.empty();
        }
        Guild guild = guilds.get(0);
        if (guild.getEmojis().size() >= guild.getMaxEmojis()) {
            LOGGER.warn("Emoji limit reached, cannot upload emoji, deleting old ones");
            Collections.sort(deletionQueue, Comparator.comparingLong(DeletionRequest::timestamp).reversed());
            jda.getEmojiById(deletionQueue.get(deletionQueue.size() - 1).emojiId()).delete()
                    .onSuccess(unused -> deletionQueue.remove(deletionQueue.size() - 1))
                    .complete();
        }
        RichCustomEmoji emoji = guild
                .createEmoji(name, Icon.from(b))
                .onErrorMap(e -> {
                    LOGGER.error("Error while uploading emoji", e);
                    return null;
                })
                .complete();
        if (emoji != null) {
            deletionQueue.add(new DeletionRequest(actual, emoji.getIdLong()));
            emoji.delete()
                    .setCheck(() -> deletionQueue.stream().anyMatch(dr -> dr.emojiId() == emoji.getIdLong() && dr.timestamp() == actual))
                    .queueAfter(1, TimeUnit.HOURS, unused -> deletionQueue.removeIf(dr -> dr.emojiId() == emoji.getIdLong()));
        }
        return Optional.ofNullable(emoji);
    }

    private record DeletionRequest(long timestamp, long emojiId) {}

    private Optional<RichCustomEmoji> uploadEmoji(JDA jda, Gfx gfx) {
        return uploadEmoji(jda, wakfuClient.getAssetsRepository().getAsset(gfx, true).get(), String.valueOf(gfx.gfxId()));
    }
    // endregion

    // region Dev
    // TODO: 02/11/23 Remove that in prod or disable it
    @Slash.Sub
    public void json(GlobalSlashCommandEvent event, @Slash.Option Type type, @Slash.Option OptionalInt id) throws IOException {
        event.deferReply().queue();
        String content = switch (type) {
            case GAMEDATA ->
                    WakfuJSON.JSON_MAPPER.writeValueAsString(wakfuClient.getJsonRepository().getGamedataConfig0());
            case RESOURCES ->
                    getData(wakfuClient.getJsonRepository()::getResource, wakfuClient.getJsonRepository()::getResources, id);
            case RESOURCE_TYPES ->
                    getData(wakfuClient.getJsonRepository()::getResourceType, wakfuClient.getJsonRepository()::getResourceTypes, id);
            case STATES ->
                    getData(wakfuClient.getJsonRepository()::getState, wakfuClient.getJsonRepository()::getStates, id);
            case JOB_ITEMS ->
                    getData(wakfuClient.getJsonRepository()::getJobItem, wakfuClient.getJsonRepository()::getJobItems, id);
            case ITEM_TYPES ->
                    getData(wakfuClient.getJsonRepository()::getItemType, wakfuClient.getJsonRepository()::getItemTypes, id);
            case RECIPE_CATEGORIES ->
                    getData(wakfuClient.getJsonRepository()::getRecipeCategory, wakfuClient.getJsonRepository()::getRecipeCategories, id);
            case ITEM_PROPERTIES ->
                    getData(wakfuClient.getJsonRepository()::getItemProperty, wakfuClient.getJsonRepository()::getItemProperties, id);
            case ACTIONS ->
                    getData(wakfuClient.getJsonRepository()::getAction, wakfuClient.getJsonRepository()::getActions, id);
            case BLUEPRINTS ->
                    getData(wakfuClient.getJsonRepository()::getBlueprint, wakfuClient.getJsonRepository()::getBlueprints, id);
            case COLLECTIBLE_RESOURCES ->
                    getData(wakfuClient.getJsonRepository()::getCollectibleResource, wakfuClient.getJsonRepository()::getCollectibleResources, id);
            case HARVEST_LOOTS ->
                    getData(wakfuClient.getJsonRepository()::getHarvestLoot, wakfuClient.getJsonRepository()::getHarvestLoots, id);
            case EQUIPMENT_ITEM_TYPES ->
                    getData(wakfuClient.getJsonRepository()::getEquipmentItemType, wakfuClient.getJsonRepository()::getEquipmentItemTypes, id);
            case RECIPE_INGREDIENTS ->
                    WakfuJSON.JSON_MAPPER.writeValueAsString(id.isPresent() ? wakfuClient.getJsonRepository().getRecipeIngredient(id.getAsInt()).orElseThrow() : wakfuClient.getJsonRepository().getRecipeIngredients().orElseThrow());
            case RECIPE_RESULTS ->
                    getData(wakfuClient.getJsonRepository()::getRecipeResult, wakfuClient.getJsonRepository()::getRecipeResults, id);
            case RECIPES ->
                    getData(wakfuClient.getJsonRepository()::getRecipe, wakfuClient.getJsonRepository()::getRecipes, id);
            case ITEMS ->
                    getData(wakfuClient.getJsonRepository()::getItem, wakfuClient.getJsonRepository()::getItems, id);
        };

        String baseMessage = "```json\n%s```";
        if (baseMessage.formatted(content).length() > 2000) {
            event.getHook()
                    .editOriginal("The json is too long to be sent, it has been uploaded as a file, try again with an id to get a shorter json")
                    .setFiles(FileUpload.fromData(content.getBytes(), "data.json"))
                    .queue();
        } else {
            event.getHook()
                    .editOriginal(baseMessage.formatted(content))
                    .queue();
        }
    }

    private <T> String getData(Function<Integer, Optional<T>> f1, Supplier<Optional<T[]>> f2, OptionalInt id) throws JsonProcessingException {
        return WakfuJSON.JSON_MAPPER.writeValueAsString(id.isPresent() ? f1.apply(id.getAsInt()).orElseThrow() : f2.get().orElseThrow());
    }

    public enum Type {
        GAMEDATA,
        RESOURCES,
        RESOURCE_TYPES,
        STATES,
        JOB_ITEMS,
        ITEM_TYPES,
        RECIPE_CATEGORIES,
        ITEM_PROPERTIES,
        ACTIONS,
        BLUEPRINTS,
        COLLECTIBLE_RESOURCES,
        HARVEST_LOOTS,
        EQUIPMENT_ITEM_TYPES,
        RECIPE_INGREDIENTS,
        RECIPE_RESULTS,
        RECIPES,
        ITEMS;
    }

    // endregion
}
