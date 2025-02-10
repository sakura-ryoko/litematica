package fi.dy.masa.litematica.scheduler;

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;

import fi.dy.masa.litematica.Reference;

public class TaskScheduler
{
    private static final TaskScheduler INSTANCE_CLIENT = new TaskScheduler(false);
    private static final TaskScheduler INSTANCE_SERVER = new TaskScheduler(true);

    private final List<ITask> tasks = new ArrayList<>();
    private final List<ITask> tasksToAdd = new ArrayList<>();
    private final boolean isServer;

    private TaskScheduler(boolean isServer)
    {
        this.isServer = isServer;
    }

    public static TaskScheduler getInstanceClient()
    {
        return INSTANCE_CLIENT;
    }

    public static TaskScheduler getInstanceServer()
    {
        return INSTANCE_SERVER;
    }

    public static TaskScheduler getServerInstanceIfExistsOrClient()
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        // Yes this is actually correct despite the naming - in single player we want to
        // schedule stuff to the integrated server's thread in some cases
        return mc.isIntegratedServerRunning() ? INSTANCE_SERVER : INSTANCE_CLIENT;
    }

    public void scheduleTask(ITask task, int interval)
    {
        synchronized (this)
        {
            task.createTimer(interval);
            task.getTimer().setNextDelay(0);
            this.tasksToAdd.add(task);
        }
    }

    public void runTasks()
    {
        Profiler profiler = Profilers.get();

        profiler.push(Reference.MOD_ID+"_run_tasks");
        synchronized (this)
        {
            if (this.tasks.isEmpty() == false)
            {
                for (int i = 0; i < this.tasks.size(); ++i)
                {
                    boolean finished = false;
                    ITask task = this.tasks.get(i);

                    if (task.shouldRemove())
                    {
                        finished = true;
                    }
                    else if (task.canExecute() && task.getTimer().tick())
                    {
                        finished = task.execute(profiler);
                    }

                    if (finished)
                    {
                        task.stop();
                        this.tasks.remove(i);
                        --i;
                    }
                }
            }

            if (this.tasksToAdd.isEmpty() == false)
            {
                this.addNewTasks();
            }
        }

        profiler.pop();
    }

    private void addNewTasks()
    {
        for (int i = 0; i < this.tasksToAdd.size(); ++i)
        {
            ITask task = this.tasksToAdd.get(i);
            task.init();
            this.tasks.add(task);
        }

        this.tasksToAdd.clear();
    }

    public boolean hasTask(Class <? extends ITask> clazz)
    {
        synchronized (this)
        {
            for (ITask task : this.tasks)
            {
                if (clazz.equals(task.getClass()))
                {
                    return true;
                }
            }

            for (ITask task : this.tasksToAdd)
            {
                if (clazz.equals(task.getClass()))
                {
                    return true;
                }
            }

            return false;
        }
    }

    /*
    public boolean hasTasks()
    {
        synchronized (this)
        {
            return this.tasks.isEmpty() == false || this.tasksToAdd.isEmpty() == false;
        }
    }

    public <T extends ITask> List<T> getTasksOfType(Class <? extends T> clazz)
    {
        synchronized (this)
        {
            List<T> list = new ArrayList<>();

            for (int i = 0; i < this.tasks.size(); ++i)
            {
                ITask task = this.tasks.get(i);

                if (clazz.isAssignableFrom(task.getClass()))
                {
                    list.add(clazz.cast(task));
                }
            }

            return list;
        }
    }

    public boolean removeTasksOfType(Class <? extends ITask> clazz)
    {
        synchronized (this)
        {
            boolean removed = false;

            for (int i = 0; i < this.tasks.size(); ++i)
            {
                ITask task = this.tasks.get(i);

                if (clazz.equals(task.getClass()))
                {
                    task.stop();
                    this.tasks.remove(i);
                    removed = true;
                    --i;
                }
            }

            return removed;
        }
    }
    */

    public ImmutableList<ITask> getAllTasks()
    {
        return ImmutableList.copyOf(this.tasks);
    }

    public boolean removeTask(ITask task)
    {
        synchronized (this)
        {
            task.stop();
            return this.tasks.remove(task);
        }
    }

    public void clearTasks()
    {
        synchronized (this)
        {
            for (int i = 0; i < this.tasks.size(); ++i)
            {
                ITask task = this.tasks.get(i);
                task.stop();
            }

            this.tasks.clear();
        }
    }
}
