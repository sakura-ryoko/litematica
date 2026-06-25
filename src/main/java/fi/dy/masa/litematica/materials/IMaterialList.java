package fi.dy.masa.litematica.materials;

import java.util.List;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.litematica.util.InclusionType;

public interface IMaterialList
{
    BlockInfoListType getMaterialListType();

    InclusionType getEntitiesInclusionType();

    InclusionType getContainersInclusionType();

    void setMaterialListEntries(List<MaterialListEntry> list);
}
