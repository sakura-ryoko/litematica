package fi.dy.masa.litematica.gui;

import java.nio.file.Path;
import javax.annotation.Nullable;

import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetDirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser;

public abstract class GuiSchematicBrowserBase extends GuiListBase<DirectoryEntry, WidgetDirectoryEntry, WidgetSchematicBrowser>
{
    public GuiSchematicBrowserBase(int browserX, int browserY)
    {
        super(browserX, browserY);
    }

    @Override
    protected WidgetSchematicBrowser createListWidget(int listX, int listY)
    {
        // The width and height will be set to the actual values in initGui()
        return new WidgetSchematicBrowser(listX, listY, 100, 100, this, this.getSelectionListener());
    }

    /**
     * This is the string the DataManager uses for saving/loading/storing the last used directory
     * for each browser GUI type/context.
     * @return ()
     */
    public abstract String getBrowserContext();

    public abstract Path getDefaultDirectory();

    @Override
    @Nullable
    protected ISelectionListener<DirectoryEntry> getSelectionListener()
    {
        return null;
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.getScreenWidth() - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.getScreenHeight() - 70;
    }

    public int getMaxInfoHeight()
    {
        return this.getBrowserHeight();
    }
}
