package fi.dy.masa.litematica.util;

public interface IWorldUpdateSuppressor
{
    boolean litematica$getShouldPreventBlockUpdates();

    void litematica$setShouldPreventBlockUpdates(boolean preventUpdates);
}
