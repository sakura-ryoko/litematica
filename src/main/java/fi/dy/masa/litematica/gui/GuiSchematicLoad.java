package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.materials.MaterialListCustom;
import fi.dy.masa.litematica.materials.MaterialListSchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiStringListSelection;
import fi.dy.masa.malilib.gui.GuiTextInputFeedback;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.interfaces.IStringListConsumer;
import fi.dy.masa.malilib.gui.widgets.WidgetCheckBox;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.interfaces.IStringConsumerFeedback;
import fi.dy.masa.malilib.util.FileRenamer;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.StringUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;

public class GuiSchematicLoad extends GuiSchematicBrowserBase implements ISelectionListener<DirectoryEntry>
{
    public GuiSchematicLoad()
    {
        super(12, 24);

        this.title = StringUtils.translate("litematica.gui.title.load_schematic");
    }

    @Override
    public String getBrowserContext()
    {
        return "schematic_load";
    }

    @Override
    public Path getDefaultDirectory()
    {
        return DataManager.getSchematicsBaseDirectory();
    }

    @Override
    public int getMaxInfoHeight()
    {
        return this.getBrowserHeight() + 10;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        this.createButtons();
    }

	private void createButtons()
	{
		int x = 12;
		int y = this.getScreenHeight() - 40;
		int buttonWidth;
		String label;
		ButtonGeneric button;

		label = StringUtils.translate("litematica.gui.label.schematic_load.checkbox.create_placement");
		String hover = StringUtils.translate("litematica.gui.label.schematic_load.hoverinfo.create_placement");
		WidgetCheckBox checkbox = new WidgetCheckBox(x, y, Icons.CHECKBOX_UNSELECTED, Icons.CHECKBOX_SELECTED, label, hover);
		checkbox.setListener(new CheckboxListener());
		checkbox.setChecked(DataManager.getCreatePlacementOnLoad(), false);
		this.addWidget(checkbox);

		DirectoryEntry selected = this.getListWidget().getLastSelectedEntry();

		y = this.getScreenHeight() - 26;

		if (this.getListWidget() == null) return;
		if (selected != null)
		{
			FileType type = FileType.fromFile(selected.getFullPath());

			if (type == FileType.LITEMATICA_SCHEMATIC || type == FileType.SPONGE_SCHEMATIC ||
				type == FileType.SCHEMATICA_SCHEMATIC || type == FileType.VANILLA_STRUCTURE)
			{
				x += this.createButton(x, y, -1, ButtonListener.Type.LOAD_SCHEMATIC) + 4;
				x += this.createButton(x, y, -1, ButtonListener.Type.MATERIAL_LIST) + 4;
				x += this.createButton(x, y, -1, ButtonListener.Type.RENAME_SCHEMATIC) + 4;
				x += this.createButton(x, y, -1, ButtonListener.Type.RENAME_FILE) + 4;
			}
			else if (type == FileType.TEXT || type == FileType.JSON)
			{
				x += this.createButton(x, y, -1, ButtonListener.Type.MATERIAL_LIST) + 4;
				x += this.createButton(x, y, -1, ButtonListener.Type.RENAME_FILE) + 4;
			}
		}

		ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.LOADED_SCHEMATICS;
		label = StringUtils.translate(type.getLabelKey());
		buttonWidth = this.getStringWidth(label) + 30;
		button = new ButtonGeneric(x, y, buttonWidth, 20, label, type.getIcon());
		this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));

		type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
		label = StringUtils.translate(type.getLabelKey());
		buttonWidth = this.getStringWidth(label) + 20;
		x = this.getScreenWidth() - buttonWidth - 10;
		button = new ButtonGeneric(x, y, buttonWidth, 20, label);
		this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));
	}

    private int createButton(int x, int y, int width, ButtonListener.Type type)
    {
        ButtonListener listener = new ButtonListener(type, this);
        String label = StringUtils.translate(type.getTranslationKey());

        if (width == -1)
        {
            width = this.getStringWidth(label) + 10;
        }

        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, label);

        if (type == ButtonListener.Type.MATERIAL_LIST)
        {
            button.setHoverStrings(StringUtils.translate("litematica.gui.button.hover.material_list_shift_to_select_sub_regions"));
        }

        this.addButton(button, listener);

        return width;
    }

	@Override
	public void onSelectionChange(@Nullable WidgetFileBrowserBase.DirectoryEntry entry)
	{
		this.clearButtons();
		this.createButtons();
	}

	@Override
	protected ISelectionListener<DirectoryEntry> getSelectionListener()
	{
		return this;
	}

	private record ButtonListener(Type type, GuiSchematicLoad gui) implements IButtonActionListener
	{
		@Override
		public void actionPerformedWithButton(ButtonBase button, int mouseButton)
		{
			if (this.gui.getListWidget() == null) { return; }
			DirectoryEntry entry = this.gui.getListWidget().getLastSelectedEntry();

			if (entry == null)
			{
				this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_load.no_schematic_selected");
			}
			else
			{
				Path file = entry.getFullPath();

				if (!Files.exists(file) || !Files.isReadable(file))
				{
					this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_load.cant_read_file", file.getFileName());
					return;
				}

				this.gui.setNextMessageType(MessageType.ERROR);
				LitematicaSchematic schematic = null;
				FileType fileType = FileType.fromFile(entry.getFullPath());
				boolean warnType = false;

				// Handle custom item list files for material list button
				if (this.type == Type.MATERIAL_LIST && (fileType == FileType.JSON || fileType == FileType.TEXT))
				{
					MaterialListCustom customList = MaterialListCustom.fromFile(file);

					if (customList != null)
					{
						DataManager.setMaterialList(customList);
						GuiBase.openGui(new GuiMaterialList(customList));
						this.gui.addMessage(MessageType.SUCCESS, "litematica.info.material_list.custom_loaded", file.getFileName());
					}
					else
					{
						this.gui.addMessage(MessageType.ERROR, "litematica.error.material_list.custom_load_failed", file.getFileName());
					}

					return;
				}

				if (fileType == FileType.LITEMATICA_SCHEMATIC)
				{
					schematic = LitematicaSchematic.createFromFile(entry.getDirectory(), entry.name());
				}
				else if (fileType == FileType.SCHEMATICA_SCHEMATIC)
				{
					schematic
							= WorldUtils.convertSchematicaSchematicToLitematicaSchematic(entry.getDirectory(), entry.name(), false, this.gui);
					warnType = true;
				}
				else if (fileType == FileType.VANILLA_STRUCTURE)
				{
					schematic = WorldUtils.convertStructureToLitematicaSchematic(entry.getDirectory(), entry.name());
					warnType = true;
				}
				else if (fileType == FileType.SPONGE_SCHEMATIC)
				{
					schematic
							= WorldUtils.convertSpongeSchematicToLitematicaSchematic(entry.getDirectory(), entry.name());
					warnType = true;
				}
				else
				{
					this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_load.unsupported_type", file.getFileName());
				}

				if (schematic != null)
				{
					if (this.type == Type.LOAD_SCHEMATIC)
					{
						SchematicHolder.getInstance().addSchematic(schematic, true);
						this.gui.addMessage(MessageType.SUCCESS, "litematica.info.schematic_load.schematic_loaded", file.getFileName());

						if (DataManager.getCreatePlacementOnLoad() && this.gui.mc.player != null)
						{
							BlockPos pos = BlockPos.containing(this.gui.mc.player.position());
							String name = schematic.getMetadata().getName();
							boolean enabled = GuiBase.isShiftDown() == false;

							SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
							SchematicPlacement placement
									= SchematicPlacement.createFor(schematic, pos, name, enabled, enabled);
							manager.addSchematicPlacement(placement, true);
							manager.setSelectedSchematicPlacement(placement);
						}
					}
					else if (this.type == Type.MATERIAL_LIST)
					{
						if (GuiBase.isShiftDown())
						{
							MaterialListCreator creator = new MaterialListCreator(schematic);
							GuiStringListSelection gui = new GuiStringListSelection(schematic.getAreas()
							                                                                 .keySet(), creator);
							gui.setTitle(StringUtils.translate("litematica.gui.title.material_list.select_schematic_regions", schematic.getMetadata()
							                                                                                                           .getName()));
							gui.setParent(GuiUtils.getCurrentScreen());
							GuiBase.openGui(gui);
						}
						else
						{
							MaterialListSchematic materialList = new MaterialListSchematic(schematic, true);
							DataManager.setMaterialList(materialList); // Remember the last opened material list for the hotkey to (re-) open it
							GuiBase.openGui(new GuiMaterialList(materialList));
						}
					}
					else if (this.type == Type.RENAME_SCHEMATIC)
					{
						String oldName = schematic.getMetadata().getName();
						GuiBase.openGui(new GuiTextInputFeedback(256, "litematica.gui.title.rename_schematic", oldName, this.gui, new SchematicRenamer(entry.getDirectory(), entry.name(), this.gui)));
					}
					else if (this.type == Type.RENAME_FILE)
					{
						FileRenamer renamer = new FileRenamer(file, this.gui.getListWidget(), Configs.Generic.DISPLAY_FILE_OPS_FEEDBACK.getBooleanValue());
						GuiBase.openGui(new GuiTextInputFeedback(256, "litematica.gui.title.rename_file", entry.name(), this.gui, renamer));
					}

					if (warnType)
					{
						InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, 15000, "litematica.message.warn.schematic_load_non_litematica");
					}
				}
			}
		}

		private record SchematicRenamer(Path dir, String fileName, GuiSchematicLoad gui)
				implements IStringConsumerFeedback
		{
			@Override
			public boolean setString(String string)
			{
				LitematicaSchematic schematic = LitematicaSchematic.createFromFile(this.dir, this.fileName);

				if (schematic != null)
				{
					schematic.getMetadata().setName(string);
					schematic.getMetadata().setTimeModifiedToNow();

					if (schematic.writeToFile(this.dir, this.fileName, true))
					{
						if (this.gui.getListWidget() != null)
						{
							this.gui.getListWidget().clearSchematicMetadataCache();
						}

						return true;
					}
				}
				else
				{
					this.gui.setString(StringUtils.translate("litematica.error.schematic_rename.read_failed"));
				}

				return false;
			}
		}

		public enum Type
		{
			LOAD_SCHEMATIC      ("litematica.gui.button.load_schematic_to_memory"),
			MATERIAL_LIST       ("litematica.gui.button.material_list"),
			RENAME_SCHEMATIC    ("litematica.gui.button.rename_schematic"),
			RENAME_FILE         ("litematica.gui.button.rename_file"),
			;

			private final String translationKey;

			Type(String translationKey)
			{
				this.translationKey = translationKey;
			}

			public String getTranslationKey()
			{
				return this.translationKey;
			}
		}
	}

    private static class CheckboxListener implements ISelectionListener<WidgetCheckBox>
    {
        @Override
        public void onSelectionChange(WidgetCheckBox entry)
        {
            if (entry == null) return;
            DataManager.setCreatePlacementOnLoad(entry.isChecked());
        }
    }

	private record MaterialListCreator(LitematicaSchematic schematic) implements IStringListConsumer
	{
		@Override
		public boolean consume(Collection<String> strings)
		{
			MaterialListSchematic materialList = new MaterialListSchematic(this.schematic, strings, true);
			DataManager.setMaterialList(materialList); // Remember the last opened material list for the hotkey to (re-) open it
			GuiBase.openGui(new GuiMaterialList(materialList));

			return true;
		}
	}
}
