package fi.dy.masa.litematica.util.post_rewrite.malilib;

import java.util.Optional;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.Direction;

public class BlockUtils2
{
    /**
     * Returns the Direction value of the first found PropertyDirection
     * type block state property in the given state, if any.
     * If there are no PropertyDirection properties, then empty() is returned.
     */
    public static Optional<Direction> getFirstPropertyFacingValue(BlockState state)
    {
        Optional<DirectionProperty> propOptional = getFirstDirectionProperty(state);
        return propOptional.map(directionProperty -> Direction.byId(state.get(directionProperty).getId()));
    }

    /**
     * Returns the first PropertyDirection property from the provided state, if any.
     * @return the first PropertyDirection, or empty() if there are no such properties
     */
    public static Optional<DirectionProperty> getFirstDirectionProperty(BlockState state)
    {
        for (Property<?> prop : state.getProperties())
        {
            if (prop instanceof DirectionProperty)
            {
                return Optional.of((DirectionProperty) prop);
            }
        }

        return Optional.empty();
    }
}
