package fi.dy.masa.litematica.schematic.placement;

/**
 * The flags used by {@link SchematicPlacementEventHandler}
 */
public enum SchematicPlacementEventFlag
{
    ALL_EVENTS,

    INIT,
    CREATE_FOR,
    FOR_CONVERSION,
    FROM_JSON,
    FROM_NBT,
    TO_JSON,
    TO_NBT,

    TOGGLE_LOCKED,
    SET_ENABLED,
    SET_RENDER,
    SET_NAME,
    SET_ORIGIN,
    SET_MIRROR,
    SET_ROTATION,
    ON_RESET,

    INIT_SUBREGION,
    SUBREGION_FROM_JSON,
    SUBREGION_TO_JSON,

    SET_SUBREGION_ENABLED,
    SET_SUBREGION_RENDER,
    SET_SUBREGION_NAME,
    SET_SUBREGION_ORIGIN,
    SET_SUBREGION_MIRROR,
    SET_SUBREGION_ROTATION,
    ON_SUBREGION_RESET,

    ON_SELECTED,
    ON_ADDED,
    ON_REMOVED,
    ON_UPDATED
}
