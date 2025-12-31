package fi.dy.masa.litematica.schematic.placement;

import javax.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.netty.buffer.ByteBuf;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;

import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.position.PositionUtils.CoordinateType;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.util.PositionUtils;

public class SubRegionPlacement
{
    public static final Codec<SubRegionPlacement> CODEC = RecordCodecBuilder.create(
            inst -> inst.group(
                    PrimitiveCodec.STRING.fieldOf("Name").forGetter(get -> get.name),
                    BlockPos.CODEC.fieldOf("DefaultPos").forGetter(get -> get.defaultPos),
                    BlockPos.CODEC.fieldOf("Pos").forGetter(get -> get.pos),
                    BlockRotation.CODEC.fieldOf("Rotation").forGetter(get -> get.rotation),
                    BlockMirror.CODEC.fieldOf("Mirror").forGetter(get -> get.mirror),
                    PrimitiveCodec.BOOL.fieldOf("Enabled").forGetter(get -> get.enabled),
                    PrimitiveCodec.BOOL.fieldOf("RenderingEnabled").forGetter(get -> get.renderingEnabled),
                    PrimitiveCodec.BOOL.fieldOf("IgnoreEntities").forGetter(get -> get.ignoreEntities),
                    PrimitiveCodec.INT.fieldOf("CoordinateLockMask").forGetter(get -> get.coordinateLockMask)
            ).apply(inst, SubRegionPlacement::new)
    );
    public static final PacketCodec<ByteBuf, BlockMirror> BLOCK_MIRROR_PACKET_CODEC = PacketCodecs.STRING.xmap(BlockMirror::valueOf, BlockMirror::asString);
    public static final PacketCodec<ByteBuf, SubRegionPlacement> PACKET_CODEC = new PacketCodec<>()
    {
        @Override
        public void encode(ByteBuf buf, SubRegionPlacement value)
        {
            PacketCodecs.STRING.encode(buf, value.name);
            BlockPos.PACKET_CODEC.encode(buf, value.defaultPos);
            BlockPos.PACKET_CODEC.encode(buf, value.pos);
            BlockRotation.PACKET_CODEC.encode(buf, value.rotation);
            BLOCK_MIRROR_PACKET_CODEC.encode(buf, value.mirror);
            PacketCodecs.BOOLEAN.encode(buf, value.enabled);
            PacketCodecs.BOOLEAN.encode(buf, value.renderingEnabled);
            PacketCodecs.BOOLEAN.encode(buf, value.ignoreEntities);
            PacketCodecs.INTEGER.encode(buf, value.coordinateLockMask);
        }

        @Override
        public SubRegionPlacement decode(ByteBuf buf)
        {
            return new SubRegionPlacement(
                PacketCodecs.STRING.decode(buf),
                BlockPos.PACKET_CODEC.decode(buf),
                BlockPos.PACKET_CODEC.decode(buf),
                BlockRotation.PACKET_CODEC.decode(buf),
                BLOCK_MIRROR_PACKET_CODEC.decode(buf),
                PacketCodecs.BOOLEAN.decode(buf),
                PacketCodecs.BOOLEAN.decode(buf),
                PacketCodecs.BOOLEAN.decode(buf),
                PacketCodecs.INTEGER.decode(buf)
            );
        }
    };
    private final String name;
    private final BlockPos defaultPos;
    private BlockPos pos;
    private BlockRotation rotation = BlockRotation.NONE;
    private BlockMirror mirror = BlockMirror.NONE;
    private boolean enabled = true;
    private boolean renderingEnabled = true;
    private boolean ignoreEntities;
    private int coordinateLockMask;

    public SubRegionPlacement(BlockPos pos, String name)
    {
        this.pos = pos;
        this.defaultPos = pos;
        this.name = name;
        SchematicPlacementEventHandler.getInstance().onSubRegionInit(this);
    }

    private SubRegionPlacement(String name, BlockPos defPos, BlockPos pos, BlockRotation rot, BlockMirror mirror, Boolean enabled, Boolean renderingEnabled, Boolean ignoreEntities, Integer coordinateLockMask)
    {
        this(defPos, name);
        this.pos = pos;
        this.rotation = rot;
        this.mirror = mirror;
        this.enabled = enabled;
        this.renderingEnabled = renderingEnabled;
        this.ignoreEntities = ignoreEntities;
        this.coordinateLockMask = coordinateLockMask;
        SchematicPlacementEventHandler.getInstance().onSubRegionInit(this);
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public boolean isRenderingEnabled()
    {
        return this.renderingEnabled;
    }

    public boolean ignoreEntities()
    {
        return this.ignoreEntities;
    }

    public void setCoordinateLocked(CoordinateType coord, boolean locked)
    {
        int mask = 0x1 << coord.ordinal();

        if (locked)
        {
            this.coordinateLockMask |= mask;
        }
        else
        {
            this.coordinateLockMask &= ~mask;
        }
    }

    public boolean isCoordinateLocked(CoordinateType coord)
    {
        int mask = 0x1 << coord.ordinal();
        return (this.coordinateLockMask & mask) != 0;
    }

    public boolean matchesRequirement(RequiredEnabled required)
    {
        if (required == RequiredEnabled.ANY)
        {
            return true;
        }

        if (required == RequiredEnabled.PLACEMENT_ENABLED)
        {
            return this.isEnabled();
        }

        return this.isEnabled() && this.isRenderingEnabled();
    }

    public String getName()
    {
        return this.name;
    }

    public BlockPos getDefaultPos()
    {
        return this.defaultPos;
    }

    public BlockPos getPos()
    {
        return this.pos;
    }

    public BlockRotation getRotation()
    {
        return this.rotation;
    }

    public BlockMirror getMirror()
    {
        return this.mirror;
    }

    public void setRenderingEnabled(boolean renderingEnabled)
    {
        this.renderingEnabled = renderingEnabled;
        SchematicPlacementEventHandler.getInstance().onSetSubRegionRender(this, renderingEnabled);
    }

    public void toggleRenderingEnabled()
    {
        this.setRenderingEnabled(! this.isRenderingEnabled());
    }

    void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
        SchematicPlacementEventHandler.getInstance().onSetSubRegionEnabled(this, enabled);
    }

