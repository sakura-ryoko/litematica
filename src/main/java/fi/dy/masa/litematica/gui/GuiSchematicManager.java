package fi.dy.masa.litematica.gui;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.malilib.util.*;
import fi.dy.masa.malilib.config.IConfigOptionList;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfirmAction;
import fi.dy.masa.malilib.gui.GuiTextInputFeedback;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ConfigButtonOptionList;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.interfaces.IStringConsumerFeedback;
import com.mojang.blaze3d.platform.NativeImage;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.FileType;

public class GuiSchematicManager extends GuiSchematicBrowserBase implements ISelectionListener<DirectoryEntry>
{
    private static PreviewGenerator previewGenerator;
    private ExportType exportType = ExportType.V6_LITEMATIC;
	private EditType editType = EditType.RENAME_SCHEMATIC;
	private FileOpType fileOpType = FileOpType.RENAME_FILE;

    public GuiSchematicManager()
    {
        super(10, 24);

        this.title = StringUtils.translate("litematica.gui.title.schematic_manager");
    }

    @Override
    public String getBrowserContext()
    {
        return "schematic_manager";
    }

    @Override
    public Path getDefaultDirectory()
    {
        return DataManager.getSchematicsBaseDirectory();
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.getScreenHeight() - 60;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        this.createButtons();
    }

    private void createButtons()
    {
        int x = 10;
        int y = this.getScreenHeight() - 26;

		if (this.getListWidget() == null) return;
        DirectoryEntry selected = this.getListWidget().getLastSelectedEntry();

        if (selected != null)
        {
            FileType type = FileType.fromFile(selected.getFullPath());

            if (type == FileType.LITEMATICA_SCHEMATIC)
            {
	            x = this.createButton(x, y, ButtonListener.Type.EDIT_SCHEMATIC);
	            x = this.createButton(x, y, ButtonListener.Type.EDIT_TYPE);
	            x = this.createButton(x, y, ButtonListener.Type.IMPORT_SCHEMATIC);
                x = this.createButton(x, y, ButtonListener.Type.EXPORT_SCHEMATIC);
                x = this.createButton(x, y, ButtonListener.Type.EXPORT_TYPE);
	            x = this.createButton(x, y, ButtonListener.Type.FILE_OPS);
	            x = this.createButton(x, y, ButtonListener.Type.FILE_OPS_TYPE);

            }
            else if (type == FileType.SPONGE_SCHEMATIC || type == FileType.SCHEMATICA_SCHEMATIC || type == FileType.VANILLA_STRUCTURE)
            {
                x = this.createButton(x, y, ButtonListener.Type.IMPORT_SCHEMATIC);
	            x = this.createButton(x, y, ButtonListener.Type.FILE_OPS);
	            x = this.createButton(x, y, ButtonListener.Type.FILE_OPS_TYPE);

            }
		}

        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        String label = StringUtils.translate(type.getLabelKey());
        int buttonWidth = this.getStringWidth(label) + 20;
        this.addButton(new ButtonGeneric(this.getScreenWidth() - buttonWidth - 10, y, buttonWidth, 20, label), new ButtonListenerChangeMenu(type, null));
    }

    @Override
    public void onSelectionChange(@Nullable DirectoryEntry entry)
    {
        this.clearButtons();
        this.createButtons();
    }

