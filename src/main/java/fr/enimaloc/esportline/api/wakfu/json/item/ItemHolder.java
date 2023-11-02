package fr.enimaloc.esportline.api.wakfu.json.item;

import fr.enimaloc.esportline.api.wakfu.json.WakfuJSON;
import fr.enimaloc.esportline.api.wakfu.WakfuLocale;
import fr.enimaloc.esportline.api.wakfu.json.global.GraphicParameters;
import fr.enimaloc.esportline.api.wakfu.json.global.Rarity;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public record ItemHolder(Definition definition, Map<WakfuLocale, String> title, Map<WakfuLocale, String> description) implements Item {
    @Override
    public int id() {
        return definition().item().id();
    }

    @Override
    public Rarity rarity() {
        return definition().item().baseParameters().rarity();
    }

    @Override
    public int level() {
        return definition().item().level();
    }

    @Override
    public int gfxId() {
        return definition().item().graphicParameters().gfxId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ItemHolder that = (ItemHolder) o;

        if (id() != that.id()) return false;
        if (level() != that.level()) return false;
        if (!title.equals(that.title)) return false;
        if (!description.equals(that.description)) return false;
        if (!definition.equals(that.definition)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = definition.hashCode();
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + level();
        result = 31 * result + id();
        return result;
    }

    public boolean isFromSameSet(ItemHolder itemHolder) {
        return definition().item().baseParameters().isFromSameSet(itemHolder);
    }

    public record Definition(Item item, EffectHolder[] useEffects, EffectHolder[] useCriticalEffects, EffectHolder[] equipEffects) {
        public record Item(int id, int level, BaseParameters baseParameters, UseParameters useParameters, GraphicParameters graphicParameters, int[] properties, SublimationParameters sublimationParameters, ShardsParameters shardsParameters) {
            public record BaseParameters(int itemTypeId, int itemSetId, Rarity rarity, int bindType, int minimumShardSlotNumber, int maximumShardSlotNumber) {
                public Optional<ItemHolder[]> itemSet(WakfuJSON client) {
                    if (itemSetId() == 0) return Optional.empty();
                    return Optional.of(client.getEquipmentItems()
                            .stream()
                            .flatMap(Arrays::stream)
                            .filter(itemHolder -> itemHolder.definition().item().baseParameters().itemSetId() == itemSetId())
                            .toArray(ItemHolder[]::new));
                }

                public boolean isFromSameSet(ItemHolder itemHolder) {
                    return isFromSameSet(itemHolder.definition().item().baseParameters());
                }

                public boolean isFromSameSet(BaseParameters item) {
                    return itemTypeId() != 0 && item.itemTypeId() != 0
                            && itemSetId() == item.itemSetId();
                }
            }
            public record UseParameters(int useCostAp, int useCostMp, int useCostWp, int useRangeMin, int useRangeMax, int useWorldTarget,
                                        boolean useTestFreeCell, boolean useTestLos, boolean useTestOnlyLine, boolean useTestNoBorderCell) {}

            public record SublimationParameters(int[] slotColorPattern, boolean isEpic, boolean isRelic) {
                @Override
                public String toString() {
                    return "SublimationParameters{" +
                            "slotColorPattern=" + Arrays.toString(slotColorPattern()) +
                            ", isEpic=" + isEpic() +
                            ", isRelic=" + isRelic() +
                            '}';
                }
            }

            public record ShardsParameters(int color, int[] doubleBonusPosition, int[] shardLevelingCurve, int[] shardLevelRequirement) {}

            @Override
            public String toString() {
                return "Item{" +
                        "id=" + id() +
                        ", level=" + level() +
                        ", baseParameters=" + baseParameters() +
                        ", useParameters=" + useParameters() +
                        ", graphicParameters=" + graphicParameters() +
                        ", properties=" + Arrays.toString(properties()) +
                        ", sublimationParameters=" + sublimationParameters() +
                        ", shardsParameters=" + shardsParameters() +
                        '}';
            }
        }

    }

}