    void toggleEnabled()
    {
        this.setEnabled(! this.isEnabled());
    }

    void toggleIgnoreEntities()
    {
        this.ignoreEntities = ! this.ignoreEntities;
    }

    void setPos(BlockPos pos)
    {
        this.pos = PositionUtils.getModifiedPartiallyLockedPosition(this.pos, pos, this.coordinateLockMask);
        SchematicPlacementEventHandler.getInstance().onSetSubRegionOrigin(this, this.pos);
    }

    void setRotation(BlockRotation rotation)
    {
        this.rotation = rotation;
        SchematicPlacementEventHandler.getInstance().onSetSubRegionRotation(this, rotation);
    }

    void setMirror(BlockMirror mirror)
    {
        this.mirror = mirror;
        SchematicPlacementEventHandler.getInstance().onSetSubRegionMirror(this, mirror);
    }

    void resetToOriginalValues()
    {
        SchematicPlacementEventHandler.getInstance().onSubRegionReset(this);
        this.pos = this.defaultPos;
        this.rotation = BlockRotation.NONE;
        this.mirror = BlockMirror.NONE;
        this.enabled = true;
        this.ignoreEntities = false;
    }

    public boolean isRegionPlacementModifiedFromDefault()
    {
        return this.isRegionPlacementModified(this.defaultPos);
    }

    public boolean isRegionPlacementModified(BlockPos originalPosition)
    {
        return this.isEnabled() == false ||
               this.ignoreEntities() ||
               this.getMirror() != BlockMirror.NONE ||
               this.getRotation() != BlockRotation.NONE ||
               this.getPos().equals(originalPosition) == false;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();

        arr.add(this.pos.getX());
        arr.add(this.pos.getY());
        arr.add(this.pos.getZ());

        obj.add("pos", arr);
        obj.add("name", new JsonPrimitive(this.getName()));
        obj.add("rotation", new JsonPrimitive(this.rotation.name()));
        obj.add("mirror", new JsonPrimitive(this.mirror.name()));
        obj.add("locked_coords", new JsonPrimitive(this.coordinateLockMask));
        obj.add("enabled", new JsonPrimitive(this.enabled));
        obj.add("rendering_enabled", new JsonPrimitive(this.renderingEnabled));
        obj.add("ignore_entities", new JsonPrimitive(this.ignoreEntities));

        SchematicPlacementEventHandler.getInstance().onSaveSubRegionToJson(this, obj);

        return obj;
    }

    @Nullable
    public static SubRegionPlacement fromJson(JsonObject obj)
    {
        if (JsonUtils.hasArray(obj, "pos") &&
            JsonUtils.hasString(obj, "name") &&
            JsonUtils.hasString(obj, "rotation") &&
            JsonUtils.hasString(obj, "mirror"))
        {
            JsonArray posArr = obj.get("pos").getAsJsonArray();

            if (posArr.size() != 3)
            {
                Litematica.LOGGER.warn("Placement.fromJson(): Failed to load a placement from JSON, invalid position data");
                return null;
            }

            BlockPos pos = new BlockPos(posArr.get(0).getAsInt(), posArr.get(1).getAsInt(), posArr.get(2).getAsInt());
            SubRegionPlacement placement = new SubRegionPlacement(pos, obj.get("name").getAsString());
            placement.setEnabled(JsonUtils.getBoolean(obj, "enabled"));
            placement.setRenderingEnabled(JsonUtils.getBoolean(obj, "rendering_enabled"));
            placement.ignoreEntities = JsonUtils.getBoolean(obj, "ignore_entities");
            placement.coordinateLockMask = JsonUtils.getInteger(obj, "locked_coords");

            try
            {
                BlockRotation rotation = BlockRotation.valueOf(obj.get("rotation").getAsString());
                BlockMirror mirror = BlockMirror.valueOf(obj.get("mirror").getAsString());

                placement.setRotation(rotation);
                placement.setMirror(mirror);
            }
            catch (Exception e)
            {
                Litematica.LOGGER.warn("Placement.fromJson(): Invalid rotation or mirror value for a placement");
            }

            SchematicPlacementEventHandler.getInstance().onSubRegionCreateFromJson(placement, pos, placement.getName(), placement.getRotation(), placement.getMirror(), placement.isEnabled(), placement.renderingEnabled, obj);

            return placement;
        }

        return null;
    }

    public enum RequiredEnabled
    {
        ANY,
        PLACEMENT_ENABLED,
        RENDERING_ENABLED
    }
}
