package fi.dy.masa.litematica.gui.widgets;

import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import fi.dy.masa.malilib.render.GuiContext;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.Pair;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.data.Schema;
import com.mojang.blaze3d.platform.NativeImage;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicBrowserBase;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.materials.MaterialListCustom;
import fi.dy.masa.litematica.materials.MaterialListPreview;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.schematic.SchematicSchema;
import fi.dy.masa.litematica.util.FileType;

public class WidgetSchematicBrowser extends WidgetFileBrowserBase
{
    protected static final FileFilter SCHEMATIC_FILTER = new FileFilterSchematics();

    protected final Map<Path, SchematicMetadata> cachedMetadata = new HashMap<>();
    protected final Map<Path, SchematicSchema> cachedVersion = new HashMap<>();
    protected final Map<Path, Pair<Identifier, DynamicTexture>> cachedPreviewImages = new HashMap<>();
    protected final Map<Path, MaterialListPreview> cachedMatsMetadata = new HashMap<>();
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
    public void onClose()
    {
        super.onClose();

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
    protected void drawAdditionalContents(GuiContext ctx, int mouseX, int mouseY)
    {
        this.drawSelectedSchematicInfo(ctx, this.getLastSelectedEntry());
    }

    protected void drawSelectedSchematicInfo(GuiContext ctx, @Nullable DirectoryEntry entry)
    {
        int x = this.posX + this.totalWidth - this.infoWidth;
        int y = this.posY;
        int height = Math.min(this.infoHeight, this.parent.getMaxInfoHeight());

        RenderUtils.drawOutlinedBox(ctx, x, y, this.infoWidth, height, 0xA0000000, COLOR_HORIZONTAL_BAR);

        if (entry == null)
        {
            return;
        }

        FileType type = FileType.fromName(entry.getName());
        boolean matType = type == FileType.JSON || type == FileType.TEXT;
        boolean schemType = type == FileType.LITEMATICA_SCHEMATIC || type == FileType.SPONGE_SCHEMATIC || type == FileType.VANILLA_STRUCTURE;
        Pair<SchematicSchema, SchematicMetadata> metaPair = schemType ? this.getSchematicVersionAndMetadata(entry) : null;
        MaterialListPreview listData = matType ? this.getMaterialListPreview(entry) : null;
        SchematicMetadata meta = null;
        SchematicSchema version = null;

        if (metaPair != null)
        {
            meta = metaPair.getRight();
            version = metaPair.getLeft();
        }

        if (meta == null && listData == null)
        {
            return;
        }

        if (meta != null)
        {
            x += 3;
            y += 3;
            int textColor = 0xC0C0C0C0;
            int valueColor = 0xFFFFFFFF;

            String str = StringUtils.translate("litematica.gui.label.schematic_info.name");
            this.drawString(ctx, str, x, y, textColor);
            y += 12;

            this.drawString(ctx, meta.getName(), x + 4, y, valueColor);
            y += 12;

            str = StringUtils.translate("litematica.gui.label.schematic_info.schematic_author", meta.getAuthor());
            this.drawString(ctx, str, x, y, textColor);
            y += 12;

            String strDate = DATE_FORMAT.format(new Date(meta.getTimeCreated()));
            str = StringUtils.translate("litematica.gui.label.schematic_info.time_created", strDate);
            this.drawString(ctx, str, x, y, textColor);
            y += 12;

            if (meta.hasBeenModified())
            {
                strDate = DATE_FORMAT.format(new Date(meta.getTimeModified()));
                str = StringUtils.translate("litematica.gui.label.schematic_info.time_modified", strDate);
                this.drawString(ctx, str, x, y, textColor);
                y += 12;
            }

            str = StringUtils.translate("litematica.gui.label.schematic_info.region_count", meta.getRegionCount());
            this.drawString(ctx, str, x, y, textColor);
            y += 12;

            if (this.parent.getScreenHeight() >= 340)
            {
                str = StringUtils.translate("litematica.gui.label.schematic_info.total_volume", meta.getTotalVolume());
                this.drawString(ctx, str, x, y, textColor);
                y += 12;

                if (meta.getTotalBlocks() > 0)
                {
                    str = StringUtils.translate("litematica.gui.label.schematic_info.total_blocks", meta.getTotalBlocks());
                    this.drawString(ctx, str, x, y, textColor);
                    y += 12;
                }

                str = StringUtils.translate("litematica.gui.label.schematic_info.enclosing_size");
                this.drawString(ctx, str, x, y, textColor);
                y += 12;

                Vec3i areaSize = meta.getEnclosingSize();
                String tmp = String.format("%d x %d x %d", areaSize.getX(), areaSize.getY(), areaSize.getZ());
                this.drawString(ctx, tmp, x + 4, y, valueColor);
                y += 12;
            }
            else
            {
                if (meta.getTotalBlocks() > 0)
                {
                    str = StringUtils.translate("litematica.gui.label.schematic_info.total_blocks_and_volume", meta.getTotalBlocks(), meta.getTotalVolume());
                    this.drawString(ctx, str, x, y, textColor);
                    y += 12;
                }
                else
                {
                    str = StringUtils.translate("litematica.gui.label.schematic_info.total_volume", meta.getTotalVolume());
                    this.drawString(ctx, str, x, y, textColor);
                    y += 12;
                }

                Vec3i areaSize = meta.getEnclosingSize();
                String tmp = String.format("%d x %d x %d", areaSize.getX(), areaSize.getY(), areaSize.getZ());
                str = StringUtils.translate("litematica.gui.label.schematic_info.enclosing_size_value", tmp);
                this.drawString(ctx, str, x, y, textColor);
                y += 12;
            }

            if (version != null)
            {
                switch (meta.getFileType())
                {
                    case LITEMATICA_SCHEMATIC ->
                    {
                        str = StringUtils.translate("litematica.gui.label.schematic_info.version", version.litematicVersion());
                        this.drawString(ctx, str, x, y, textColor);
                        y += 12;
                    }
                    case SPONGE_SCHEMATIC ->
                    {
                        str = StringUtils.translate("litematica.gui.label.schematic_info.sponge_version", version.litematicVersion());
                        this.drawString(ctx, str, x, y, textColor);
                        y += 12;
                    }
                    case VANILLA_STRUCTURE ->
                    {
                        str = StringUtils.translate("litematica.gui.label.schematic_info.vanilla_version");
                        this.drawString(ctx, str, x, y, textColor);
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
                    this.drawString(ctx, str, x, y, textColor);
                    y += 12;
                }
            }

            /*
            str = StringUtils.translate("litematica.gui.label.schematic_info.description");
            this.drawString(x, y, textColor, str);
            */
            //y += 12;

            Pair<Identifier, DynamicTexture> pair = this.cachedPreviewImages.get(entry.getFullPath());

            if (pair != null && pair.getRight().getPixels() != null)
            {
                //y += 14;
                y += 12;

                int iconSize = pair.getRight().getPixels().getWidth();
                boolean needsScaling = height < this.infoHeight;

                if (needsScaling)
                {
                    iconSize = height - y + this.posY - 6;
                }

                RenderUtils.drawOutlinedBox(ctx, x + 4, y, iconSize, iconSize, 0xA0000000, COLOR_HORIZONTAL_BAR);

                ctx.blit(RenderPipelines.GUI_TEXTURED, pair.getLeft(), x + 4, y, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
            }
        }
        else        // Material List Info Panel
        {
            x += 3;
            y += 3;
            int textColor = 0xC0C0C0C0;
            int valueColor = 0xFFFFFFFF;

            String str = StringUtils.translate("litematica.gui.label.material_list_info.title_colon");
            this.drawString(ctx, str, x, y, textColor);
            y += 12;

            this.drawString(ctx, FileType.getString(listData.type()), x + 4, y, valueColor);
            y += 12;

            str = StringUtils.translate("litematica.gui.label.material_list_info.name");
            this.drawString(ctx, str, x, y, textColor);
            y += 12;

            this.drawString(ctx, listData.name(), x + 4, y, valueColor);
            y += 12;

            str = StringUtils.translate("litematica.gui.label.material_list_info.item_count");
            this.drawString(ctx, str, x, y, textColor);
            y += 12;

            str = String.format("%03d", listData.itemCount());
            this.drawString(ctx, str, x + 4, y, valueColor);
            y += 12;
        }
    }

    public void clearSchematicMetadataCache()
    {
        this.clearPreviewImages();
        this.cachedMetadata.clear();
        this.cachedPreviewImages.clear();
        this.cachedVersion.clear();
        this.cachedMatsMetadata.clear();
    }

    @Deprecated
    @Nullable
    protected SchematicMetadata getSchematicMetadata(DirectoryEntry entry)
    {
        Path file = entry.getDirectory().resolve(entry.name());
        SchematicMetadata meta = this.cachedMetadata.get(file);

        if (meta == null && !this.cachedMetadata.containsKey(file))
        {
            if (entry.name().endsWith(LitematicaSchematic.FILE_EXTENSION))
            {
                meta = LitematicaSchematic.readMetadataFromFile(entry.getDirectory(), entry.name());

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
        Path file = entry.getDirectory().resolve(entry.name());
        SchematicMetadata meta = this.cachedMetadata.get(file);
        SchematicSchema version = this.cachedVersion.get(file);

        if (meta == null && !this.cachedMetadata.containsKey(file))
        {
            Pair<SchematicSchema, SchematicMetadata> pair = LitematicaSchematic.readMetadataAndVersionFromFile(entry.getDirectory(), entry.name());

            if (pair != null)
            {
                meta = pair.getRight();
                version = pair.getLeft();

                if (entry.name().endsWith(LitematicaSchematic.FILE_EXTENSION))
                {
                    this.createPreviewImage(file, meta);
                }

                this.cachedMetadata.put(file, meta);
                this.cachedVersion.put(file, version);
            }
        }

        return Pair.of(version, meta);
    }

    @Nullable
    protected MaterialListPreview getMaterialListPreview(DirectoryEntry entry)
    {
        Path file = entry.getDirectory().resolve(entry.getName());
        MaterialListPreview meta = this.cachedMatsMetadata.get(file);

        if (meta == null && !this.cachedMatsMetadata.containsKey(file))
        {
            MaterialListCustom data = MaterialListCustom.fromFile(file);

            if (data != null)
            {
                meta = new MaterialListPreview(FileType.fromFile(file), data.getName(), data.getMaterialsAll().size());
                this.cachedMatsMetadata.put(file, meta);
            }
        }

        return meta;
    }

    private void clearPreviewImages()
    {
        for (Pair<Identifier, DynamicTexture> pair : this.cachedPreviewImages.values())
        {
            this.mc.getTextureManager().release(pair.getLeft());
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
                    Identifier rl = Identifier.fromNamespaceAndPath(Reference.MOD_ID, DigestUtils.sha1Hex(file.toAbsolutePath().toString()));
                    DynamicTexture tex = new DynamicTexture(rl::toString, image);
                    this.mc.getTextureManager().register(rl, tex);

                    for (int y = 0, i = 0; y < size; ++y)
                    {
                        for (int x = 0; x < size; ++x)
                        {
                            int val = previewImageData[i++];
                            // Swap the color channels from ARGB to ABGR
                            //val = (val & 0xFF00FF00) | (val & 0xFF0000) >> 16 | (val & 0xFF) << 16;
                            image.setPixel(x, y, val);
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
                    name.endsWith(MaterialListCustom.JSON_FILE_EXTENSION) ||
                    name.endsWith(MaterialListCustom.TEXT_FILE_EXTENSION);
        }
    }
}
