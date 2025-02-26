package fi.dy.masa.litematica.selection;

import javax.annotation.Nullable;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.netty.buffer.ByteBuf;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.BlockPos;

import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.position.PositionUtils.CoordinateType;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.PositionUtils.Corner;

public class Box
{
    public static final Codec<Box> CODEC = RecordCodecBuilder.create(
            inst -> inst.group(
                    BlockPos.CODEC.fieldOf("pos1").forGetter(get -> get.pos1 != null ? get.pos1 : BlockPos.ORIGIN),
                    BlockPos.CODEC.fieldOf("pos2").forGetter(get -> get.pos2 != null ? get.pos2 : BlockPos.ORIGIN),
                    PrimitiveCodec.STRING.fieldOf("name").forGetter(get -> get.name)
            ).apply(inst, Box::new)
    );
    public static final PacketCodec<ByteBuf, Box> PACKET_CODEC = new PacketCodec<>()
    {
        @Override
        public Box decode(ByteBuf buf)
        {
            return new Box(
                    BlockPos.PACKET_CODEC.decode(buf),
                    BlockPos.PACKET_CODEC.decode(buf),
                    PacketCodecs.STRING.decode(buf)
            );
        }

        @Override
        public void encode(ByteBuf buf, Box value)
        {
            BlockPos.PACKET_CODEC.encode(buf, value.pos1 != null ? value.pos1 : BlockPos.ORIGIN);
            BlockPos.PACKET_CODEC.encode(buf, value.pos2 != null ? value.pos2 : BlockPos.ORIGIN);
            PacketCodecs.STRING.encode(buf, value.name);
        }
    };
    @Nullable private BlockPos pos1;
    @Nullable private BlockPos pos2;
    private BlockPos size = BlockPos.ORIGIN;
    private String name = "Unnamed";
    private Corner selectedCorner = Corner.NONE;

    public Box()
    {
        this.pos1 = BlockPos.ORIGIN;
        this.pos2 = BlockPos.ORIGIN;
        this.updateSize();
    }

    public Box(@Nullable BlockPos pos1, @Nullable BlockPos pos2, String name)
    {
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.name = name;

        this.updateSize();
    }

    public Box copy()
    {
        Box box = new Box(this.pos1, this.pos2, this.name);
        box.setSelectedCorner(this.selectedCorner);
        return box;
    }

    @Nullable
    public BlockPos getPos1()
    {
        return this.pos1;
    }

    @Nullable
    public BlockPos getPos2()
    {
        return this.pos2;
    }

    public BlockPos getSize()
    {
        return this.size;
    }

    public String getName()
    {
        return this.name;
    }

    public Corner getSelectedCorner()
    {
        return this.selectedCorner;
    }

    public void setPos1(@Nullable BlockPos pos)
    {
        this.pos1 = pos;
        this.updateSize();
    }

    public void setPos2(@Nullable BlockPos pos)
    {
        this.pos2 = pos;
        this.updateSize();
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setSelectedCorner(Corner corner)
    {
        this.selectedCorner = corner;
    }

    /*
    public void rotate(Rotation rotation)
    {
        BlockPos pos = PositionUtils.getTransformedBlockPos(this.getSize(), Mirror.NONE, rotation);
        this.setPos2(this.getPos1().add(pos).add(-1, -1, -1));
    }

    public void mirror(Mirror mirror)
    {
        BlockPos pos = PositionUtils.getTransformedBlockPos(this.getSize(), mirror, Rotation.NONE);
        this.setPos2(this.getPos1().add(pos).add(-1, -1, -1));
    }
    */

    private void updateSize()
    {
        if (this.pos1 != null && this.pos2 != null)
        {
            this.size = PositionUtils.getAreaSizeFromRelativeEndPosition(this.pos2.subtract(this.pos1));
        }
        else if (this.pos1 == null && this.pos2 == null)
        {
            this.size = BlockPos.ORIGIN;
        }
        else
        {
            this.size = new BlockPos(1, 1, 1);
        }
    }

    public BlockPos getPosition(Corner corner)
    {
        return corner == Corner.CORNER_1 ? this.getPos1() : this.getPos2();
    }

    public int getCoordinate(Corner corner, CoordinateType type)
    {
        BlockPos pos = this.getPosition(corner);

        switch (type)
        {
            case X: return pos.getX();
            case Y: return pos.getY();
            case Z: return pos.getZ();
        }

        return 0;
    }

    protected void setPosition(BlockPos pos, Corner corner)
    {
        if (corner == Corner.CORNER_1)
        {
            this.setPos1(pos);
        }
        else if (corner == Corner.CORNER_2)
        {
            this.setPos2(pos);
        }
    }

    public void setCoordinate(int value, Corner corner, CoordinateType type)
    {
        BlockPos pos = this.getPosition(corner);
        pos = PositionUtils.getModifiedPosition(pos, value, type);
        this.setPosition(pos, corner);
    }

    @Nullable
    public static Box fromJson(JsonObject obj)
    {
        if (JsonUtils.hasString(obj, "name"))
        {
            BlockPos pos1 = JsonUtils.blockPosFromJson(obj, "pos1");
            BlockPos pos2 = JsonUtils.blockPosFromJson(obj, "pos2");

            if (pos1 != null || pos2 != null)
            {
                Box box = new Box();
                box.setName(obj.get("name").getAsString());

                if (pos1 != null)
                {
                    box.setPos1(pos1);
                }

                if (pos2 != null)
                {
                    box.setPos2(pos2);
                }

                return box;
            }
        }

        return null;
    }

    @Nullable
    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        if (this.pos1 != null)
        {
            obj.add("pos1", JsonUtils.blockPosToJson(this.pos1));
        }

        if (this.pos2 != null)
        {
            obj.add("pos2", JsonUtils.blockPosToJson(this.pos2));
        }

        obj.add("name", new JsonPrimitive(this.name));

        return this.pos1 != null || this.pos2 != null ? obj : null;
    }
}
