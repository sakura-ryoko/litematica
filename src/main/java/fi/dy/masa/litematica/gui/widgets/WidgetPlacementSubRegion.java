package fi.dy.masa.litematica.gui.widgets;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import fi.dy.masa.malilib.render.GuiContext;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.litematica.gui.GuiSubRegionConfiguration;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.PositionUtils;

public class WidgetPlacementSubRegion extends WidgetListEntryBase<SubRegionPlacement>
{
    private final SchematicPlacement schematicPlacement;
    private final WidgetListPlacementSubRegions parent;
    private final SubRegionPlacement placement;
    private final boolean isOdd;
    private final int buttonsStartX;

    public WidgetPlacementSubRegion(int x, int y, int width, int height, boolean isOdd,
            SchematicPlacement schematicPlacement, SubRegionPlacement placement, int listIndex,
            WidgetListPlacementSubRegions parent)
    {
        super(x, y, width, height, placement, listIndex);

        this.parent = parent;
        this.schematicPlacement = schematicPlacement;
        this.placement = placement;
        this.isOdd = isOdd;

        int posX = x + width - 2;
        int posY = y + 1;

        // Note: These are placed from right to left

        posX = this.createButtonOnOff(posX, posY, this.placement.isEnabled(), WidgetSchematicPlacement.ButtonListener.ButtonType.TOGGLE_ENABLED);
        posX = this.createButtonGeneric(posX, posY, WidgetSchematicPlacement.ButtonListener.ButtonType.CONFIGURE);

        this.buttonsStartX = posX;
    }

    private int createButtonGeneric(int xRight, int y, WidgetSchematicPlacement.ButtonListener.ButtonType type)
    {
        String label = StringUtils.translate(type.getTranslationKey());
        int len = this.getStringWidth(label) + 10;
        xRight -= len;
        this.addButton(new ButtonGeneric(xRight, y, len, 20, label), new ButtonListener(type, this));

        return xRight - 2;
    }

    private int createButtonOnOff(int xRight, int y, boolean isCurrentlyOn, WidgetSchematicPlacement.ButtonListener.ButtonType type)
    {
        ButtonOnOff button = new ButtonOnOff(xRight, y, -1, true, type.getTranslationKey(), isCurrentlyOn);
        this.addButton(button, new ButtonListener(type, this));

        return xRight - button.getWidth() - 2;
    }

    @Override
    public boolean canSelectAt(MouseButtonEvent click)
    {
        return click.x() < this.buttonsStartX && super.canSelectAt(click);
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        boolean placementSelected = this.schematicPlacement.getSelectedSubRegionPlacement() == this.placement;

        // Draw a lighter background for the hovered and the selected entry
        if (selected || placementSelected || this.isMouseOver(mouseX, mouseY))
        {
            RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height, 0xA0707070);
        }
        else if (this.isOdd)
        {
            RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height, 0xA0101010);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height, 0xA0303030);
        }

        if (placementSelected)
        {
            RenderUtils.drawOutline(ctx, this.x, this.y, this.width, this.height, 0xFFE0E0E0);
        }

        String name = this.placement.getName();
        String pre = this.placement.isEnabled() ? GuiBase.TXT_GREEN : GuiBase.TXT_RED;
        this.drawString(ctx, this.x + 20, this.y + 7, 0xFFFFFFFF, pre + name);

        Icons icon;

        if (this.schematicPlacement.getSchematic().getFile() != null)
        {
            icon = Icons.SCHEMATIC_TYPE_FILE;
        }
        else
        {
            icon = Icons.SCHEMATIC_TYPE_MEMORY;
        }

        icon.renderAt(ctx, this.x + 2, this.y + 5, this.zLevel, false, false);

        if (this.placement.isRegionPlacementModifiedFromDefault())
        {
            icon = Icons.NOTICE_EXCLAMATION_11;
            icon.renderAt(ctx, this.buttonsStartX - icon.getWidth() - 2, this.y + 6, this.zLevel, false, false);
        }

        super.render(ctx, mouseX, mouseY, placementSelected);
    }

    @Override
    public void postRenderHovered(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        LitematicaSchematic schematic = this.schematicPlacement.getSchematic();
        Path schematicFile = schematic.getFile();
        String fileName = schematicFile != null ? schematicFile.getFileName().toString() : StringUtils.translate("litematica.gui.label.schematic_placement.in_memory");

        if (this.placement.isRegionPlacementModifiedFromDefault() &&
            GuiBase.isMouseOver(mouseX, mouseY, this.x + this.buttonsStartX - 25, this.y + 6, 11, 11))
        {
            String str = StringUtils.translate("litematica.hud.schematic_placement.hover_info.placement_sub_region_modified");
            RenderUtils.drawHoverText(ctx, mouseX, mouseY, ImmutableList.of(str));
        }
        else if (GuiBase.isMouseOver(mouseX, mouseY, this.x, this.y, this.buttonsStartX - 14, this.height))
        {
            List<String> text = new ArrayList<>();
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.schematic_name", schematic.getMetadata().getName()));
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.schematic_file", fileName));

            BlockPos o = this.placement.getPos();
            o = PositionUtils.getTransformedBlockPos(o, this.schematicPlacement.getMirror(), this.schematicPlacement.getRotation());
            o = o.offset(this.schematicPlacement.getOrigin());
            String strOrigin = String.format("x: %d, y: %d, z: %d", o.getX(), o.getY(), o.getZ());
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.origin", strOrigin));

            Vec3i size = schematic.getAreaSize(this.placement.getName());

            if (size != null)
            {
                String strSize = String.format("%d x %d x %d", size.getX(), size.getY(), size.getZ());
                text.add(StringUtils.translate("litematica.gui.label.placement_sub.region_size", strSize));
            }

            RenderUtils.drawHoverText(ctx, mouseX, mouseY, text);
        }
    }

	private record ButtonListener(WidgetSchematicPlacement.ButtonListener.ButtonType type, WidgetPlacementSubRegion widget)
            implements IButtonActionListener
	{
		@Override
		public void actionPerformedWithButton(ButtonBase button, int mouseButton)
		{
			if (this.type == WidgetSchematicPlacement.ButtonListener.ButtonType.CONFIGURE)
			{
				GuiSubRegionConfiguration gui
						= new GuiSubRegionConfiguration(this.widget.schematicPlacement, this.widget.placement);
				gui.setParent(this.widget.parent.getParentGui());
				GuiBase.openGui(gui);
			}
			else if (this.type == WidgetSchematicPlacement.ButtonListener.ButtonType.TOGGLE_ENABLED)
			{
				this.widget.schematicPlacement.toggleSubRegionEnabled(this.widget.placement.getName(), this.widget.parent.getParentGui());
				this.widget.parent.refreshEntries();
			}
		}
	}
}
