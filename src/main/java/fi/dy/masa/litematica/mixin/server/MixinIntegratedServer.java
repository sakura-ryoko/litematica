package fi.dy.masa.litematica.mixin.server;

@Deprecated
//@Mixin(IntegratedServer.class)
public abstract class MixinIntegratedServer
        //extends MinecraftServer
{
    /*
    private MixinIntegratedServer(Thread serverThread,
                                  LevelStorage.Session session,
                                  ResourcePackManager dataPackManager,
                                  SaveLoader saveLoader,
                                  Proxy proxy,
                                  DataFixer dataFixer,
                                  ApiServices apiServices,
                                  WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory)
    {
        super(serverThread, session, dataPackManager, saveLoader, proxy, dataFixer, apiServices, worldGenerationProgressListenerFactory);
    }
     */

    /*
    @Inject(method = "tick", at = @At(value = "INVOKE", shift = Shift.AFTER,
            target = "Lnet/minecraft/server/MinecraftServer;tick(Ljava/util/function/BooleanSupplier;)V"))
    private void onPostTick(BooleanSupplier supplier, CallbackInfo ci)
    {
        TaskScheduler.getInstanceServer().runTasks();
    }
     */
}
