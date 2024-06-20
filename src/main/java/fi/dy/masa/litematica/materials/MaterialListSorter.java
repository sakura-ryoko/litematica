package fi.dy.masa.litematica.materials;

import java.util.Comparator;

public class MaterialListSorter implements Comparator<MaterialListEntry>
{
    private final MaterialListBase materialList;

    public MaterialListSorter(MaterialListBase materialList)
    {
        this.materialList = materialList;
    }

    @Override
    public int compare(MaterialListEntry entry1, MaterialListEntry entry2)
    {
        int cmp = switch (this.materialList.getSortCriteria()) {
            case COUNT_TOTAL -> entry1.getCountTotal() - entry2.getCountTotal();
            case COUNT_MISSING -> entry1.getCountMissing() - entry2.getCountMissing();
            case COUNT_AVAILABLE -> entry1.getCountAvailable() - entry2.getCountAvailable();
            default -> 0;
        };
        if(cmp==0)
            cmp = entry1.getStack().getName().getString().compareTo(entry2.getStack().getName().getString());
        return this.materialList.getSortInReverse()? -cmp: cmp;
    }
}
