package fi.dy.masa.litematica.schematic.projects;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import javax.annotation.Nullable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.data.json.JsonUtils;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.gui.GuiSchematicProjectManager;
import fi.dy.masa.litematica.gui.GuiSchematicProjectsBrowser;
import fi.dy.masa.litematica.util.FileType;

public class SchematicProjectsManager
{
    //private static final Pattern PATTERN_NAME_NUMBER = Pattern.compile("(.*)([0-9]+)$");
    private final Minecraft mc;
    @Nullable
    private SchematicProject currentProject;

    public SchematicProjectsManager()
    {
        this.mc = Minecraft.getInstance();
        this.currentProject = null;
    }

    public void openSchematicProjectsGui()
    {
        if (Configs.Generic.UNHIDE_SCHEMATIC_PROJECTS.getBooleanValue() == false)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, 10000, "litematica.message.warning.schematic_projects_hidden");
            return;
        }

        if (this.currentProject != null)
        {
            GuiSchematicProjectManager gui = new GuiSchematicProjectManager(this.currentProject);
            gui.setParent(GuiUtils.getCurrentScreen());
            GuiBase.openGui(gui);
        }
        else
        {
            GuiSchematicProjectsBrowser gui = new GuiSchematicProjectsBrowser();
            gui.setParent(GuiUtils.getCurrentScreen());
            GuiBase.openGui(gui);
        }
    }

    @Nullable
    public SchematicProject getCurrentProject()
    {
        return this.hasProjectOpen() ? this.currentProject : null;
    }

    public boolean hasProjectOpen()
    {
        return this.currentProject != null && Configs.Generic.UNHIDE_SCHEMATIC_PROJECTS.getBooleanValue();
    }

    public void createNewProject(Path dir, String projectName)
    {
        this.closeCurrentProject();

        BlockPos origin = BlockPos.ZERO;

        if (this.mc.player != null)
        {
            origin = fi.dy.masa.malilib.util.position.PositionUtils.getEntityBlockPos(this.mc.player);
        }

        this.currentProject = new SchematicProject(dir, dir.resolve(projectName + ".json"));
        this.currentProject.setName(projectName);
        this.currentProject.setOrigin(origin);
        this.currentProject.saveToFile();
    }

    public boolean openProject(Path projectFile)
    {
        this.closeCurrentProject();

        this.currentProject = this.loadProjectFromFile(projectFile, true);

        if (this.currentProject == null)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_projects.failed_to_load_project");
            return false;
        }

        this.currentProject.checkSelectionModeConfig();
        return true;
    }

    @Nullable
    public SchematicProject loadProjectFromFile(Path projectFile, boolean createPlacement)
    {
        if (FileType.fromFile(projectFile) == FileType.JSON &&
            Files.exists(projectFile) && Files.isRegularFile(projectFile) && Files.isReadable(projectFile))
        {
            JsonElement el = JsonUtils.parseJsonFile(projectFile);

            if (el != null && el.isJsonObject())
            {
                SchematicProject project = SchematicProject.fromJson(el.getAsJsonObject(), projectFile, createPlacement);

                if (project != null)
                {
                    project.checkSelectionModeConfig();

                    return project;
                }
            }
        }

        return null;
    }

    public void closeCurrentProject()
    {
        if (this.currentProject != null)
        {
            this.currentProject.saveToFile();
            this.removeCurrentPlacement();
            this.clear();
        }
    }

    public void saveCurrentProject()
    {
        if (this.currentProject != null)
        {
            this.currentProject.saveToFile();
        }
    }

    private void removeCurrentPlacement()
    {
        if (this.currentProject != null)
        {
            this.currentProject.removeCurrentPlacement();
        }
    }

    public void clear()
    {
        this.currentProject = null;
    }

    public boolean cycleVersion(int amount)
    {
        if (this.currentProject != null)
        {
            return this.currentProject.cycleVersion(amount);
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_projects.no_project_open");
        }

        return false;
    }

    public boolean commitNewVersion(String string, String description)
    {
        if (this.currentProject != null)
        {
            return this.currentProject.commitNewVersion(string, description);
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_projects.no_project_open");
        }

        return false;
    }

    public boolean pasteCurrentVersionToWorld()
    {
        SchematicProject project = this.getCurrentProject();

        if (project != null)
        {
            project.pasteToWorld();
            return true;
        }

        return false;
    }

    public boolean deleteLastSeenArea(Minecraft mc)
    {
        SchematicProject project = this.getCurrentProject();

        if (project != null)
        {
            project.deleteLastSeenArea(mc);
            return true;
        }

        return false;
    }

    public boolean deleteBlocksByPlacement()
    {
        SchematicProject project = this.getCurrentProject();

        if (project != null)
        {
            project.deleteBlocksByPlacement();
            return true;
        }

        return false;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        if (this.currentProject != null)
        {
            obj.add("current_project", new JsonPrimitive(this.currentProject.getProjectFile().toAbsolutePath().toString()));
        }

        return obj;
    }

    public void loadFromJson(JsonObject obj)
    {
        if (JsonUtils.hasString(obj, "current_project"))
        {
            Path file = Path.of(Objects.requireNonNull(JsonUtils.getString(obj, "current_project")));
            this.currentProject = this.loadProjectFromFile(file, true);

            if (this.currentProject != null)
            {
                this.currentProject.checkSelectionModeConfig();
            }
        }
    }
}
