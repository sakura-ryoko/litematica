package fi.dy.masa.litematica.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nonnull;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.interfaces.ICompletionListener;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListCustom;

public class GuiMaterialListSave extends GuiMaterialListSaveBase implements ICompletionListener
{
    public GuiMaterialListSave(@Nonnull MaterialListCustom materialList)
    {
        super(materialList);

        this.title = StringUtils.translate("litematica.gui.title.save_material_list");
        this.defaultText = materialList.getName().replaceAll("[^a-zA-Z0-9_\\-]", "_") +MaterialListCustom.JSON_FILE_EXTENSION;
    }

    @Override
    public String getBrowserContext()
    {
        return "material_list_save";
    }

    @Override
    public Path getDefaultDirectory()
    {
        return DataManager.getSchematicsBaseDirectory();
    }

    @Override
    protected IButtonActionListener createButtonListener(ButtonType type)
    {
        return new ButtonListener(type, this);
    }

    @Override
    public void onTaskCompleted()
    {
        if (this.mc.isSameThread())
        {
            this.refreshList();
        }
        else
        {
            this.mc.execute(GuiMaterialListSave.this::refreshList);
        }
    }

    private void refreshList()
    {
        if (GuiUtils.getCurrentScreen() == this)
        {
            this.getListWidget().refreshEntries();
            this.getListWidget().clearMetadataCache();
        }
    }

    private record ButtonListener(ButtonType type, GuiMaterialListSave gui) implements IButtonActionListener
    {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            if (this.type == ButtonType.SAVE)
            {
                Path dir = this.gui.getListWidget().getCurrentDirectory();
                String fileName = this.gui.getTextFieldText();

                if (!Files.isDirectory(dir))
                {
                    this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_save.invalid_directory", dir.toAbsolutePath());
                    return;
                }

                if (fileName.isEmpty())
                {
                    this.gui.addMessage(MessageType.ERROR, "litematica.error.material_list_save.invalid_name", fileName);
                    return;
                }

                // Saving a schematic from memory
                if (this.gui.materialList != null)
                {
                    String customFileName = fileName;
                    boolean shiftDown = GuiBase.isShiftDown();

                    if (!customFileName.endsWith(MaterialListCustom.JSON_FILE_EXTENSION))
                    {
                        customFileName += MaterialListCustom.JSON_FILE_EXTENSION;
                    }

                    if (FileUtils.canWriteToFileAsPath(dir, customFileName, shiftDown) == false)
                    {
                        this.gui.addMessage(MessageType.ERROR, "litematica.error.material_list_write_to_file_failed.exists", fileName);
                        return;
                    }

                    Path customFile = dir.resolve(customFileName);

                    if (this.gui.materialList.toJsonFile(customFile, shiftDown))
                    {
                        this.gui.getListWidget().refreshEntries();
                        String key = "litematica.message.material_list_save.exported";
                        this.gui.addMessage(MessageType.SUCCESS, key, customFile.getFileName().toString());

                        if (this.gui.mc.player != null)
                        {
                            StringUtils.sendOpenFileChatMessage(this.gui.mc.player, key, customFile.toFile());
                        }
                    }
                }
                else
                {
                    this.gui.addMessage(MessageType.ERROR, "litematica.message.error.material_list_save");
                }
            }
        }
    }
}
