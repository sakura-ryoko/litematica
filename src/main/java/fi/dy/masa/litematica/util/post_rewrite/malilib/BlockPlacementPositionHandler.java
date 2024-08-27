package fi.dy.masa.litematica.util.post_rewrite.malilib;

import java.util.ArrayList;
import javax.annotation.Nullable;

import net.minecraft.util.math.BlockPos;

public class BlockPlacementPositionHandler
{
    protected final ArrayList<IBlockPlacementPositionProvider> providers = new ArrayList<>();

    public BlockPlacementPositionHandler()
    {
    }

    public void registerPositionProvider(IBlockPlacementPositionProvider provider)
    {
        if (this.providers.contains(provider) == false)
        {
            this.providers.add(provider);
        }
    }

    public void unregisterPositionProvider(IBlockPlacementPositionProvider provider)
    {
        this.providers.remove(provider);
    }

    /**
     * Returns the current overridden block placement position, if any.
     * If no providers currently want to override the position, then
     * null is returned.
     * @return the current overridden block placement position, or null for no changes from vanilla
     */
    @Nullable
    public BlockPos getCurrentPlacementPosition()
    {
        if (this.providers.isEmpty() == false)
        {
            for (IBlockPlacementPositionProvider provider : this.providers)
            {
                BlockPos pos = provider.getPlacementPosition();

                if (pos != null)
                {
                    return pos;
                }
            }
        }

        return null;
    }
}