    private int createButton(int x, int y, ButtonListener.Type type)
    {
        String label = type.getLabel();
        String hover = type.getHoverText();
        int buttonWidth = this.getStringWidth(label) + 10;
        ButtonGeneric button;

		if (type == ButtonListener.Type.EDIT_TYPE)
        {
	        buttonWidth = this.getStringWidth(this.editType.getDisplayName()) + 10;
	        button = new ConfigButtonOptionList(x, y, buttonWidth, 20, new EditSchematicWrapper());

	        if (this.editType.getHoverText() != null)
	        {
		        button.setHoverStrings(this.editType.getHoverText());
	        }
        }
	    else if (type == ButtonListener.Type.EXPORT_TYPE)
	    {
		    buttonWidth = this.getStringWidth(this.exportType.getDisplayName()) + 10;
		    button = new ConfigButtonOptionList(x, y, buttonWidth, 20, new ExportTypeWrapper());

		    if (this.exportType.getHoverText() != null)
		    {
			    button.setHoverStrings(this.exportType.getHoverText());
		    }
	    }
        else if (type == ButtonListener.Type.FILE_OPS_TYPE)
        {
	        buttonWidth = this.getStringWidth(this.fileOpType.getDisplayName()) + 10;
	        button = new ConfigButtonOptionList(x, y, buttonWidth, 20, new FileOpsWrapper());

	        if (this.fileOpType.getHoverText() != null)
	        {
		        button.setHoverStrings(this.fileOpType.getHoverText());
	        }
        }
        else if (hover != null)
        {
            button = new ButtonGeneric(x, y, buttonWidth, 20, label, hover);
        }
        else
        {
            button = new ButtonGeneric(x, y, buttonWidth, 20, label);
        }

        this.addButton(button, new ButtonListener(type, this));

        return x + buttonWidth + 4;
    }

    @Override
    protected ISelectionListener<DirectoryEntry> getSelectionListener()
    {
        return this;
    }

    public static boolean setPreviewImage()
    {
        if (previewGenerator != null)
        {
            previewGenerator.createAndSetPreviewImage();
            previewGenerator = null;
            return true;
        }

        return false;
    }

    public static boolean hasPendingPreviewTask()
    {
        return previewGenerator != null;
    }

    private class ExportTypeWrapper implements IConfigOptionList
    {
        @Override
        public IConfigOptionListEntry getOptionListValue()
        {
            return GuiSchematicManager.this.exportType;
        }

        @Override
        public IConfigOptionListEntry getDefaultOptionListValue()
        {
            return ExportType.V6_LITEMATIC;
        }

        @Override
        public void setOptionListValue(IConfigOptionListEntry value)
        {
            GuiSchematicManager.this.exportType = (ExportType) value;
            GuiSchematicManager.this.clearButtons();
            GuiSchematicManager.this.createButtons();
        }
    }

	private class EditSchematicWrapper implements IConfigOptionList
	{
		@Override
		public IConfigOptionListEntry getOptionListValue()
		{
			return GuiSchematicManager.this.editType;
		}

		@Override
		public IConfigOptionListEntry getDefaultOptionListValue()
		{
			return EditType.RENAME_SCHEMATIC;
		}

		@Override
		public void setOptionListValue(IConfigOptionListEntry value)
		{
			GuiSchematicManager.this.editType = (EditType) value;
			GuiSchematicManager.this.clearButtons();
			GuiSchematicManager.this.createButtons();
		}
	}

	private class FileOpsWrapper implements IConfigOptionList
	{
		@Override
		public IConfigOptionListEntry getOptionListValue()
		{
			return GuiSchematicManager.this.fileOpType;
		}

		@Override
		public IConfigOptionListEntry getDefaultOptionListValue()
		{
			return FileOpType.RENAME_FILE;
		}

		@Override
		public void setOptionListValue(IConfigOptionListEntry value)
		{
			GuiSchematicManager.this.fileOpType = (FileOpType) value;
			GuiSchematicManager.this.clearButtons();
			GuiSchematicManager.this.createButtons();
		}
	}

