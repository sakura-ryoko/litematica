package fi.dy.masa.litematica.gui.widgets;

import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.Pair;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.data.Schema;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicBrowserBase;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.schematic.SchematicSchema;

public class WidgetSchematicBrowser extends WidgetFileBrowserBase
{
    protected static final FileFilter SCHEMATIC_FILTER = new FileFilterSchematics();

    protected final Map<Path, SchematicMetadata> cachedMetadata = new HashMap<>();
    protected final Map<Path, SchematicSchema> cachedVersion = new HashMap<>();
    protected final Map<Path, Pair<Identifier, NativeImageBackedTexture>> cachedPreviewImages = new HashMap<>();
    protected final GuiSchematicBrowserBase parent;
    protected final int infoWidth;
    protected final int infoHeight;

    public WidgetSchematicBrowser(int x, int y, int width, int height, GuiSchematicBrowserBase parent, @Nullable ISelectionListener<DirectoryEntry> selectionListener)
    {
        super(x, y, width, height, DataManager.getDirectoryCache(), parent.getBrowserContext(),
                parent.getDefaultDirectory(), selectionListener, Icons.FILE_ICON_LITEMATIC);

        this.title = StringUtils.translate("litematica.gui.title.schematic_browser");
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
    public void close()
    {
        super.close();

        this.clearPreviewImages();
    }

    @Override
    protected Path getRootDirectory()
    {
        return DataManager.getSchematicsBaseDirectory();
    }

    @Override
    protected FileFilter getFileFilter()
    {
        return SCHEMATIC_FILTER;
    }

    @Override
    protected void drawAdditionalContents(DrawContext drawContext, int mouseX, int mouseY)
    {
        this.drawSelectedSchematicInfo(drawContext, this.getLastSelectedEntry());
    }

    protected void drawSelectedSchematicInfo(DrawContext drawContext, @Nullable DirectoryEntry entry)
    {
        int x = this.posX + this.totalWidth - this.infoWidth;
        int y = this.posY;
        int height = Math.min(this.infoHeight, this.parent.getMaxInfoHeight());

        RenderUtils.drawOutlinedBox(drawContext, x, y, this.infoWidth, height, 0xA0000000, COLOR_HORIZONTAL_BAR);

        if (entry == null)
        {
            return;
        }

        Pair<SchematicSchema, SchematicMetadata> metaPair = this.getSchematicVersionAndMetadata(entry);
        SchematicMetadata meta;
        SchematicSchema version;

        if (metaPair != null)
        {
            meta = metaPair.getRight();
            version = metaPair.getLeft();
        }
        else
        {
            return;
        }

        if (meta != null)
        {
//            RenderUtils.color(1f, 1f, 1f, 1f);

            x += 3;
            y += 3;
            int textColor = 0xC0C0C0C0;
            int valueColor = 0xFFFFFFFF;

            String str = StringUtils.translate("litematica.gui.label.schematic_info.name");
            this.drawString(drawContext, str, x, y, textColor);
            y += 12;

            this.drawString(drawContext, meta.getName(), x + 4, y, valueColor);
            y += 12;

            str = StringUtils.translate("litematica.gui.label.schematic_info.schematic_author", meta.getAuthor());
            this.drawString(drawContext, str, x, y, textColor);
            y += 12;

            String strDate = DATE_FORMAT.format(new Date(meta.getTimeCreated()));
            str = StringUtils.translate("litematica.gui.label.schematic_info.time_created", strDate);
            this.drawString(drawContext, str, x, y, textColor);
            y += 12;

            if (meta.hasBeenModified())
            {
                strDate = DATE_FORMAT.format(new Date(meta.getTimeModified()));
                str = StringUtils.translate("litematica.gui.label.schematic_info.time_modified", strDate);
                this.drawString(drawContext, str, x, y, textColor);
                y += 12;
            }

            str = StringUtils.translate("litematica.gui.label.schematic_info.region_count", meta.getRegionCount());
            this.drawString(drawContext, str, x, y, textColor);
            y += 12;

            if (this.parent.getScreenHeight() >= 340)
            {
                str = StringUtils.translate("litematica.gui.label.schematic_info.total_volume", meta.getTotalVolume());
                this.drawString(drawContext, str, x, y, textColor);
                y += 12;

                if (meta.getTotalBlocks() > 0)
                {
                    str = StringUtils.translate("litematica.gui.label.schematic_info.total_blocks", meta.getTotalBlocks());
                    this.drawString(drawContext, str, x, y, textColor);
                    y += 12;
                }

                str = StringUtils.translate("litematica.gui.label.schematic_info.enclosing_size");
                this.drawString(drawContext, str, x, y, textColor);
                y += 12;

                Vec3i areaSize = meta.getEnclosingSize();
                String tmp = String.format("%d x %d x %d", areaSize.getX(), areaSize.getY(), areaSize.getZ());
                this.drawString(drawContext, tmp, x + 4, y, valueColor);
                y += 12;
            }
            else
            {
                if (meta.getTotalBlocks() > 0)
                {
                    str = StringUtils.translate("litematica.gui.label.schematic_info.total_blocks_and_volume", meta.getTotalBlocks(), meta.getTotalVolume());
                    this.drawString(drawContext, str, x, y, textColor);
                    y += 12;
                }
                else
                {
                    str = StringUtils.translate("litematica.gui.label.schematic_info.total_volume", meta.getTotalVolume());
                    this.drawString(drawContext, str, x, y, textColor);
                    y += 12;
                }

                Vec3i areaSize = meta.getEnclosingSize();
                String tmp = String.format("%d x %d x %d", areaSize.getX(), areaSize.getY(), areaSize.getZ());
                str = StringUtils.translate("litematica.gui.label.schematic_info.enclosing_size_value", tmp);
                this.drawString(drawContext, str, x, y, textColor);
                y += 12;
            }

            if (version != null)
            {
                switch (meta.getFileType())
                {
                    case LITEMATICA_SCHEMATIC ->
                    {
                        str = StringUtils.translate("litematica.gui.label.schematic_info.version", version.litematicVersion());
                        this.drawString(drawContext, str, x, y, textColor);
                        y += 12;
                    }
                    case SPONGE_SCHEMATIC ->
                    {
                        str = StringUtils.translate("litematica.gui.label.schematic_info.sponge_version", version.litematicVersion());
                        this.drawString(drawContext, str, x, y, textColor);
                        y += 12;
                    }
                    case VANILLA_STRUCTURE ->
                    {
                        str = StringUtils.translate("litematica.gui.label.schematic_info.vanilla_version");
                        this.drawString(drawContext, str, x, y, textColor);
                        y += 12;
                    }
                    // Not supported
//                    case SCHEMATICA_SCHEMATIC ->  {}
                }

                Schema schema = Schema.getSchemaByDataVersion(version.minecraftDataVersion());

                if (schema != null)
                {
                    if (version.minecraftDataVersion() - LitematicaSchematic.MINECRAFT_DATA_VERSION > 100)
                    {
                        str = StringUtils.translate("litematica.gui.label.schematic_info.schema.newer", schema.getString(), version.minecraftDataVersion());
                    }
                    else
                    {
                        str = StringUtils.translate("litematica.gui.label.schematic_info.schema", schema.getString(), version.minecraftDataVersion());
                    }
                    this.drawString(drawContext, str, x, y, textColor);
                    y += 12;
                }
            }

            /*
            str = StringUtils.translate("litematica.gui.label.schematic_info.description");
            this.drawString(x, y, textColor, str);
            */
            //y += 12;

            Pair<Identifier, NativeImageBackedTexture> pair = this.cachedPreviewImages.get(entry.getFullPath());

            if (pair != null)
            {
                //y += 14;
                y += 12;

                int iconSize = pair.getRight().getImage().getWidth();
                boolean needsScaling = height < this.infoHeight;

//                RenderUtils.color(1f, 1f, 1f, 1f);

                if (needsScaling)
                {
                    iconSize = height - y + this.posY - 6;
                }

                RenderUtils.drawOutlinedBox(drawContext, x + 4, y, iconSize, iconSize, 0xA0000000, COLOR_HORIZONTAL_BAR);

                drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, pair.getLeft(), x + 4, y, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
            }
        }
    }

    public void clearSchematicMetadataCache()
    {
        this.clearPreviewImages();
        this.cachedMetadata.clear();
        this.cachedPreviewImages.clear();
        this.cachedVersion.clear();
    }

    @Deprecated
    @Nullable
    protected SchematicMetadata getSchematicMetadata(DirectoryEntry entry)
    {
        Path file = entry.getDirectory().resolve(entry.getName());
        SchematicMetadata meta = this.cachedMetadata.get(file);

        if (meta == null && this.cachedMetadata.containsKey(file) == false)
        {
            if (entry.getName().endsWith(LitematicaSchematic.FILE_EXTENSION))
            {
                meta = LitematicaSchematic.readMetadataFromFile(entry.getDirectory(), entry.getName());

                if (meta != null)
                {
                    this.createPreviewImage(file, meta);
                }
            }

            this.cachedMetadata.put(file, meta);
        }

        return meta;
    }

    @Nullable
    protected Pair<SchematicSchema, SchematicMetadata> getSchematicVersionAndMetadata(DirectoryEntry entry)
    {
        Path file = entry.getDirectory().resolve(entry.getName());
        SchematicMetadata meta = this.cachedMetadata.get(file);
        SchematicSchema version = this.cachedVersion.get(file);

        if (meta == null && this.cachedMetadata.containsKey(file) == false)
        {
            Pair<SchematicSchema, SchematicMetadata> pair = LitematicaSchematic.readMetadataAndVersionFromFile(entry.getDirectory(), entry.getName());

            if (pair != null)
            {
                meta = pair.getRight();
                version = pair.getLeft();

                if (entry.getName().endsWith(LitematicaSchematic.FILE_EXTENSION))
                {
                    this.createPreviewImage(file, meta);
                }

                this.cachedMetadata.put(file, meta);
                this.cachedVersion.put(file, version);
            }
        }

        return Pair.of(version, meta);
    }

    private void clearPreviewImages()
    {
        for (Pair<Identifier, NativeImageBackedTexture> pair : this.cachedPreviewImages.values())
        {
            this.mc.getTextureManager().destroyTexture(pair.getLeft());
        }
    }

    private void createPreviewImage(Path file, SchematicMetadata meta)
    {
        int[] previewImageData = meta.getPreviewImagePixelData();

        if (previewImageData != null && previewImageData.length > 0)
        {
            int size = (int) Math.sqrt(previewImageData.length);

            if ((size * size) == previewImageData.length)
            {
                try
                {
                    NativeImage image = new NativeImage(size, size, false);
                    Identifier rl = Identifier.of(Reference.MOD_ID, DigestUtils.sha1Hex(file.toAbsolutePath().toString()));
                    NativeImageBackedTexture tex = new NativeImageBackedTexture(rl::toString, image);
                    this.mc.getTextureManager().registerTexture(rl, tex);

                    for (int y = 0, i = 0; y < size; ++y)
                    {
                        for (int x = 0; x < size; ++x)
                        {
                            int val = previewImageData[i++];
                            // Swap the color channels from ARGB to ABGR
                            //val = (val & 0xFF00FF00) | (val & 0xFF0000) >> 16 | (val & 0xFF) << 16;
                            image.setColorArgb(x, y, val);
                        }
                    }

                    tex.upload();

                    this.cachedPreviewImages.put(file, Pair.of(rl, tex));
                }
                catch (Exception e)
                {
                    Litematica.LOGGER.warn("Failed to create a preview image", e);
                }
            }
        }
    }

    public static class FileFilterSchematics extends FileFilter
    {
        @Override
        public boolean accept(Path pathName)
        {
            String name = pathName.getFileName().toString();

            return  name.endsWith(".litematic") ||
                    name.endsWith(".schem") ||
                    name.endsWith(".schematic") ||
                    name.endsWith(".nbt") ||
                    name.endsWith(".json") ||
                    name.endsWith(".txt");
        }
    }
}
