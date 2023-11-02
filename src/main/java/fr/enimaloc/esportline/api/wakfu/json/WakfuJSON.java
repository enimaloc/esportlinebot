package fr.enimaloc.esportline.api.wakfu.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.enimaloc.esportline.api.wakfu.CachedEntity;
import fr.enimaloc.esportline.api.wakfu.WakfuClient;
import fr.enimaloc.esportline.api.wakfu.json.item.*;
import fr.enimaloc.esportline.api.wakfu.json.recipe.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class WakfuJSON {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    public static final String BASE_URL = "https://wakfu.cdn.ankama.com/gamedata/";
    public static final Logger LOGGER = LoggerFactory.getLogger(WakfuJSON.class);

    private final WakfuClient baseClient;
    private String version;

    private CachedEntity<Resource[]> resourcesCache;
    private CachedEntity<ResourceType[]> resourceTypesCache;
    private CachedEntity<States[]> statesCache;
    private CachedEntity<JobItem[]> jobItemsCache;
    private CachedEntity<ItemType[]> itemTypesCache;
    private CachedEntity<RecipeCategory[]> recipeCategoriesCache;
    private CachedEntity<ItemProperty[]> itemPropertiesCache;
    private CachedEntity<Action[]> actionsCache;
    private CachedEntity<Blueprint[]> blueprintsCache;
    private CachedEntity<CollectibleResource[]> collectibleResourcesCache;
    private CachedEntity<HarvestLoot[]> harvestLootsCache;
    private CachedEntity<ItemType[]> equipmentItemTypesCache;
    private CachedEntity<RecipeIngredient[]> recipeIngredientsCache;
    private CachedEntity<RecipeResult[]> recipeResultsCache;
    private CachedEntity<Recipe[]> recipesCache;
    private CachedEntity<ItemHolder[]> equipmentItemsCache;

    public WakfuJSON(WakfuClient baseClient) {
        this(baseClient, null);
    }

    public WakfuJSON(WakfuClient baseClient, String version) {
        this.baseClient = baseClient;
        if (version == null || version.equals("latest")) {
            try {
                version = getGamedataConfig0().version();
            } catch (IOException e) {
                LOGGER.error("Cannot get latest version", e);
            }
        }
        this.version = version;
    }

    public void update() {
        String oldVersion = version;
        try {
            version = getGamedataConfig0().version();
        } catch (IOException e) {
            LOGGER.error("Cannot get latest version", e);
        }
        if (!version.equals(oldVersion)) {
            LOGGER.info("Updating WakfuClient from {} to {}, invalidating cache...", oldVersion, version);
            invalidate();
        }
    }

    public void invalidate() {
        resourcesCache = null;
        resourceTypesCache = null;
        statesCache = null;
        jobItemsCache = null;
        itemTypesCache = null;
        recipeCategoriesCache = null;
        itemPropertiesCache = null;
        actionsCache = null;
        blueprintsCache = null;
        collectibleResourcesCache = null;
        harvestLootsCache = null;
        equipmentItemTypesCache = null;
        recipeIngredientsCache = null;
        recipeResultsCache = null;
        recipesCache = null;
    }

    public WakfuClient getBaseClient() {
        return baseClient;
    }

    public Optional<GamedataConfig> getGamedataConfig() {
        return getGamedataConfig(null);
    }

    public Optional<GamedataConfig> getGamedataConfig(Consumer<IOException> throwable) {
        try {
            return Optional.ofNullable(getGamedataConfig0());
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot get /config.json", e);
            }
            return Optional.empty();
        }
    }

    public GamedataConfig getGamedataConfig0() throws IOException {
        return JSON_MAPPER.readValue(URI.create(BASE_URL + "config.json").toURL(), GamedataConfig.class);
    }

    public Optional<Resource> getResource(int id) {
        return getResource(id, null);
    }

    public Optional<Resource> getResource(int id, Consumer<IOException> throwable) {
        try {
            return getResource0(id);
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot get resource with id {}", id, e);
            }
            return Optional.empty();
        }
    }

    public Optional<Resource> getResource0(int id) throws IOException {
        return Arrays.stream(getResources0())
                .filter(resource -> resource.id() == id)
                .findFirst();
    }

    public Optional<Resource[]> getResources() {
        return getResources(null);
    }

    public Optional<Resource[]> getResources(Consumer<IOException> throwable) {
        try {
            return Optional.ofNullable(getResources0());
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot get " + version + "/resource.json", e);
            }
            return Optional.empty();
        }
    }

    public Resource[] getResources0() throws IOException {
        if (resourcesCache == null || resourcesCache.isExpired(TimeUnit.DAYS, 1)) {
            resourcesCache = new CachedEntity<>(JSON_MAPPER.readValue(URI.create(BASE_URL + version + "/resources.json").toURL(), Resource[].class));
        }
        return resourcesCache.get();
    }

    public Optional<ResourceType> getResourceType(int id) {
        return getResourceType(id, null);
    }

    public Optional<ResourceType> getResourceType(int id, Consumer<IOException> throwable) {
        try {
            return getResourceType0(id);
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot get resource type with id {}", id, e);
            }
            return Optional.empty();
        }
    }

    public Optional<ResourceType> getResourceType0(int id) throws IOException {
        return Arrays.stream(getResourceTypes0())
                .filter(resourceType -> resourceType.id() == id)
                .findFirst();
    }

    public Optional<ResourceType[]> getResourceTypes() {
        return getResourceTypes(null);
    }

    public Optional<ResourceType[]> getResourceTypes(Consumer<IOException> throwable) {
        try {
            return Optional.ofNullable(getResourceTypes0());
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot get " + version + "/resourceTypes.json", e);
            }
            return Optional.empty();
        }
    }

    public ResourceType[] getResourceTypes0() throws IOException {
        if (resourceTypesCache == null || resourceTypesCache.isExpired(TimeUnit.DAYS, 1)) {
            resourceTypesCache = new CachedEntity<>(JSON_MAPPER.readValue(URI.create(BASE_URL + version + "/resourceTypes.json").toURL(), ResourceType[].class));
        }
        return resourceTypesCache.get();
    }

    public Optional<States> getState(int id) {
        return getState(id, null);
    }

    public Optional<States> getState(int id, Consumer<IOException> throwable) {
        try {
            return getState0(id);
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot get state with id {}", id, e);
            }
            return Optional.empty();
        }
    }

    public Optional<States> getState0(int id) throws IOException {
        return Arrays.stream(getStates0())
                .filter(state -> state.id() == id)
                .findFirst();
    }

    public Optional<States[]> getStates() {
        return getStates(null);
    }

    public Optional<States[]> getStates(Consumer<IOException> throwable) {
        try {
            return Optional.ofNullable(getStates0());
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot get " + version + "/states.json", e);
            }
            return Optional.empty();
        }
    }

    public States[] getStates0() throws IOException {
        if (statesCache == null || statesCache.isExpired(TimeUnit.DAYS, 1)) {
            statesCache = new CachedEntity<>(JSON_MAPPER.readValue(URI.create(BASE_URL + version + "/states.json").toURL(), States[].class));
        }
        return statesCache.get();
    }

    public Optional<JobItem> getJobItem(int id) {
        return getJobItem(id, null);
    }

    public Optional<JobItem> getJobItem(int id, Consumer<IOException> throwable) {
        try {
            return getJobItem0(id);
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot get job item with id {}", id, e);
            }
            return Optional.empty();
        }
    }

    public Optional<JobItem> getJobItem0(int id) throws IOException {
        return Arrays.stream(getJobItems0())
                .filter(jobItem -> jobItem.id() == id)
                .findFirst();
    }

    public Optional<JobItem[]> getJobItems() {
        return getJobItems(null);
    }

    public Optional<JobItem[]> getJobItems(Consumer<IOException> throwable) {
        try {
            return Optional.ofNullable(getJobItems0());
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot get " + version + "/jobsItems.json", e);
            }
            return Optional.empty();
        }
    }

    public JobItem[] getJobItems0() throws IOException {
        if (jobItemsCache == null || jobItemsCache.isExpired(TimeUnit.DAYS, 1)) {
            jobItemsCache = new CachedEntity<>(JSON_MAPPER.readValue(URI.create(BASE_URL + version + "/jobsItems.json").toURL(), JobItem[].class));
        }
        return jobItemsCache.get();
    }

    public Optional<ItemType> getItemType(int id) {
        return getItemType(id, null);
    }

    public Optional<ItemType> getItemType(int id, Consumer<IOException> throwable) {
        try {
            return getItemType0(id);
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot get item type with id {}", id, e);
            }
            return Optional.empty();
        }
    }

    public Optional<ItemType> getItemType0(int id) throws IOException {
        return Arrays.stream(getItemTypes0())
                .filter(itemType -> itemType.id() == id)
                .findFirst();
    }

    public Optional<ItemType[]> getItemTypes() {
        return getItemTypes(null);
    }

    public Optional<ItemType[]> getItemTypes(Consumer<IOException> throwable) {
        try {
            return Optional.ofNullable(getItemTypes0());
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot get " + version + "/itemTypes.json", e);
            }
            return Optional.empty();
        }
    }

    public ItemType[] getItemTypes0() throws IOException {
        if (itemTypesCache == null || itemTypesCache.isExpired(TimeUnit.DAYS, 1)) {
            itemTypesCache = new CachedEntity<>(JSON_MAPPER.readValue(URI.create(BASE_URL + version + "/itemTypes.json").toURL(), ItemType[].class));
        }
        return itemTypesCache.get();
    }

    public Optional<RecipeCategory> getRecipeCategory(int id) {
        return getRecipeCategory(id, null);
    }

    public Optional<RecipeCategory> getRecipeCategory(int id, Consumer<IOException> throwable) {
        try {
            return getRecipeCategory0(id);
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot get recipe category with id {}", id, e);
            }
            return Optional.empty();
        }
    }

    public Optional<RecipeCategory> getRecipeCategory0(int id) throws IOException {
        return Arrays.stream(getRecipeCategories0())
                .filter(recipeCategory -> recipeCategory.id() == id)
                .findFirst();
    }

    public Optional<RecipeCategory[]> getRecipeCategories() {
        return getRecipeCategories(null);
    }

    public Optional<RecipeCategory[]> getRecipeCategories(Consumer<IOException> throwable) {
        try {
            return Optional.ofNullable(getRecipeCategories0());
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot get " + version + "/recipeCategories.json", e);
            }
            return Optional.empty();
        }
    }

    public RecipeCategory[] getRecipeCategories0() throws IOException {
        if (recipeCategoriesCache == null || recipeCategoriesCache.isExpired(TimeUnit.DAYS, 1)) {
            recipeCategoriesCache = new CachedEntity<>(JSON_MAPPER.readValue(URI.create(BASE_URL + version + "/recipeCategories.json").toURL(), RecipeCategory[].class));
        }
        return recipeCategoriesCache.get();
    }

    public Optional<ItemProperty> getItemProperty(int id) {
        return getItemProperty(id, null);
    }

    public Optional<ItemProperty> getItemProperty(int id, Consumer<IOException> throwable) {
        try {
            return getItemProperty0(id);
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot get item property with id {}", id, e);
            }
            return Optional.empty();
        }
    }

    public Optional<ItemProperty> getItemProperty0(int id) throws IOException {
        return Arrays.stream(getItemProperties0())
                .filter(itemProperty -> itemProperty.id() == id)
                .findFirst();
    }

    public Optional<ItemProperty[]> getItemProperties() {
        return getItemProperties(null);
    }

    public Optional<ItemProperty[]> getItemProperties(Consumer<IOException> throwable) {
        try {
            return Optional.ofNullable(getItemProperties0());
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot get " + version + "/itemProperties.json", e);
            }
            return Optional.empty();
        }
    }

    public ItemProperty[] getItemProperties0() throws IOException {
        if (itemPropertiesCache == null || itemPropertiesCache.isExpired(TimeUnit.DAYS, 1)) {
            itemPropertiesCache = new CachedEntity<>(JSON_MAPPER.readValue(URI.create(BASE_URL + version + "/itemProperties.json").toURL(), ItemProperty[].class));
        }
        return itemPropertiesCache.get();
    }

    public Optional<Action> getAction(int id) {
        return getAction(id, null);
    }

    public Optional<Action> getAction(int id, Consumer<IOException> throwable) {
        try {
            return getAction0(id);
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot action with id {}", id, e);
            }
            return Optional.empty();
        }
    }

    public Optional<Action> getAction0(int id) throws IOException {
        return Arrays.stream(getActions0())
                .filter(action -> action.id() == id)
                .findFirst();
    }

    public Optional<Action[]> getActions() {
        return getActions(null);
    }

    public Optional<Action[]> getActions(Consumer<IOException> throwable) {
        try {
            return Optional.ofNullable(getActions0());
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot get " + version + "/actions.json", e);
            }
            return Optional.empty();
        }
    }

    public Action[] getActions0() throws IOException {
        if (actionsCache == null || actionsCache.isExpired(TimeUnit.DAYS, 1)) {
            actionsCache = new CachedEntity<>(JSON_MAPPER.readValue(URI.create(BASE_URL + version + "/actions.json").toURL(), Action[].class));
        }
        return actionsCache.get();
    }

    public Optional<Blueprint> getBlueprint(int id) {
        return getBlueprint(id, null);
    }

    public Optional<Blueprint> getBlueprint(int id, Consumer<IOException> throwable) {
        try {
            return getBlueprint0(id);
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot blueprint with id {}", id, e);
            }
            return Optional.empty();
        }
    }

    public Optional<Blueprint> getBlueprint0(int id) throws IOException {
        return Arrays.stream(getBlueprints0())
                .filter(blueprint -> blueprint.id() == id)
                .findFirst();
    }

    public Optional<Blueprint[]> getBlueprints() {
        return getBlueprints(null);
    }

    public Optional<Blueprint[]> getBlueprints(Consumer<IOException> throwable) {
        try {
            return Optional.ofNullable(getBlueprints0());
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot get " + version + "/blueprints.json", e);
            }
            return Optional.empty();
        }
    }

    public Blueprint[] getBlueprints0() throws IOException {
        if (blueprintsCache == null || blueprintsCache.isExpired(TimeUnit.DAYS, 1)) {
            blueprintsCache = new CachedEntity<>(JSON_MAPPER.readValue(URI.create(BASE_URL + version + "/blueprints.json").toURL(), Blueprint[].class));
        }
        return blueprintsCache.get();
    }

    public Optional<CollectibleResource> getCollectibleResource(int id) {
        return getCollectibleResource(id, null);
    }

    public Optional<CollectibleResource> getCollectibleResource(int id, Consumer<IOException> throwable) {
        try {
            return getCollectibleResource0(id);
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot collectible resource with id {}", id, e);
            }
            return Optional.empty();
        }
    }

    public Optional<CollectibleResource> getCollectibleResource0(int id) throws IOException {
        return Arrays.stream(getCollectibleResources0())
                .filter(collectibleResource -> collectibleResource.id() == id)
                .findFirst();
    }

    public Optional<CollectibleResource[]> getCollectibleResources() {
        return getCollectibleResources(null);
    }

    public Optional<CollectibleResource[]> getCollectibleResources(Consumer<IOException> throwable) {
        try {
            return Optional.ofNullable(getCollectibleResources0());
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot get " + version + "/collectibleResources.json", e);
            }
            return Optional.empty();
        }
    }

    public CollectibleResource[] getCollectibleResources0() throws IOException {
        if (collectibleResourcesCache == null || collectibleResourcesCache.isExpired(TimeUnit.DAYS, 1)) {
            collectibleResourcesCache = new CachedEntity<>(JSON_MAPPER.readValue(URI.create(BASE_URL + version + "/collectibleResources.json").toURL(), CollectibleResource[].class));
        }
        return collectibleResourcesCache.get();
    }

    public Optional<HarvestLoot[]> getHarvestLoots() {
        return getHarvestLoots(null);
    }

    public Optional<HarvestLoot[]> getHarvestLoots(Consumer<IOException> throwable) {
        try {
            return Optional.ofNullable(getHarvestLoots0());
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot harvest loots", e);
            }
            return Optional.empty();
        }
    }

    public HarvestLoot[] getHarvestLoots0() throws IOException {
        if (harvestLootsCache == null || harvestLootsCache.isExpired(TimeUnit.DAYS, 1)) {
            harvestLootsCache = new CachedEntity<>(JSON_MAPPER.readValue(URI.create(BASE_URL + version + "/harvestLoots.json").toURL(), HarvestLoot[].class));
        }
        return harvestLootsCache.get();
    }

    public Optional<HarvestLoot> getHarvestLoot(int id) {
        return getHarvestLoot(id, null);
    }

    public Optional<HarvestLoot> getHarvestLoot(int id, Consumer<IOException> throwable) {
        try {
            return getHarvestLoot0(id);
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot harvest loot with id {}", id, e);
            }
            return Optional.empty();
        }
    }

    public Optional<HarvestLoot> getHarvestLoot0(int id) throws IOException {
        return Arrays.stream(getHarvestLoots0())
                .filter(harvestLoot -> harvestLoot.id() == id)
                .findFirst();
    }

    public Optional<ItemType[]> getEquipmentItemTypes() {
        return getEquipmentItemTypes(null);
    }

    public Optional<ItemType[]> getEquipmentItemTypes(Consumer<IOException> throwable) {
        try {
            return Optional.ofNullable(getEquipmentItemTypes0());
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot equipment item types", e);
            }
            return Optional.empty();
        }
    }

    public ItemType[] getEquipmentItemTypes0() throws IOException {
        if (equipmentItemTypesCache == null || equipmentItemTypesCache.isExpired(TimeUnit.DAYS, 1)) {
            equipmentItemTypesCache = new CachedEntity<>(JSON_MAPPER.readValue(URI.create(BASE_URL + version + "/equipmentItemTypes.json").toURL(), ItemType[].class));
        }
        return equipmentItemTypesCache.get();
    }

    public Optional<ItemType> getEquipmentItemType(int id) {
        return getEquipmentItemType(id, null);
    }

    public Optional<ItemType> getEquipmentItemType(int id, Consumer<IOException> throwable) {
        try {
            return getEquipmentItemType0(id);
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot equipment item type with id {}", id, e);
            }
            return Optional.empty();
        }
    }

    public Optional<ItemType> getEquipmentItemType0(int id) throws IOException {
        return Arrays.stream(getEquipmentItemTypes0())
                .filter(itemType -> itemType.id() == id)
                .findFirst();
    }

    public Optional<RecipeIngredient[]> getRecipeIngredients() {
        return getRecipeIngredients(null);
    }

    public Optional<RecipeIngredient[]> getRecipeIngredients(Consumer<IOException> throwable) {
        try {
            return Optional.ofNullable(getRecipeIngredients0());
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot recipe ingredients", e);
            }
            return Optional.empty();
        }
    }

    public RecipeIngredient[] getRecipeIngredients0() throws IOException {
        if (recipeIngredientsCache == null || recipeIngredientsCache.isExpired(TimeUnit.DAYS, 1)) {
            recipeIngredientsCache = new CachedEntity<>(JSON_MAPPER.readValue(URI.create(BASE_URL + version + "/recipeIngredients.json").toURL(), RecipeIngredient[].class));
        }
        return recipeIngredientsCache.get();
    }

    public Optional<RecipeIngredient[]> getRecipeIngredient(int id) {
        return getRecipeIngredient(id, null);
    }

    public Optional<RecipeIngredient[]> getRecipeIngredient(int id, Consumer<IOException> throwable) {
        try {
            return Optional.ofNullable(getRecipeIngredient0(id));
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot recipe ingredient with id {}", id, e);
            }
            return Optional.empty();
        }
    }

    public RecipeIngredient[] getRecipeIngredient0(int id) throws IOException {
        return Arrays.stream(getRecipeIngredients0())
                .filter(recipeIngredient -> recipeIngredient.id() == id)
                .toArray(RecipeIngredient[]::new);
    }

    public Optional<RecipeResult[]> getRecipeResults() {
        return getRecipeResults(null);
    }

    public Optional<RecipeResult[]> getRecipeResults(Consumer<IOException> throwable) {
        try {
            return Optional.ofNullable(getRecipeResults0());
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot recipe results", e);
            }
            return Optional.empty();
        }
    }

    public RecipeResult[] getRecipeResults0() throws IOException {
        if (recipeResultsCache == null || recipeResultsCache.isExpired(TimeUnit.DAYS, 1)) {
            recipeResultsCache = new CachedEntity<>(JSON_MAPPER.readValue(URI.create(BASE_URL + version + "/recipeResults.json").toURL(), RecipeResult[].class));
        }
        return recipeResultsCache.get();
    }

    public Optional<RecipeResult> getRecipeResult(int id) {
        return getRecipeResult(id, null);
    }

    public Optional<RecipeResult> getRecipeResult(int id, Consumer<IOException> throwable) {
        try {
            return getRecipeResult0(id);
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot recipe result with id {}", id, e);
            }
            return Optional.empty();
        }
    }

    public Optional<RecipeResult> getRecipeResult0(int id) throws IOException {
        return Arrays.stream(getRecipeResults0())
                .filter(recipeResult -> recipeResult.id() == id)
                .findFirst();
    }

    public Optional<Recipe[]> getRecipes() {
        return getRecipes(null);
    }

    public Optional<Recipe[]> getRecipes(Consumer<IOException> throwable) {
        try {
            return Optional.ofNullable(getRecipes0());
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot recipes", e);
            }
            return Optional.empty();
        }
    }

    public Recipe[] getRecipes0() throws IOException {
        if (recipesCache == null || recipesCache.isExpired(TimeUnit.DAYS, 1)) {
            recipesCache = new CachedEntity<>(JSON_MAPPER.readValue(URI.create(BASE_URL + version + "/recipes.json").toURL(), Recipe[].class));
        }
        return recipesCache.get();
    }

    public Optional<Recipe> getRecipe(int id) {
        return getRecipe(id, null);
    }

    public Optional<Recipe> getRecipe(int id, Consumer<IOException> throwable) {
        try {
            return getRecipe0(id);
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot recipe with id {}", id, e);
            }
            return Optional.empty();
        }
    }

    public Optional<Recipe> getRecipe0(int id) throws IOException {
        return Arrays.stream(getRecipes0())
                .filter(recipe -> recipe.id() == id)
                .findFirst();
    }

    public Optional<ItemHolder[]> getEquipmentItems() {
        return getEquipmentItems(null);
    }

    public Optional<ItemHolder[]> getEquipmentItems(Consumer<IOException> throwable) {
        try {
            return Optional.ofNullable(getEquipmentItems0());
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot equipment items", e);
            }
            return Optional.empty();
        }
    }

    public ItemHolder[] getEquipmentItems0() throws IOException {
        if (equipmentItemsCache == null || equipmentItemsCache.isExpired(TimeUnit.DAYS, 1)) {
            equipmentItemsCache = new CachedEntity<>(JSON_MAPPER.readValue(URI.create(BASE_URL + version + "/items.json").toURL(), ItemHolder[].class));
        }
        return equipmentItemsCache.get();
    }

    public Optional<ItemHolder> getEquipmentItem(int id) {
        return getEquipmentItem(id, null);
    }

    public Optional<ItemHolder> getEquipmentItem(int id, Consumer<IOException> throwable) {
        try {
            return getEquipmentItem0(id);
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot equipment item with id {}", id, e);
            }
            return Optional.empty();
        }
    }

    public Optional<ItemHolder> getEquipmentItem0(int id) throws IOException {
        return Arrays.stream(getEquipmentItems0())
                .filter(itemHolder -> itemHolder.id() == id)
                .findFirst();
    }

    public Optional<Item> getItem(int id) {
        return getItem(id, null);
    }

    public Optional<Item> getItem(int id, Consumer<IOException> throwable) {
        try {
            return getItem0(id);
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot item with id {}", id, e);
            }
            return Optional.empty();
        }
    }

    public Optional<Item> getItem0(int id) throws IOException {
        return Arrays.stream(getItems0())
                .filter(item -> item.id() == id)
                .findFirst();
    }

    public Optional<Item[]> getItems() {
        return getItems(null);
    }

    public Optional<Item[]> getItems(Consumer<IOException> throwable) {
        try {
            return Optional.ofNullable(getItems0());
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot items", e);
            }
            return Optional.empty();
        }
    }

    public Item[] getItems0() throws IOException {
        List<Item> items = new ArrayList<>();
        items.addAll(Arrays.asList(getEquipmentItems0()));
        items.addAll(Arrays.asList(getJobItems0()));
        return items.toArray(new Item[0]);
    }

    public String getCurrentVersion() {
        return version;
    }

    public static void main(String[] args) {
//        WakfuJSON client = new WakfuJSON(baseClient);
//        client.getGamedataConfig().ifPresentOrElse(
//                config -> System.out.println("Version: " + config.version()),
//                () -> System.out.println("Cannot get version")
//        );
//        client.getResources().ifPresentOrElse(
//                resources -> System.out.println("Resources loaded: " + resources.length),
//                () -> System.out.println("Cannot get resources")
//        );
//        client.getResource(6).ifPresentOrElse(
//                resource -> System.out.println("Resource: " + resource + "\n\tType: " + resource.definition().fetchResourceType(client)),
//                () -> System.out.println("Cannot get resource 6")
//        );
//        client.getResourceTypes().ifPresentOrElse(
//                resourceTypes -> System.out.println("ResourceTypes loaded: " + resourceTypes.length),
//                () -> System.out.println("Cannot get resourceTypes")
//        );
//        client.getResourceType(1).ifPresentOrElse(
//                resourceType -> System.out.println("ResourceType: " + resourceType),
//                () -> System.out.println("Cannot get resourceType 1")
//        );
//        client.getStates().ifPresentOrElse(
//                states -> System.out.println("States loaded: " + states.length),
//                () -> System.out.println("Cannot get states")
//        );
//        client.getState(5076).ifPresentOrElse(
//                state -> System.out.println("State: " + state),
//                () -> System.out.println("Cannot get state 5076")
//        );
//        client.getJobItems().ifPresentOrElse(
//                jobItems -> System.out.println("JobItems loaded: " + jobItems.length),
//                () -> System.out.println("Cannot get jobItems")
//        );
//        client.getJobItem(1718).ifPresentOrElse(
//                jobItem -> System.out.println("JobItem: " + jobItem + "\n\tType: " + jobItem.definition().fetchItemType(client)),
//                () -> System.out.println("Cannot get jobItem 1718")
//        );
//        client.getItemTypes().ifPresentOrElse(
//                itemTypes -> System.out.println("ItemTypes loaded: " + itemTypes.length),
//                () -> System.out.println("Cannot get itemTypes")
//        );
//        client.getItemType(306).ifPresentOrElse(
//                itemType -> System.out.println("ItemType: " + itemType),
//                () -> System.out.println("Cannot get itemType 306")
//        );
//        client.getRecipeCategories().ifPresentOrElse(
//                recipeCategories -> System.out.println("RecipeCategories loaded: " + recipeCategories.length),
//                () -> System.out.println("Cannot get recipeCategories")
//        );
//        client.getRecipeCategory(40).ifPresentOrElse(
//                recipeCategory -> System.out.println("RecipeCategory: " + recipeCategory),
//                () -> System.out.println("Cannot get recipeCategory 40")
//        );
//        client.getItemProperties().ifPresentOrElse(
//                itemProperties -> System.out.println("ItemProperties loaded: " + itemProperties.length),
//                () -> System.out.println("Cannot get itemProperties")
//        );
//        client.getItemProperty(1).ifPresentOrElse(
//                itemProperty -> System.out.println("ItemProperty: " + itemProperty),
//                () -> System.out.println("Cannot get itemProperty 1")
//        );
//        client.getActions().ifPresentOrElse(
//                actions -> System.out.println("Actions loaded: " + actions.length),
//                () -> System.out.println("Cannot get actions")
//        );
//        client.getAction(1).ifPresentOrElse(
//                action -> System.out.println("Action: " + action),
//                () -> System.out.println("Cannot get action 1")
//        );
//        client.getBlueprints().ifPresentOrElse(
//                blueprints -> System.out.println("Blueprints loaded: " + blueprints.length),
//                () -> System.out.println("Cannot get blueprints")
//        );
//        client.getBlueprint(15992).ifPresentOrElse(
//                blueprint -> System.out.println("Blueprint: " + blueprint),
//                () -> System.out.println("Cannot get blueprint 15992")
//        );
//        client.getCollectibleResources().ifPresentOrElse(
//                collectibleResources -> System.out.println("CollectibleResources loaded: " + collectibleResources.length),
//                () -> System.out.println("Cannot get collectibleResources")
//        );
//        client.getCollectibleResource(1378).ifPresentOrElse(
//                collectibleResource -> System.out.println("CollectibleResource: " + collectibleResource),
//                () -> System.out.println("Cannot get collectibleResource 1378")
//        );
//        client.getHarvestLoots().ifPresentOrElse(
//                harvestLoots -> System.out.println("HarvestLoots loaded: " + harvestLoots.length),
//                () -> System.out.println("Cannot get harvestLoots")
//        );
//        client.getHarvestLoot(993).ifPresentOrElse(
//                harvestLoot -> System.out.println("HarvestLoot: " + harvestLoot),
//                () -> System.out.println("Cannot get harvestLoot 993")
//        );
//        client.getEquipmentItemTypes().ifPresentOrElse(
//                equipmentItemTypes -> System.out.println("EquipmentItemTypes loaded: " + equipmentItemTypes.length),
//                () -> System.out.println("Cannot get equipmentItemTypes")
//        );
//        client.getEquipmentItemType(101).ifPresentOrElse(
//                equipmentItemType -> System.out.println("EquipmentItemType: " + equipmentItemType),
//                () -> System.out.println("Cannot get equipmentItemType 101")
//        );
//        client.getRecipeIngredients().ifPresentOrElse(
//                recipeIngredients -> System.out.println("RecipeIngredients loaded: " + recipeIngredients.length),
//                () -> System.out.println("Cannot get recipeIngredients")
//        );
//        client.getRecipeIngredient(1160).ifPresentOrElse(
//                recipeIngredient -> System.out.println("RecipeIngredient: " + Arrays.toString(recipeIngredient)),
//                () -> System.out.println("Cannot get recipeIngredient 1160")
//        );
//        client.getRecipeResults().ifPresentOrElse(
//                recipeResults -> System.out.println("RecipeResults loaded: " + recipeResults.length),
//                () -> System.out.println("Cannot get recipeResults")
//        );
//        client.getRecipeResult(1160).ifPresentOrElse(
//                recipeResult -> System.out.println("RecipeResult: " + recipeResult),
//                () -> System.out.println("Cannot get recipeResult 1160")
//        );
//        client.getRecipes().ifPresentOrElse(
//                recipes -> System.out.println("Recipes loaded: " + recipes.length),
//                () -> System.out.println("Cannot get recipes")
//        );
//        client.getRecipe(1160).ifPresentOrElse(
//                recipe -> System.out.println("Recipe: " + recipe),
//                () -> System.out.println("Cannot get recipe 1160")
//        );
//        client.getEquipmentItems().ifPresentOrElse(
//                equipmentItems -> System.out.println("EquipmentItems loaded: " + equipmentItems.length),
//                () -> System.out.println("Cannot get equipmentItems")
//        );
//        client.getEquipmentItem(2021).ifPresentOrElse(
//                equipmentItem -> System.out.println("EquipmentItem: " + equipmentItem),
//                () -> System.out.println("Cannot get equipmentItem 2021")
//        );
    }

    public void load() {
        getGamedataConfig();
        getResources();
        getResourceTypes();
        getStates();
        getJobItems();
        getItemTypes();
        getRecipeCategories();
        getItemProperties();
        getActions();
        getBlueprints();
        getCollectibleResources();
        getHarvestLoots();
        getEquipmentItemTypes();
        getRecipeIngredients();
        getRecipeResults();
        getRecipes();
        getEquipmentItems();
    }
}
