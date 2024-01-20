package fi.dy.masa.litematica.network;

import net.minecraft.util.Identifier;

public class CarpetHelloPacketHandler
{
    public static final Identifier HELLO_CHANNEL = new Identifier("carpet", "hello");
    // See CarpetClient.java for (ResourceLocation == Identifier):
    // --> public static final ResourceLocation CARPET_CHANNEL = new ResourceLocation("carpet", "hello");
}
