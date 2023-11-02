package fr.enimaloc.esportline.commands.context;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import fr.enimaloc.enutils.jda.JDAEnutils;
import fr.enimaloc.enutils.jda.commands.MessageContextInteractionEvent;
import fr.enimaloc.enutils.jda.register.annotation.Command;
import fr.enimaloc.enutils.jda.register.annotation.Context;
import fr.enimaloc.enutils.jda.register.annotation.I18n;
import fr.enimaloc.esportline.utils.PaginationMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.io.IOException;
import java.text.DateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class EventCreator {

    private final OpenAiService openAi;
    private final ObjectMapper mapper = new ObjectMapper();

    public EventCreator(String openAiToken) {
        this.openAi = new OpenAiService(openAiToken);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.registerModule(new JavaTimeModule());
    }

    record GeneratedEvent(String name, OffsetDateTime start, OffsetDateTime end, String inscription, String description,
                          @JsonIgnore Message.Attachment attachment) {}

    @Context(i18n = @I18n(locales = {
            @I18n.Locale(language = DiscordLocale.FRENCH, value = "Créer un évènement"),
            @I18n.Locale(language = DiscordLocale.ENGLISH_UK, value = "Create an event"),
            @I18n.Locale(language = DiscordLocale.ENGLISH_US, value = "Create an event")
    }))
    @Command.RequiredPermission(Permission.MANAGE_EVENTS)
    public void generateEventFromMessage(MessageContextInteractionEvent interaction) throws IOException {
        interaction.deferReply(true).queue();
        List<Message.Attachment> attachments = interaction.getTarget().getAttachments().stream().filter(Message.Attachment::isImage).toList();
        List<GeneratedEvent> baseChoice = askGPT(interaction.getTarget().getContentRaw())
                .stream()
                .map(ChatCompletionChoice::getMessage)
                .map(ChatMessage::getContent)
                .map(json -> {
                    try {
                        return mapper.readValue(json, GeneratedEvent.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).toList();
        List<GeneratedEvent> generatedEvents = new ArrayList<>();
        if (attachments.isEmpty()) {
            generatedEvents.addAll(baseChoice);
        } else {
            for (GeneratedEvent event : baseChoice) {
                for (Message.Attachment attachment : attachments) {
                    generatedEvents.add(new GeneratedEvent(event.name(), event.start(), event.end(), event.inscription(), event.description(), attachment));
                }
            }
        }

        PaginationMessage<GeneratedEvent> paginationMessage = new PaginationMessage(generatedEvents.toArray(GeneratedEvent[]::new), 1);
        paginationMessage.displayEmbed(interaction.getHook(), events -> {
            GeneratedEvent event = events[0];
            return new EmbedBuilder()
                    .setTitle("Generated event")
                    .addField("Name", event.name(), false)
                    .addField("Start", TimeFormat.DATE_TIME_LONG.format(event.start().toEpochSecond() * 1000) + " [" + event.start() + "]", false)
                    .addField("End", TimeFormat.DATE_TIME_LONG.format(event.end().toEpochSecond() * 1000) + " [" + event.end() + "]", false)
                    .addField("Inscription", event.inscription(), false)
                    .addField("Description", event.description(), false)
                    .setImage(event.attachment() != null ? event.attachment().getProxyUrl() : null)
                    .build();
        }, ActionRow.of(
                interaction.buildComponent().button().primary("confirm", "Confirm").withCallback(e -> {
                    GeneratedEvent event = paginationMessage.getPage()[0];
                    try {
                        interaction.getGuild().createScheduledEvent(
                                        event.name(),
                                        event.inscription(),
                                        event.start(),
                                        event.end()
                                )
                                .setDescription(event.description())
                                .setImage(event.attachment() != null ? event.attachment().getProxy().downloadAsIcon().get() : null)
                                .queue(success -> e.reply("Event created").queue(), throwable -> JDAEnutils.DEFAULT_EXCEPTION_HANDLER.accept(throwable, e.getHook(), e));
                    } catch (InterruptedException | ExecutionException ex) {
                        JDAEnutils.DEFAULT_EXCEPTION_HANDLER.accept(ex, e.getHook(), e);
                    }
                })
        ));
    }

    private List<ChatCompletionChoice> askGPT(String messageContent) {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo-0613")
                .messages(List.of(new ChatMessage(ChatMessageRole.SYSTEM.value(),
                        "Tu est un community manager dans l'association d'esport nommé \"esportline\", tu dois rédiger un évènement pour Discord.\n" +
                                "\n" +
                                "Cet fonctionnalité doit contenir:\n" +
                                "- le nom de l'évènement;\n" +
                                "- la date de début et de fin de l'évènement sous forme de date JSR310 en prenant compte la zone id suivante " + ZoneId.systemDefault() + ";\n" +
                                "- un lieu ou un lien d'inscription à l'évènement;\n" +
                                "- une description de cette évènement.\n" +
                                "\n" +
                                "La description doit respecter les points suivants:\n" +
                                "- doit contenir les grands point de l’événement;\n" +
                                "- ne pas ce répéter avec les autres points de la liste;\n" +
                                "- être sous forme de liste à point;\n" +
                                "- utilisant du markdown;\n" +
                                "- pour les retour a la ligne utilise le caractère \\n\n" +
                                "- tu ne dois pas formatter les liens;\n" +
                                "- ne dois pas dépasser 1000 caractères;\n" +
                                "- POINT IMPORTANT: ne doit pas être égale au contenu du message et dois contenir uniquement la DESCRIPTION.\n" +
                                "\n" +
                                "Note: La date d'aujourd'hui est " + DateFormat.getDateInstance(0).format(System.currentTimeMillis()) + " donc si le message ne contient pas d'année ou de mois prends cela\n" +
                                "\n" +
                                "\n" +
                                "Remplis moi cela soit forme de json comme tel: {\"name\": \"%nomDeLEvenement%\", \"start\": %startTimestamp%, \"end\": %endTimestamp%, \"inscription\": \"%lienDinscription%\",  \"description\": \"%description%\"}.\n"
                ), new ChatMessage(ChatMessageRole.USER.value(), messageContent)))
                .build();
        return openAi.createChatCompletion(request).getChoices();
    }
}
