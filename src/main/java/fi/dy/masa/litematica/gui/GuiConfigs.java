package fi.dy.masa.litematica.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.IConfigGuiAllTab;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiConfigs extends GuiConfigsBase implements IConfigGuiAllTab
{
    public GuiConfigs()
    {
        super(10, 50, Reference.MOD_ID, null, "litematica.gui.title.configs", String.format("%s", Reference.MOD_VERSION));
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.clearOptions();

        if (DataManager.getConfigGuiTab() == ConfigGuiTab.RENDER_LAYERS)
        {
            GuiBase.openGui(new GuiRenderLayer());
            return;
        }

        int x = 10;
        int y = 26;

        for (ConfigGuiTab tab : ConfigGuiTab.values())
        {
            if (!this.useAllTab() && tab == ConfigGuiTab.ALL) continue;
            x += this.createButton(x, y, -1, tab);
        }

//        x += this.createButton(x, y, -1, ConfigGuiTab.GENERIC);
//        x += this.createButton(x, y, -1, ConfigGuiTab.INFO_OVERLAYS);
//        x += this.createButton(x, y, -1, ConfigGuiTab.VISUALS);
//        x += this.createButton(x, y, -1, ConfigGuiTab.COLORS);
//        x += this.createButton(x, y, -1, ConfigGuiTab.HOTKEYS);
//        x += this.createButton(x, y, -1, ConfigGuiTab.RENDER_LAYERS);
//        x += this.createButton(x, y, -1, ConfigGuiTab.TEST);
    }

    @Override
    public void removed()
    {
        super.removed();
        Configs.checkBaseLanguage();
    }

    private int createButton(int x, int y, int width, ConfigGuiTab tab)
    {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tab.getDisplayName());
        button.setEnabled(DataManager.getConfigGuiTab() != tab);
        this.addButton(button, new ButtonListener(tab, this));

        return button.getWidth() + 2;
    }

    @Override
    protected int getConfigWidth()
    {
        ConfigGuiTab tab = DataManager.getConfigGuiTab();

        if (tab == ConfigGuiTab.INFO_OVERLAYS)
        {
            return 140;
        }
        else if (tab == ConfigGuiTab.GENERIC || tab == ConfigGuiTab.VISUALS)
        {
            return 180;
        }
        else if (tab == ConfigGuiTab.COLORS)
        {
            return 100;
        }

        return super.getConfigWidth();
    }

    @Override
    protected boolean useKeybindSearch()
    {
        ConfigGuiTab tab = DataManager.getConfigGuiTab();

        return  tab == ConfigGuiTab.ALL ||
                tab == ConfigGuiTab.GENERIC ||
                tab == ConfigGuiTab.VISUALS ||
                tab == ConfigGuiTab.HOTKEYS;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs()
    {
        List<? extends IConfigBase> configs;
        ConfigGuiTab tab = DataManager.getConfigGuiTab();

        if (tab == ConfigGuiTab.ALL && this.useAllTab())
        {
            return this.getAllConfigs();
        }
        else if (tab == ConfigGuiTab.GENERIC)
        {
            configs = Configs.Generic.OPTIONS;
        }
        else if (tab == ConfigGuiTab.INFO_OVERLAYS)
        {
            configs = Configs.InfoOverlays.OPTIONS;
        }
        else if (tab == ConfigGuiTab.VISUALS)
        {
            configs = Configs.Visuals.OPTIONS;
        }
        else if (tab == ConfigGuiTab.COLORS)
        {
            configs = Configs.Colors.OPTIONS;
        }
        else if (tab == ConfigGuiTab.HOTKEYS)
        {
            configs = Hotkeys.HOTKEY_LIST;
        }
        else if (tab == ConfigGuiTab.RENDER_LAYERS)
        {
            configs = Collections.emptyList();
        }
//        else if (tab == ConfigGuiTab.TEST)
//        {
//            configs = Configs.Test.OPTIONS;
//        }
        else
        {
            configs = Collections.emptyList();
        }

        return ConfigOptionWrapper.createFor(configs);
    }

    @Override
    public boolean useAllTab()
    {
        return true;
    }

    @Override
    public List<ConfigOptionWrapper> getAllConfigs()
    {
        List<ConfigOptionWrapper> configs = new ArrayList<>();

        configs.addAll(ConfigOptionWrapper.createFor(Configs.Generic.OPTIONS));
        configs.addAll(ConfigOptionWrapper.createFor(Configs.InfoOverlays.OPTIONS));
        configs.addAll(ConfigOptionWrapper.createFor(Configs.Visuals.OPTIONS));
        configs.addAll(ConfigOptionWrapper.createFor(Configs.Colors.OPTIONS));
        configs.addAll(ConfigOptionWrapper.createFor(Hotkeys.HOTKEY_LIST));

        return configs;
    }

    @Override
    protected void onSettingsChanged()
    {
        super.onSettingsChanged();

        SchematicWorldRefresher.INSTANCE.updateAll();
    }

    private record ButtonListener(ConfigGuiTab tab, GuiConfigs parent) implements IButtonActionListener
    {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            DataManager.setConfigGuiTab(this.tab);

            if (this.tab != ConfigGuiTab.RENDER_LAYERS)
            {
                this.parent.reCreateListWidget(); // apply the new config width
                Objects.requireNonNull(this.parent.getListWidget()).resetScrollbarPosition();
                this.parent.initGui();
            }
            else
            {
                GuiBase.openGui(new GuiRenderLayer());
            }
        }
    }

    public enum ConfigGuiTab
    {
        ALL             (IConfigGuiAllTab.getTranslationKey()),
        GENERIC         ("litematica.gui.button.config_gui.generic"),
        INFO_OVERLAYS   ("litematica.gui.button.config_gui.info_overlays"),
        VISUALS         ("litematica.gui.button.config_gui.visuals"),
        COLORS          ("litematica.gui.button.config_gui.colors"),
        HOTKEYS         ("litematica.gui.button.config_gui.hotkeys"),
        RENDER_LAYERS   ("litematica.gui.button.config_gui.render_layers");
        //TEST            ("litematica.gui.button.config_gui.test");

        private final String translationKey;

        ConfigGuiTab(String translationKey)
        {
            this.translationKey = translationKey;
        }

        public String getDisplayName()
        {
            return StringUtils.translate(this.translationKey);
        }
    }
}
