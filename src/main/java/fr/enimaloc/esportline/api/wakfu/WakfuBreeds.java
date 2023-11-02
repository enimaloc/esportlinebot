package fr.enimaloc.esportline.api.wakfu;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public enum WakfuBreeds {
    CRA(1169041324155093072L), // 0
    ECA(1169041539662618714L), // 1
    ELIO(1169041321676259408L), // 2
    ENI(1169041537057968230L), // 3
    ENU(1169041535858389092L), // 4
    FECA(1169041316877963367L), // 5
    HUP(1169041534373605466L), // 6
    IOP(1169041531861209209L), // 7
    KILORF(1169041313681920000L), // 8
    MASK(1169041312494927932L), // 9
    OSA(1169041311261802556L), // 10
    PANDA(1169041308933947472L), // 11
    ROGUE(1169041307809878190L), // 12
    SAC(1169041306459316274L), // 13
    SADI(1169041304634789968L), // 14
    SRAM(1169041303288418424L), // 15
    STEAM(1169041301631684699L), // 16
    XEL(1169041299253514315L); // 17

    private final long emojiId;

    WakfuBreeds(long emojiId) {
        this.emojiId = emojiId;
    }

    public Emoji getEmoji(JDA jda) {
        return jda.getEmojiById(emojiId);
    }

    public static List<SelectOption> getAsOption(JDA jda) {
        return getAsOption(jda, unused -> true);
    }

    public static List<SelectOption> getAsOption(JDA jda, Predicate<WakfuBreeds> filter) {
        return Arrays.stream(WakfuBreeds.values())
                .filter(filter)
                .map(breed -> SelectOption.of(breed.name().substring(0, 1).toUpperCase() + breed.name().substring(1).toLowerCase(),
                                String.valueOf(breed.ordinal()))
                        .withEmoji(breed.getEmoji(jda)))
                .toList();
    }
}