	private record ButtonListener(Type type, GuiSchematicManager gui) implements IButtonActionListener
	{
		@Override
		public void actionPerformedWithButton(ButtonBase button, int mouseButton)
		{
			if (this.type == Type.EDIT_SCHEMATIC && this.gui.editType == EditType.SET_PREVIEW
				&& mouseButton == 1)
			{
				if (previewGenerator != null)
				{
					previewGenerator = null;
					this.gui.addMessage(MessageType.SUCCESS, "litematica.message.schematic_preview_cancelled");
				}

				return;
			}

			DirectoryEntry entry = this.gui.getListWidget().getLastSelectedEntry();

			if (entry == null)
			{
				this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_load.no_schematic_selected");
				return;
			}

			Path file = entry.getFullPath();

			if (!Files.exists(file) || !Files.isRegularFile(file) || !Files.isReadable(file))
			{
				this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_load.cant_read_file", file.getFileName());
				return;
			}

			FileType fileType = FileType.fromFile(entry.getFullPath());

			if (this.type == Type.EDIT_SCHEMATIC)
			{
				if (fileType == FileType.LITEMATICA_SCHEMATIC)
				{
					if (this.gui.editType == EditType.RENAME_SCHEMATIC)
					{
						LitematicaSchematic schematic = LitematicaSchematic.createFromFile(entry.getDirectory(), entry.name());
						String oldName = schematic != null ? schematic.getMetadata().getName() : "";
						GuiBase.openGui(new GuiTextInputFeedback(256, "litematica.gui.title.rename_schematic", oldName, this.gui, new SchematicRenamer(entry.getDirectory(), entry.name(), this.gui)));
					}
					else if (this.gui.editType == EditType.CHANGE_AUTHOR)
					{
						LitematicaSchematic schematic = LitematicaSchematic.createFromFile(entry.getDirectory(), entry.name());
						String oldName = schematic != null ? schematic.getMetadata().getAuthor() : "";
						GuiBase.openGui(new GuiTextInputFeedback(128, "litematica.gui.title.schematic_change_author", oldName, this.gui, new SchematicChangeAuthor(entry.getDirectory(), entry.name(), this.gui)));
					}
					else if (this.gui.editType == EditType.SET_PREVIEW)
					{
						if (GuiBase.isShiftDown() && GuiBase.isCtrlDown() && GuiBase.isAltDown())
						{
							Path imageFile = entry.getDirectory().resolve("thumb.png");

							if (Files.exists(imageFile) && Files.isReadable(imageFile))
							{
								LitematicaSchematic schematic = LitematicaSchematic.createFromFile(entry.getDirectory(), entry.name());

								if (schematic != null)
								{
									try
									{
										InputStream inputStream = Files.newInputStream(imageFile);
										NativeImage image = NativeImage.read(inputStream);
										int x = image.getWidth() >= image.getHeight() ? (image.getWidth() - image.getHeight()) / 2 : 0;
										int y = image.getHeight() >= image.getWidth() ? (image.getHeight() - image.getWidth()) / 2 : 0;
										int longerSide = Math.min(image.getWidth(), image.getHeight());
										//System.out.printf("w: %d, h: %d, x: %d, y: %d\n", screenshot.getWidth(), screenshot.getHeight(), x, y);
										//int previewDimensions = 140;
										int previewDimensions = 120;
										NativeImage scaled = new NativeImage(previewDimensions, previewDimensions, false);
										image.resizeSubRectTo(x, y, longerSide, longerSide, scaled);
										@SuppressWarnings("deprecation")
										int[] pixels = scaled.makePixelArray();

										schematic.getMetadata().setPreviewImagePixelData(pixels);
										schematic.getMetadata().setTimeModifiedToNow();

										if (schematic.writeToFile(entry.getDirectory(), entry.name(), true))
										{
											InfoUtils.showGuiAndInGameMessage(MessageType.SUCCESS, "Custom preview image set");
										}

										return;
									}
									catch (Exception ignore)
									{
									}
								}
							}
							else
							{
								InfoUtils.showGuiAndInGameMessage(MessageType.ERROR, "Image 'thumb.png' not found");
								return;
							}
							InfoUtils.showGuiAndInGameMessage(MessageType.ERROR, "Failed to set custom preview image");
							return;
						}
						previewGenerator = new PreviewGenerator(entry.getDirectory(), entry.name());
						GuiBase.openGui(null);
						InfoUtils.showGuiAndInGameMessage(MessageType.INFO, "litematica.info.schematic_manager.preview.set_preview_by_taking_a_screenshot");
					}
				}
				else
				{
					this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_manager.schematic_edit.unsupported_type", file.getFileName());
				}
			}
			if (this.type == Type.EXPORT_SCHEMATIC)
			{
				if (fileType == FileType.LITEMATICA_SCHEMATIC)
				{
					GuiSchematicSaveExported gui = new GuiSchematicSaveExported(entry.type(), entry.getDirectory(), entry.name(), this.gui.exportType);
					gui.setParent(this.gui);
					GuiBase.openGui(gui);
				}
				else
				{
					this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_manager.schematic_export.unsupported_type", file.getFileName());
				}
			}
			else if (this.type == Type.IMPORT_SCHEMATIC)
			{
				if (fileType == FileType.LITEMATICA_SCHEMATIC ||
					fileType == FileType.SPONGE_SCHEMATIC ||
					fileType == FileType.SCHEMATICA_SCHEMATIC ||
					fileType == FileType.VANILLA_STRUCTURE)
				{
					GuiSchematicSaveImported gui = new GuiSchematicSaveImported(entry.type(), entry.getDirectory(), entry.name());
					gui.setParent(this.gui);
					GuiBase.openGui(gui);
				}
				else
				{
					this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_manager.schematic_import.unsupported_type", file.getFileName());
				}
			}
			else if (this.type == Type.FILE_OPS)
			{
				if (this.gui.fileOpType == FileOpType.RENAME_FILE)
				{
					FileRenamer renamer = new FileRenamer(file, this.gui.getListWidget(), Configs.Generic.DISPLAY_FILE_OPS_FEEDBACK.getBooleanValue());
					GuiBase.openGui(new GuiTextInputFeedback(256, "litematica.gui.title.rename_file", entry.name(), this.gui, renamer));
				}
				else if (this.gui.fileOpType == FileOpType.COPY_FILE)
				{
					FileCopier copier = new FileCopier(file, this.gui.getListWidget(), Configs.Generic.DISPLAY_FILE_OPS_FEEDBACK.getBooleanValue());
					GuiBase.openGui(new GuiTextInputFeedback(256, "litematica.gui.title.copy_file", entry.name(), this.gui, copier));
				}
				else if (this.gui.fileOpType == FileOpType.DELETE_FILE)
				{
					FileDeleter deleter = new FileDeleter(entry.getFullPath(), this.gui.getListWidget(), Configs.Generic.DISPLAY_FILE_OPS_FEEDBACK.getBooleanValue());
					GuiBase.openGui(new GuiConfirmAction(400, "litematica.gui.title.confirm_file_deletion", deleter, this.gui, "litematica.gui.message.confirm_file_deletion", entry.name()));
				}
			}
		}

