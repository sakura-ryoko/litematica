package fi.dy.masa.litematica.gui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntryType;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.FileNameUtils;
import fi.dy.masa.malilib.util.KeyCodes;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.litematica.materials.MaterialListCustom;

public abstract class GuiMaterialListSaveBase extends GuiMaterialListBrowserBase implements ISelectionListener<DirectoryEntry>
{
    protected GuiTextFieldGeneric textField;
    protected String lastText = "";
    protected String defaultText = "";
    protected final MaterialListCustom materialList;

    public GuiMaterialListSaveBase(@Nonnull MaterialListCustom materialList)
    {
        super(10, 80);

        this.materialList = materialList;

        this.textField = new GuiTextFieldGeneric(10, 32, 160, 20, this.font);
        this.textField.setMaxLengthWrapper(256);
        this.textField.setFocusedWrapper(true);
    }

    @Override
    public int getBrowserHeight()
    {
        return this.getScreenHeight() - 80;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        boolean focused = this.textField.isFocusedWrapper();
        String text = this.textField.getTextWrapper();
        this.textField = new GuiTextFieldGeneric(10, 32, this.getScreenWidth() - 260, 18, this.font);
        this.textField.setTextWrapper(text);
        this.textField.setFocusedWrapper(focused);

        DirectoryEntry entry = this.getListWidget().getLastSelectedEntry();

        // Only set the text field contents if it hasn't been set already.
        // This prevents overwriting any user input text when switching to a newly created directory.
        if (this.lastText.isEmpty())
        {
            if (entry != null && entry.getType() != DirectoryEntryType.DIRECTORY && entry.getType() != DirectoryEntryType.INVALID)
            {
                this.setTextFieldText(FileNameUtils.getFileNameWithoutExtension(entry.getName()));
            }
            else if (this.materialList != null)
            {
                this.setTextFieldText(this.materialList.getName());
            }
            else
            {
                this.setTextFieldText(this.defaultText);
            }
        }

        int x = this.textField.getXWrapper() + this.textField.getWidthWrapper() + 4;
        int y = 28;

        this.createButton(10, 54, ButtonType.SAVE);
    }

    protected void setTextFieldText(String text)
    {
        this.lastText = text;
        this.textField.setTextWrapper(text);
    }

    protected String getTextFieldText()
    {
        return this.textField.getTextWrapper();
    }

    protected abstract IButtonActionListener createButtonListener(ButtonType type);

    private int createButton(int x, int y, ButtonType type)
    {
        String label = StringUtils.translate(type.getLabelKey());
        int width = this.getStringWidth(label) + 10;

        ButtonGeneric button;

        if (type == ButtonType.SAVE)
        {
            button = new ButtonGeneric(x, y, width, 20, label, "litematica.gui.label.schematic_save.hover_info.hold_shift_to_overwrite");
        }
        else
        {
            button = new ButtonGeneric(x, y, width, 20, label);
        }

        this.addButton(button, this.createButtonListener(type));

        return x + width + 4;
    }

    @Override
    public void setString(String string)
    {
        this.setNextMessageType(MessageType.ERROR);
        super.setString(string);
    }

    @Override
    public void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(ctx, mouseX, mouseY, partialTicks);

        this.textField.renderWrapper(ctx, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onSelectionChange(@Nullable DirectoryEntry entry)
    {
        if (entry != null && entry.getType() != DirectoryEntryType.DIRECTORY && entry.getType() != DirectoryEntryType.INVALID)
        {
            this.setTextFieldText(FileNameUtils.getFileNameWithoutExtension(entry.getName()));
        }
    }

    @Override
    protected ISelectionListener<DirectoryEntry> getSelectionListener()
    {
        return this;
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent click, boolean doubleClick)
    {
        if (this.textField.mouseClickedWrapper(click, doubleClick))
        {
            return true;
        }

        return super.onMouseClicked(click, doubleClick);
    }

    @Override
    public boolean onKeyTyped(KeyEvent input)
    {
        if (this.textField.keyPressedWrapper(input))
        {
            this.getListWidget().clearSelection();
            return true;
        }
        else if (input.key() == KeyCodes.KEY_TAB)
        {
            this.textField.setFocusedWrapper(! this.textField.isFocusedWrapper());
            return true;
        }

        return super.onKeyTyped(input);
    }

    @Override
    public boolean onCharTyped(CharacterEvent input)
    {
        if (this.textField.charTypedWrapper(input))
        {
            this.getListWidget().clearSelection();
            return true;
        }

        return super.onCharTyped(input);
    }

    public enum ButtonType
    {
        SAVE ("litematica.gui.button.save_material_list");

        private final String labelKey;

        private ButtonType(String labelKey)
        {
            this.labelKey = labelKey;
        }

        public String getLabelKey()
        {
            return this.labelKey;
        }
    }
}
