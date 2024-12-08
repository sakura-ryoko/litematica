package fi.dy.masa.litematica.util;

import java.io.File;

public enum FileType
{
    INVALID,
    UNKNOWN,
    JSON,
    LITEMATICA_SCHEMATIC,
    SCHEMATICA_SCHEMATIC,
    SPONGE_SCHEMATIC,
    VANILLA_STRUCTURE;

    public static FileType fromName(String fileName) {
        if (fileName.endsWith(".litematic"))
            {
                return LITEMATICA_SCHEMATIC;
            }
            else if (fileName.endsWith(".schematic"))
            {
                return SCHEMATICA_SCHEMATIC;
            }
            else if (fileName.endsWith(".nbt"))
            {
                return VANILLA_STRUCTURE;
            }
            else if (fileName.endsWith(".schem"))
            {
                return SPONGE_SCHEMATIC;
            }
            else if (fileName.endsWith(".json"))
            {
                return JSON;
            }

            return UNKNOWN;
    }

    public static FileType fromFile(File file)
    {
        if (file.isFile() && file.canRead())
        {
            return fromName(file.getName());   
        }
        else
        {
            return INVALID;
        }
    }
}
