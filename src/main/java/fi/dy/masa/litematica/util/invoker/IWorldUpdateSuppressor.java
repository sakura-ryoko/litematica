package fi.dy.masa.litematica.util.invoker;

public interface IWorldUpdateSuppressor
{
    boolean litematica_getShouldPreventBlockUpdates();

    void litematica_setShouldPreventBlockUpdates(boolean preventUpdates);
}