		public enum Type
		{
			EDIT_SCHEMATIC      ("litematica.gui.button.schematic_manager.edit_schematic", "litematica.gui.button.schematic_manager.edit_schematic.hover"),
			EDIT_TYPE           (""),
			IMPORT_SCHEMATIC    ("litematica.gui.button.import", "litematica.gui.button.import.hover"),
			EXPORT_SCHEMATIC    ("litematica.gui.button.schematic_manager.export_as", "litematica.gui.button.schematic_manager.export_as.hover"),
			EXPORT_TYPE         (""),
			FILE_OPS            ("litematica.gui.button.schematic_manager.file_ops", "litematica.gui.button.schematic_manager.file_ops.hover"),
			FILE_OPS_TYPE       (""),
			;

			private final String label;
			@Nullable
			private final String hoverText;

			Type(String label)
			{
				this(label, null);
			}

			Type(String label, @Nullable String hoverText)
			{
				this.label = label;
				this.hoverText = hoverText;
			}

			public String getLabel()
			{
				return StringUtils.translate(this.label);
			}

			@Nullable
			public String getHoverText()
			{
				return this.hoverText != null ? StringUtils.translate(this.hoverText) : null;
			}
		}
	}

	private record SchematicRenamer(Path dir, String fileName, GuiSchematicManager gui)
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
					this.gui.getListWidget().clearSchematicMetadataCache();
					return true;
				}
			}
			else
			{
				this.gui.setString(StringUtils.translate("litematica.error.schematic_load.cant_read_file"));
			}

			return false;
		}
	}

	private record SchematicChangeAuthor(Path dir, String fileName, GuiSchematicManager gui)
			implements IStringConsumerFeedback
	{
		@Override
		public boolean setString(String string)
		{
			LitematicaSchematic schematic = LitematicaSchematic.createFromFile(this.dir, this.fileName);

			if (schematic != null)
			{
				schematic.getMetadata().setAuthor(string);
				schematic.getMetadata().setTimeModifiedToNow();

				if (schematic.writeToFile(this.dir, this.fileName, true))
				{
					this.gui.getListWidget().clearSchematicMetadataCache();
					return true;
				}
			}
			else
			{
				this.gui.setString(StringUtils.translate("litematica.error.schematic_load.cant_read_file"));
			}

			return false;
		}
	}

	public record PreviewGenerator(Path dir, String fileName)
	{
		public void createAndSetPreviewImage()
		{
			LitematicaSchematic schematic = LitematicaSchematic.createFromFile(this.dir, this.fileName);

			if (schematic != null)
			{
				try
				{
					Minecraft mc = Minecraft.getInstance();
					Screenshot.takeScreenshot(mc.getMainRenderTarget(), (screenshot) ->
					{
						int x = screenshot.getWidth() >= screenshot.getHeight()
								? (screenshot.getWidth() - screenshot.getHeight()) / 2 : 0;
						int y = screenshot.getHeight() >= screenshot.getWidth()
								? (screenshot.getHeight() - screenshot.getWidth()) / 2 : 0;
						int longerSide = Math.min(screenshot.getWidth(), screenshot.getHeight());
						//System.out.printf("w: %d, h: %d, x: %d, y: %d\n", screenshot.getWidth(), screenshot.getHeight(), x, y);
						//int previewDimensions = 140;
						int previewDimensions = 120;
						NativeImage scaled = new NativeImage(previewDimensions, previewDimensions, false);
						screenshot.resizeSubRectTo(x, y, longerSide, longerSide, scaled);
						@SuppressWarnings("deprecation")
						int[] pixels = scaled.makePixelArray();

						schematic.getMetadata().setPreviewImagePixelData(pixels);
						schematic.getMetadata().setTimeModifiedToNow();
						schematic.writeToFile(this.dir, this.fileName, true);

						InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "litematica.info.schematic_manager.preview.success");
					});
				}
				catch (Exception e)
				{
					Litematica.LOGGER.warn("Exception while creating preview image", e);
				}
			}
			else
			{
				InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_rename.read_failed");
			}
		}
	}

    public enum ExportType implements IConfigOptionListEntry
    {
        SCHEMATIC       ("litematica.gui.label.schematic_manager.export_type.schematic", "litematica.gui.label.schematic_manager.export_type.schematic.hover"),
        V6_LITEMATIC    ("litematica.gui.label.schematic_manager.export_type.v6_litematic", "litematica.gui.label.schematic_manager.export_type.v6_litematic.hover"),
        VANILLA         ("litematica.gui.label.schematic_manager.export_type.vanilla", "litematica.gui.label.schematic_manager.export_type.vanilla.hover");

        private final String label;
	    private final String hoverText;

        ExportType(String label)
        {
            this(label, null);
        }

	    ExportType(String label, @Nullable String hoverText)
	    {
		    this.label = label;
			this.hoverText = hoverText;
	    }

        @Override
        public String getStringValue()
        {
            return this.name().toLowerCase();
        }

        @Override
        public String getDisplayName()
        {
            return StringUtils.translate(this.label);
        }

	    @Nullable
	    public String getHoverText()
	    {
		    return this.hoverText != null ? StringUtils.translate(this.hoverText) : null;
	    }

        @Override
        public IConfigOptionListEntry cycle(boolean forward)
        {
            int id = this.ordinal();

            if (forward)
            {
                if (++id >= values().length)
                {
                    id = 0;
                }
            }
            else
            {
                if (--id < 0)
                {
                    id = values().length - 1;
                }
            }

            return values()[id % values().length];
        }

        @Override
        public ExportType fromString(String name)
        {
            return fromStringStatic(name);
        }

        public static ExportType fromStringStatic(String name)
        {
            for (ExportType al : ExportType.values())
            {
                if (al.name().equalsIgnoreCase(name))
                {
                    return al;
                }
            }

            return ExportType.V6_LITEMATIC;
        }
    }

	public enum EditType implements IConfigOptionListEntry
	{
		RENAME_SCHEMATIC    ("litematica.gui.label.schematic_manager.edit_type.rename_schematic", "litematica.gui.label.schematic_manager.edit_type.rename_schematic.hover"),
		CHANGE_AUTHOR       ("litematica.gui.label.schematic_manager.edit_type.change_author", "litematica.gui.label.schematic_manager.edit_type.change_author.hover"),
		SET_PREVIEW         ("litematica.gui.label.schematic_manager.edit_type.set_preview", "litematica.gui.label.schematic_manager.edit_type.set_preview.hover"),
		;

		private final String label;
		private final String hoverText;

		EditType(String label)
		{
			this(label, null);
		}

		EditType(String label, @Nullable String hoverText)
		{
			this.label = label;
			this.hoverText = hoverText;
		}

		@Override
		public String getStringValue()
		{
			return this.name().toLowerCase();
		}

		@Override
		public String getDisplayName()
		{
			return StringUtils.translate(this.label);
		}

		@Nullable
		public String getHoverText()
		{
			return this.hoverText != null ? StringUtils.translate(this.hoverText) : null;
		}

		@Override
		public IConfigOptionListEntry cycle(boolean forward)
		{
			int id = this.ordinal();

			if (forward)
			{
				if (++id >= values().length)
				{
					id = 0;
				}
			}
			else
			{
				if (--id < 0)
				{
					id = values().length - 1;
				}
			}

			return values()[id % values().length];
		}

		@Override
		public EditType fromString(String name)
		{
			return fromStringStatic(name);
		}

		public static EditType fromStringStatic(String name)
		{
			for (EditType al : EditType.values())
			{
				if (al.name().equalsIgnoreCase(name))
				{
					return al;
				}
			}

			return EditType.RENAME_SCHEMATIC;
		}
	}

	public enum FileOpType implements IConfigOptionListEntry
	{
		RENAME_FILE     ("litematica.gui.label.schematic_manager.file_op_type.rename", "litematica.gui.label.schematic_manager.file_op_type.rename.hover"),
		COPY_FILE       ("litematica.gui.label.schematic_manager.file_op_type.copy", "litematica.gui.label.schematic_manager.file_op_type.copy.hover"),
		DELETE_FILE     ("litematica.gui.label.schematic_manager.file_op_type.delete", "litematica.gui.label.schematic_manager.file_op_type.delete.hover"),
		;

		private final String label;
		private final String hoverText;

		FileOpType(String label)
		{
			this(label, null);
		}

		FileOpType(String label, @Nullable String hoverText)
		{
			this.label = label;
			this.hoverText = hoverText;
		}

		@Override
		public String getStringValue()
		{
			return this.name().toLowerCase();
		}

		@Override
		public String getDisplayName()
		{
			return StringUtils.translate(this.label);
		}

		@Nullable
		public String getHoverText()
		{
			return this.hoverText != null ? StringUtils.translate(this.hoverText) : null;
		}

		@Override
		public IConfigOptionListEntry cycle(boolean forward)
		{
			int id = this.ordinal();

			if (forward)
			{
				if (++id >= values().length)
				{
					id = 0;
				}
			}
			else
			{
				if (--id < 0)
				{
					id = values().length - 1;
				}
			}

			return values()[id % values().length];
		}

		@Override
		public FileOpType fromString(String name)
		{
			return fromStringStatic(name);
		}

		public static FileOpType fromStringStatic(String name)
		{
			for (FileOpType al : FileOpType.values())
			{
				if (al.name().equalsIgnoreCase(name))
				{
					return al;
				}
			}

			return FileOpType.RENAME_FILE;
		}
	}
}
