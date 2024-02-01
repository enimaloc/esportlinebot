package fr.enimaloc.esportline.utils;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class PaginationMessage<T> {
    public static final Logger LOGGER = LoggerFactory.getLogger(PaginationMessage.class);

    private final T[] lines;
    private final int maxLinesPerPage;
    private final UUID uuid = UUID.randomUUID();
    private int page = 1;

    // region Buttons
    private BiFunction<PaginationMessage<T>, String, Button> firstButton = (self, id) -> Button.primary(id, "First").withDisabled(self.page == 1);
    private BiFunction<PaginationMessage<T>, String, Button> previousButton = (self, id) -> Button.primary(id, "Previous").withDisabled(self.page == 1);
    private BiFunction<PaginationMessage<T>, String, Button> pageButton = (self, id) -> Button.secondary(id, "Page " + self.page + "/" + self.getPageCount()).asDisabled();
    private BiFunction<PaginationMessage<T>, String, Button> nextButton = (self, id) -> Button.primary(id, "Next").withDisabled(self.page == getPageCount());
    private BiFunction<PaginationMessage<T>, String, Button> lastButton = (self, id) -> Button.primary(id, "Last").withDisabled(self.page == getPageCount());
    // endregion

    protected Function<T[], MessageEditData> lastMessageBuilder;
    protected LayoutComponent[] lastComponents;
    protected Function<T[], FileUpload[]> lastFiles;

    public PaginationMessage(T[] lines, int maxLinesPerPage) {
        this.lines = lines;
        this.maxLinesPerPage = maxLinesPerPage;
    }

    public T[] getPage() {
        int start = (page - 1) * maxLinesPerPage;
        int end = Math.min(start + maxLinesPerPage, lines.length);
        return java.util.Arrays.copyOfRange(lines, start, end);
    }

    public PaginationMessage<T> setBundle(ResourceBundle bundle) {
        return setBundle(bundle, "pagination.first", "pagination.previous", "pagination.page", "pagination.next", "pagination.last");
    }

    public PaginationMessage<T> setBundle(ResourceBundle bundle,
                                       String firstKey,
                                       String previousKey,
                                       String pageKey,
                                       String nextKey,
                                       String lastKey) {
        return setButtons(
                (self, id) -> Button.primary(id, bundle.getString(firstKey)).withDisabled(self.page == 1),
                (self, id) -> Button.primary(id, bundle.getString(previousKey)).withDisabled(self.page == 1),
                (self, id) -> Button.secondary(id, bundle.getString(pageKey).formatted(self.page, self.getPageCount())).asDisabled(),
                (self, id) -> Button.primary(id, bundle.getString(nextKey)).withDisabled(self.page == getPageCount()),
                (self, id) -> Button.primary(id, bundle.getString(lastKey)).withDisabled(self.page == getPageCount())
        );
    }

    public PaginationMessage setButtons(
            BiFunction<PaginationMessage<T>, String, Button> firstButton,
            BiFunction<PaginationMessage<T>, String, Button> previousButton,
            BiFunction<PaginationMessage<T>, String, Button> pageButton,
            BiFunction<PaginationMessage<T>, String, Button> nextButton,
            BiFunction<PaginationMessage<T>, String, Button> lastButton
    ) {
        this.firstButton = firstButton != null ? firstButton : this.firstButton;
        this.previousButton = previousButton != null ? previousButton : this.previousButton;
        this.pageButton = pageButton != null ? pageButton : this.pageButton;
        this.nextButton = nextButton != null ? nextButton : this.nextButton;
        this.lastButton = lastButton != null ? lastButton : this.lastButton;
        return this;
    }

    public int getPageCount() {
        return (lines.length + maxLinesPerPage - 1) / maxLinesPerPage;
    }

    public int getPageNumber() {
        return page;
    }

    public void firstPage() {
        page = 1;
    }

    public void lastPage() {
        page = getPageCount();
    }

    public void nextPage() {
        page = Math.min(page + 1, getPageCount());
    }

    public void previousPage() {
        page = Math.max(page - 1, 1);
    }

    public void setPage(int page) {
        this.page = Math.max(Math.min(page, getPageCount()), 1);
    }

    public void setPageRelative(int offset) {
        setPage(page + offset);
    }

    public void displayEmbed(InteractionHook hook, Function<T[], MessageEmbed> embedBuilder, LayoutComponent... components) {
        this.displayEmbed(hook, embedBuilder, components, null);
    }

    public void displayEmbed(InteractionHook hook, Function<T[], MessageEmbed> embedBuilder, Function<T[], FileUpload[]> files) {
        this.displayEmbed(hook, embedBuilder, null, files);
    }

    public void displayEmbed(InteractionHook hook, Function<T[], MessageEmbed> embedBuilder, LayoutComponent[] components, Function<T[], FileUpload[]> files) {
        this.displayMessage(hook, page -> new MessageEditBuilder().setEmbeds(embedBuilder.apply(page)).build(), components, files);
    }

    public void display(InteractionHook hook, LayoutComponent... components) {
        this.display(hook, components, null);
    }

    public void display(InteractionHook hook, Function<T[], FileUpload[]> files) {
        this.display(hook, (LayoutComponent[]) null, files);
    }

    public void display(InteractionHook hook, LayoutComponent[] components, Function<T[], FileUpload[]> files) {
        this.display(hook, String::valueOf, components, files);
    }

    public void display(InteractionHook hook, Function<T, CharSequence> mapper, LayoutComponent... components) {
        this.display(hook, mapper, components, null);
    }

    public void display(InteractionHook hook, Function<T, CharSequence> mapper, Function<T[], FileUpload[]> files) {
        this.display(hook, mapper, null, files);
    }

    public void display(InteractionHook hook, Function<T, CharSequence> mapper, LayoutComponent[] components, Function<T[], FileUpload[]> files) {
        this.displayMessage(hook, page -> new MessageEditBuilder()
                .setContent(String.join("\n", Arrays.stream(page)
                        .map(mapper::apply)
                        .toArray(CharSequence[]::new)))
                .build(), components, files);
    }

    public void displayMessage(InteractionHook hook, Function<T[], MessageEditData> messageBuilder, LayoutComponent... components) {
        displayMessage(hook, messageBuilder, components, null);
    }

    public void displayMessage(InteractionHook hook, Function<T[], MessageEditData> messageBuilder, Function<T[], FileUpload[]> files) {
        displayMessage(hook, messageBuilder, null, files);
    }

    public void displayMessage(InteractionHook hook, Function<T[], MessageEditData> messageBuilder, LayoutComponent[] components, Function<T[], FileUpload[]> files) {
        List<LayoutComponent> componentList = new ArrayList<>();
        componentList.add(ActionRow.of(
                firstButton.apply(this, "pagination.first@" + uuid),
                previousButton.apply(this, "pagination.previous@" + uuid),
                pageButton.apply(this, "pagination.page@" + uuid),
                nextButton.apply(this, "pagination.next@" + uuid),
                lastButton.apply(this, "pagination.last@" + uuid)
        ));
        if (components != null) {
            componentList.addAll(Arrays.asList(components));
        }

        lastMessageBuilder = messageBuilder;
        lastFiles = files;
        hook.editOriginal(messageBuilder.apply(getPage()))
                .setComponents(componentList)
                .setFiles(files.apply(getPage()))
                .queue(ignored -> {
                }, Throwable::printStackTrace);
        PaginationListener listener = hook.getJDA()
                .getEventManager()
                .getRegisteredListeners()
                .stream()
                .filter(PaginationListener.class::isInstance)
                .map(PaginationListener.class::cast)
                .findFirst()
                .orElseThrow();
        if (!listener.containsPagination(uuid)) {
            listener.addPagination(uuid, this);
        }
    }

    @Override
    public String toString() {
        return "PaginationMessage{" +
                "lines=" + Arrays.toString(lines) +
                ", maxLinesPerPage=" + maxLinesPerPage +
                ", uuid=" + uuid +
                ", page=" + page +
                '}';
    }

    public static class PaginationListener extends ListenerAdapter {

        private final Map<UUID, PaginationMessage> messages = new HashMap<>();

        public void addPagination(UUID uuid, PaginationMessage message) {
            messages.put(uuid, message);
        }

        public boolean containsPagination(UUID uuid) {
            return messages.containsKey(uuid);
        }
        @Override
        public void onButtonInteraction(ButtonInteractionEvent event) {
            super.onButtonInteraction(event);
            if (!event.getComponentId().startsWith("pagination")
                    || !event.getComponentId().contains("@")
                    || !messages.containsKey(UUID.fromString(event.getComponentId().split("@")[1]))
            ) {
                return;
            }
            String id = event.getComponentId().split("\\.")[1];
            PaginationMessage message = messages.get(UUID.fromString(id.split("@")[1]));
            switch (id.split("@")[0]) {
                case "first":
                    message.firstPage();
                    break;
                case "previous":
                    message.previousPage();
                    break;
                case "next":
                    message.nextPage();
                    break;
                case "last":
                    message.lastPage();
                    break;
                case "page":
                    event.reply("This button should be disabled !").setEphemeral(true).queue();
                    LOGGER.warn("Page button not disabled ! {}", message);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + event.getComponentId().split("-")[0]);
            }
            message.displayMessage(event.deferEdit().complete(), message.lastMessageBuilder, message.lastComponents, message.lastFiles);
        }

    }
}
