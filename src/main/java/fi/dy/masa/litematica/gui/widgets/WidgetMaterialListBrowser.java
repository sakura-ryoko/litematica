package fi.dy.masa.litematica.gui.widgets;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMaterialListBrowserBase;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.materials.MaterialListCustom;
import fi.dy.masa.litematica.materials.MaterialListPreview;
import fi.dy.masa.litematica.util.FileType;

public class WidgetMaterialListBrowser extends WidgetFileBrowserBase
{
    protected static final FileFilter MATERIAL_LIST_FILTER = new FileFilterMaterials();

    protected final Map<Path, MaterialListPreview> cachedMetadata = new HashMap<>();
    protected final GuiMaterialListBrowserBase parent;
    protected final int infoWidth;
    protected final int infoHeight;

    public WidgetMaterialListBrowser(int x, int y, int width, int height, GuiMaterialListBrowserBase parent, @Nullable ISelectionListener<DirectoryEntry> selectionListener)
    {
        super(x, y, width, height, DataManager.getDirectoryCache(), parent.getBrowserContext(),
                parent.getDefaultDirectory(), selectionListener, Icons.FILE_ICON_TEXT);

        this.title = StringUtils.translate("litematica.gui.title.material_list_browser");
        this.infoWidth = 170;
        this.infoHeight = 310;
        this.parent = parent;
    }

    @Override
    protected int getBrowserWidthForTotalWidth(int width)
    {
        return super.getBrowserWidthForTotalWidth(width) - this.infoWidth;
    }

    @Override
    protected Path getRootDirectory()
    {
        return DataManager.getSchematicsBaseDirectory();
    }

    @Override
    protected FileFilter getFileFilter()
    {
        return MATERIAL_LIST_FILTER;
    }

    @Override
    protected void drawAdditionalContents(GuiContext ctx, int mouseX, int mouseY)
    {
        this.drawSelectedMaterialListInfo(ctx, this.getLastSelectedEntry());
    }

    protected void drawSelectedMaterialListInfo(GuiContext ctx, @Nullable DirectoryEntry entry)
    {
        int x = this.posX + this.totalWidth - this.infoWidth;
        int y = this.posY;
        int height = Math.min(this.infoHeight, this.parent.getMaxInfoHeight());

        RenderUtils.drawOutlinedBox(ctx, x, y, this.infoWidth, height, 0xA0000000, COLOR_HORIZONTAL_BAR);

        if (entry == null)
        {
            return;
        }

        MaterialListPreview data = this.getMaterialListPreview(entry);

        if (data == null)
        {
            return;
        }

	    x += 3;
	    y += 3;
	    int textColor = 0xC0C0C0C0;
	    int valueColor = 0xFFFFFFFF;

        String str = StringUtils.translate("litematica.gui.label.material_list_info.title_colon");
        this.drawString(ctx, str, x, y, textColor);
        y += 12;

        this.drawString(ctx, FileType.getString(data.type()), x + 4, y, valueColor);
        y += 12;

	    str = StringUtils.translate("litematica.gui.label.material_list_info.name");
	    this.drawString(ctx, str, x, y, textColor);
	    y += 12;

	    this.drawString(ctx, data.name(), x + 4, y, valueColor);
	    y += 12;

	    str = StringUtils.translate("litematica.gui.label.material_list_info.item_count");
	    this.drawString(ctx, str, x, y, textColor);
	    y += 12;

	    str = String.format("%03d", data.itemCount());
	    this.drawString(ctx, str, x + 4, y, valueColor);
	    y += 12;
    }

    public void clearMetadataCache()
    {
        this.cachedMetadata.clear();
    }

    @Nullable
    protected MaterialListPreview getMaterialListPreview(DirectoryEntry entry)
    {
        Path file = entry.getDirectory().resolve(entry.getName());
        MaterialListPreview meta = this.cachedMetadata.get(file);

        if (meta == null && !this.cachedMetadata.containsKey(file))
        {
            MaterialListCustom data = MaterialListCustom.fromFile(file);

            if (data != null)
            {
                meta = new MaterialListPreview(FileType.fromFile(file), data.getName(), data.getMaterialsAll().size());
                this.cachedMetadata.put(file, meta);
            }
        }

        return meta;
    }

    public static class FileFilterMaterials extends FileFilter
    {
        @Override
        public boolean accept(Path pathName)
        {
            String name = pathName.getFileName().toString();

            return name.endsWith(MaterialListCustom.JSON_FILE_EXTENSION) ||
                   name.endsWith(MaterialListCustom.TEXT_FILE_EXTENSION);
        }
    }
}
